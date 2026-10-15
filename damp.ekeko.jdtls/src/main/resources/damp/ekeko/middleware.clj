(ns
  ^{:doc "nREPL middleware that bounds how long an \"eval\" request may run.
         Long-running Ekeko queries would otherwise block the session they
         are evaluated in until they finish. When an evaluation exceeds the
         timeout it is interrupted through nREPL's own \"interrupt\" op, so
         the client receives the usual `interrupted`/`done` statuses for it,
         preceded by an explanatory message on :err."}
  damp.ekeko.middleware
  (:require [nrepl.middleware :as middleware]
            [nrepl.middleware.interruptible-eval :as interruptible-eval]
            [nrepl.transport :as t])
  (:import (java.util.concurrent Executors ScheduledExecutorService ThreadFactory TimeUnit)))

(defonce ^:private ^ScheduledExecutorService scheduler
  (Executors/newSingleThreadScheduledExecutor
    (reify ThreadFactory
      (newThread [_ r]
        (doto (Thread. ^Runnable r "ekeko-nrepl-eval-timeout")
          (.setDaemon true))))))

(defn- status-set
  "The :status of a response as a set of strings, whether it was given as a
   keyword, a string or a collection of either."
  [{:keys [status]}]
  (cond
    (nil? status) #{}
    (coll? status) (into #{} (map name) status)
    :else #{(name status)}))

(defn- schedule
  [timeout-ms f]
  (.schedule scheduler ^Runnable f (long timeout-ms) TimeUnit/MILLISECONDS))

(defn- interrupt-eval!
  "Asks the session that runs the eval request `msg` to interrupt it.
   Responses to the interrupt request itself are captured and returned (as
   a set of statuses), waiting at most `wait-ms` for them; the `interrupted`
   notification for the eval request is forwarded to the client, preceded
   by the message `reason` explaining why."
  [handler {:keys [id session transport] :as msg} reason wait-ms]
  (let [interrupt-statuses (promise)
        relay (t/fn-transport
                (constantly nil)
                (fn [response]
                  (if (= (:id response) id)
                    (do (t/respond-to msg :err reason)
                        (t/send transport response))
                    (when (contains? (status-set response) "done")
                      (deliver interrupt-statuses (status-set response))))))]
    (handler {:op "interrupt"
              :id (str "ekeko-timeout-" id)
              :session session
              :interrupt-id id
              :transport relay})
    (deref interrupt-statuses wait-ms #{})))

(defn- arm-timeout!
  "Schedules the interruption of the eval request `msg` once `timeout-ms`
   has elapsed, unless `done?` has been set by then. When the request has
   not started running yet, the timer is re-armed instead: only the time
   the request actually spends running should count. That is the case when
   it is still queued behind another evaluation in its session
   (`interrupt-id-mismatch`), or when it has not even reached the session
   yet (`session-idle`), e.g. while cider-nrepl lazily loads its middleware
   on the first request. Sets `expired` to `timeout-ms` when the request is
   interrupted."
  [handler msg timeout-ms done? timer expired]
  (reset! timer
          (schedule timeout-ms
                    (fn []
                      (when-not @done?
                        ;; Set before interrupting, so that the evaluation
                        ;; sees it as soon as it is interrupted.
                        (reset! expired timeout-ms)
                        (let [statuses (interrupt-eval! handler msg
                                                        (str "Evaluation timed out after "
                                                             timeout-ms " ms and was interrupted.\n")
                                                        timeout-ms)]
                          (when (some statuses ["interrupt-id-mismatch" "session-idle"])
                            (reset! expired nil)
                            (when-not @done?
                              (arm-timeout! handler msg timeout-ms done? timer expired)))))))))

(def ^:private cancel-wait-ms
  "How long a cancellation waits for the session to answer its interrupt."
  5000)

(defn- with-cancel
  "Gives the eval request `msg` a function that interrupts it, for
   cancel-current-eval to return."
  [handler msg]
  (assoc msg ::cancel #(interrupt-eval! handler msg "Evaluation was cancelled.\n" cancel-wait-ms)))

(defn- timed-eval
  [handler {:keys [transport] :as msg} timeout-ms]
  (let [done? (atom false)
        timer (atom nil)
        expired (atom nil)
        watching (reify t/Transport
                   (recv [_] (t/recv transport))
                   (recv [_ timeout] (t/recv transport timeout))
                   (send [this response]
                     (when (and (= (:id response) (:id msg))
                                (contains? (status-set response) "done"))
                       (reset! done? true)
                       (some-> ^java.util.concurrent.Future @timer (.cancel false)))
                     (t/send transport response)
                     this))]
    (arm-timeout! handler msg timeout-ms done? timer expired)
    ;; Cancelling through the watching transport lets it see the request
    ;; is done, so the timeout is not re-armed for a cancelled request.
    (handler (with-cancel handler (assoc msg :transport watching ::expired expired)))))

(defn expired-timeout
  "When called from an evaluation that wrap-eval-timeout interrupted because
   it ran too long, returns that timeout in milliseconds; nil otherwise,
   e.g. when it was interrupted by the client, or outside an nREPL eval."
  []
  (some-> interruptible-eval/*msg* ::expired deref))

(defn cancel-current-eval
  "When called from an nREPL evaluation, returns a no-argument function
   that interrupts it (from another thread), as if the client had sent an
   \"interrupt\" for it; nil outside an nREPL eval."
  []
  (some-> interruptible-eval/*msg* ::cancel))

(defn wrap-eval-timeout
  "Returns an nREPL middleware that interrupts \"eval\" requests still
   running after `timeout-ms` milliseconds. A nil or non-positive
   `timeout-ms` disables the timeout. Whether or not a timeout applies,
   evaluations can also be cancelled, see cancel-current-eval. Other ops
   pass through unchanged.

   Interruption relies on nREPL's session middleware: evaluations outside
   of a session (ephemeral ones) cannot be interrupted and simply run to
   completion. The middleware must therefore sit before the session
   middleware in the stack, e.g. by wrapping a complete handler:

     ((wrap-eval-timeout 30000) cider.nrepl/cider-nrepl-handler)"
  [timeout-ms]
  (with-meta
    (fn [handler]
      (fn [{:keys [op] :as msg}]
        (cond
          (not= op "eval") (handler msg)
          (and timeout-ms (pos? timeout-ms)) (timed-eval handler msg timeout-ms)
          :else (handler (with-cancel handler msg)))))
    {::middleware/descriptor {:requires #{}
                              :expects #{"eval" "interrupt"}
                              :handles {}}}))
