(ns damp.ekeko.workspace.workspace
     ^{:doc "Utilities for accessing and interacting with the Eclipse workspace."
      :author "Coen De Roover"}
      (:import [org.eclipse.core.resources ResourcesPlugin IWorkspace]
               [org.eclipse.jdt.core  IMember IJavaElement ITypeHierarchy JavaCore IType IJavaModel IJavaProject IPackageFragment ICompilationUnit]
               [org.eclipse.ui PlatformUI IWorkingSet IWorkingSetManager]
               [org.eclipse.core.runtime.jobs Job]
               [org.eclipse.core.runtime Status]
               [damp.ekeko EkekoModel JavaProjectModel ProjectModel])
      (require [damp.ekeko [ekekomodel :as ekekomodel]]))

; JDT 
; ---

(defn 
  ^IWorkspace 
  eclipse-workspace
  "Returns the Eclipse workspace."
  []
  (ResourcesPlugin/getWorkspace))

(defn 
  ^IJavaModel 
  jdt-model
  "Returns the Eclipse JDT IJavaModel."
  []
  (JavaCore/create (.getRoot  (eclipse-workspace))))

; Projects
; --------


(defn 
  jdt-projects
  "Returns the JDT IJavaProjects in the JDT IJavaModel"
  []
  (.getJavaProjects (jdt-model)))

(defn
  ^IJavaProject 
  jdt-project-named 
  "Returns the JDT IJavaProject with the given name."
  [name]
  (.getJavaProject (jdt-model) name))

(defn 
  jdt-projects-names 
  "Returns a seq with the names of the JDT IJavaProjects in the workspace."
  []
  (map (fn [^IJavaProject p] (.getElementName p))
       (jdt-projects)))

; Working Sets
; ------------

(defn 
  ^IWorkingSetManager
  jdt-workingset-manager
  "Returns the Eclipse working set manager."
  []
  (.getWorkingSetManager (PlatformUI/getWorkbench)))

(defn 
  jdt-workingset-named 
  "Returns the Eclipse working set with the given name."
  [name]
  (.getWorkingSet (jdt-workingset-manager) name))

; Workspace Projects (including non-JDT)
; --------------------------------------


(defn 
  workspace-projects 
  "Returns a Seq of all IProjects in the Eclipse workspace.
   This includes projects not managed by the EkekoModel
   as well as non-Java projects."
  []
  (-> 
    (eclipse-workspace)
    (.getRoot)
    (.getProjects)))


(defn 
  workspace-project-open? 
  "Succeeds when the given IProject is open."
  [p]
  (.isOpen p))

(defn
  workspace-project-open!
  "Opens the given IProject."
  [p]
  (when-not 
    (workspace-project-open? p)
    (.open p nil)))
  
(defn 
  workspace-project-close!
  "Closes the given IProject."
  [p]
  (when
    (workspace-project-open? p)
    (.close p nil)))  

(defn 
  workspace-project-ekeko-enabled?
  "Succeeds when the given IProject is managed by the EkekoModel 
   (i.e., has the Ekeko nature enabled)."
  [p]
  (.hasProjectModel (ekekomodel/ekeko-model) p))

(defn 
  workspace-project-toggle-ekeko!
  "Toggles the Ekeko nature on the given IProject."
  [p]
  (EkekoModel/toggleNature p))

(defn 
  workspace-project-enable-ekeko! 
  "Enables the Ekeko nature on the given IProject."
  [p]
  (when-not 
    (workspace-project-ekeko-enabled? p)
    (workspace-project-toggle-ekeko! p)))
    
(defn 
  workspace-project-disable-ekeko!
  "Disables the Ekeko nature on the given IProject."
  [p]
  (when
    (workspace-project-ekeko-enabled? p)
    (workspace-project-toggle-ekeko! p)))

(defn 
  for-each-workspace-project 
  "Applies f to each IProject in the workspace."
  [f] 
  (doseq [p (workspace-projects)]
    (f p)))

(defn 
  workspace-close-projects! 
  "Closes all projects in the workspace."
  []
  (for-each-workspace-project workspace-project-close!))


(defn 
  map-workspace
  "Applies f to each IProject in the workspace, 
   returns a map from project name to the result of f."
  [f]
  (reduce (fn [results p] (assoc results (.getName p) (f p))) {} (workspace-projects)))

(defn 
  workspace-wait-for-builds-to-finish
  "Asks the current thread to wait for all builds in the workspace to finish.
   Important to ensure the EkekoModel is fully populated when
   querying large workspaces."
  []
  (.join (Job/getJobManager) (ResourcesPlugin/FAMILY_AUTO_BUILD) nil))


(defn 
  reduce-projects!
  "Clojure reduce over all projects in the workspace.
   Destructive as it opens and closes each project sequentially."
  [f initval projects]
  (reduce 
    (fn [sofar p]
      (workspace-project-open! p)
      (workspace-wait-for-builds-to-finish)
      (let [result (f sofar p)]
        (workspace-project-close! p)
        result))
    initval
    projects
   ))
  
(defn 
  ekeko-reduce-projects!
  "Like reduce-projects!, but also enables the Ekeko nature
   before the application of f."
  [f initval projects]
  (reduce 
    (fn [sofar p]
        (workspace-project-open! p)
        (workspace-project-enable-ekeko! p)
        (workspace-wait-for-builds-to-finish)
        (let [result (f sofar p)]
          (workspace-project-disable-ekeko! p)
          (workspace-project-close! p)
          result))
    initval
    projects
   ))

(defn 
  workspace-enable-ekeko-sequentially-and-do!
  "Sequentially:
   - opens
   - enables Ekeko nature
   - waits for build to finish
   - applies f 
   - disables Ekeko nature
   - closes
  each project in the Eclipse workspace. 
  Useful for scripting queries over a large workspace."
  [f]
  (map-workspace 
    (fn [p] 
      (workspace-project-open! p)
      (workspace-project-enable-ekeko! p)
      (workspace-wait-for-builds-to-finish)
      (let [result (f p)]
        (workspace-project-disable-ekeko! p)
        (workspace-project-close! p)
        result))))



