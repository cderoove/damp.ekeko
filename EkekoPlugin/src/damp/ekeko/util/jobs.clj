(ns 
  damp.ekeko.util.jobs
    ^{:doc "Utilities for launching and waiting for Eclipse Jobs."
    :author "Coen De Roover"}
  (:import [org.eclipse.core.runtime.jobs Job IJobChangeListener JobChangeAdapter IJobChangeEvent]
            [org.eclipse.core.runtime Status IProgressMonitor]
            [org.eclipse.swt.widgets Display]
            [org.eclipse.ui PlatformUI]
            [org.eclipse.jface.dialogs ProgressMonitorDialog]))

(defn
  make-eclipse-job 
  "2 args: Creates a job with given label that will, when run,
   apply f to the ProgressMonitor associated with the job.
 
  3 args: Creates a job with given label that will, when run,
  apply taskunitf taskcomplete times. Taskunitf is given the current 
  index in [i, taskcomplete] applications."   
  ([label f] 
    (proxy [Job] [label] 
      (run [^IProgressMonitor monitor]  (f monitor))))
   
  ([label taskcomplete taskunitf] 
    (make-eclipse-job label
                      (fn [^IProgressMonitor monitor]
                        (.beginTask monitor label (int taskcomplete))
                        (dotimes [i taskcomplete]
                          (taskunitf i)
                          (.worked monitor 1))
                        (.done monitor)
                        (Status/OK_STATUS)))))

(defn 
  schedule-job
  "Schedules the given job.
  
   Examples:
   (schedule-job (make-eclipse-job \"Counting till 10000\" 10000 (fn [i] (println (str i))))) 
  "
  ([^Job job] (schedule-job job true))
  ([^Job job user?] 
    (doto job
      (.setUser user?)
      (.schedule))))

(defn 
  promise-until-job-done 
  "Schedules a job to be run asynchronously and returns a promise until the job is done."
  ([value job] (promise-until-job-done value job true))
  ([value job user?] 
    (let [p (promise)
          listener (proxy [JobChangeAdapter] []
                     (running [^IJobChangeEvent event]
                              (println "Running job: " (.getName (.getJob event))))
                     (done [^IJobChangeEvent event] 
                           (println "Job done: " (.getName (.getJob event)))
                           (deliver p value)))]
      (.addJobChangeListener ^Job job ^IJobChangeListener listener)
      (schedule-job job user?)
      p)))


(defn 
  as-synchronous-job 
  "Schedules a job and blocks until it is done."
  [label f]
  (let [result (atom nil)
        job (make-eclipse-job label 1 (fn [i] (reset! result (f))))]
     @@(promise-until-job-done result job)))


;(defn show-progress-dialog [runnablewithprogressmonitor] 
;  (.syncExec (Display/getDefault)
;              (fn []
;                (let [dialog (ProgressMonitorDialog. (.getShell (.getActiveWorkbenchWindow (PlatformUI/getWorkbench))))]
;                  (.run dialog true false runnablewithprogressmonitor)))))


              
              













