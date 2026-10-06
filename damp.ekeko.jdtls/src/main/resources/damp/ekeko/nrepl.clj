(ns damp.ekeko.nrepl
  "Starts and stops an nREPL server inside the jdt.ls JVM. Compared to a
   plain nREPL server:
   - cider-nrepl's middleware is included, providing the completion,
     documentation and source lookup ops that clients such as Calva expect
     (see https://docs.cider.mx/cider-nrepl/);
   - evaluations can be given a timeout, so that a long-running query does
     not block its REPL session (see damp.ekeko.middleware)."
  (:require [cider.nrepl]
            [damp.ekeko.middleware :refer [wrap-eval-timeout]]
            [nrepl.server]))

;; The running server (as returned by nrepl.server/start-server), or nil.
;; Start/stop lock on the atom itself so that two servers are never started concurrently. 
(defonce ^:private server (atom nil))

(defn start-repl
  "Starts an nREPL server inside this JVM (unless one is already running)
   and returns the port it listens on.

   When `timeout-ms` is given (and positive), \"eval\" requests running
   longer than that many milliseconds are interrupted, see
   damp.ekeko.middleware/wrap-eval-timeout. It only applies to a newly
   started server: an already running server keeps its own timeout."
  ([] (start-repl nil))
  ([timeout-ms]
   (locking server
     (if-let [running @server]
       (:port running)
       ;; The timeout middleware interrupts evaluations through the session
       ;; middleware's "interrupt" op, so it has to wrap the complete
       ;; handler stack.
       (let [handler ((wrap-eval-timeout timeout-ms) cider.nrepl/cider-nrepl-handler)
             started (nrepl.server/start-server :handler handler)]
         (reset! server started)
         (:port started))))))

(defn stop-repl
  "Stops the running nREPL server, if any."
  []
  (locking server
    (when-let [running @server]
      (try
        (nrepl.server/stop-server running)
        (finally
          (reset! server nil))))))

(defn running?
  "Whether an nREPL server is currently running."
  []
  (some? @server))

(defn port
  "The port of the running nREPL server, or -1 when none is running."
  []
  (if-let [running @server]
    (:port running)
    -1))
