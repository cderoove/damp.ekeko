(ns 
  ^{:doc "Central point of access to the EkekoModel for reification relations."
    :author "Coen De Roover"}
  damp.ekeko.ekekomodel
  (:import 
    [org.eclipse.jdt.core  IMember IJavaElement ITypeHierarchy JavaCore IType IJavaModel IJavaProject IPackageFragment ICompilationUnit]
    [org.eclipse.jdt.core.dom ConstructorInvocation SuperConstructorInvocation ClassInstanceCreation SuperMethodInvocation IMethodBinding AnonymousClassDeclaration IBinding MethodDeclaration MethodInvocation ASTParser AST ASTRequestor CompilationUnit ASTNode TypeDeclaration]
    [damp.ekeko EkekoModel JavaProjectModel ProjectModel])
  (:import
    [org.eclipse.jdt.core.dom ASTNode]
    [damp.ekeko IEkekoModelUpdateListener EkekoModelRemovedEvent]))


(def #^{:dynamic true
        :doc 
        "Seq of ProjectModel instances that are to be queried. 
  
         Will be used as the source for all reification predicates if
         it is non-false, otherwise all Java project models are used 
         (see all-project-models). 

         Expected to be a subset of those on which the EkekoNature has been enabled.
         
         Example usage:
         ;;temporarily query JHotDraw51 only:
            (binding [*queried-project-models* 
                       (atom
                         (filter 
                           (fn [project-model] 
                             (= \"JHotDraw51\" (.getName (.getProject project-model))))
                        (all-project-models)))]
              (ekeko* [?cu] (ast :CompilationUnit ?cu)))"
          }
       *queried-project-models*
  nil)

(defn 
  ekeko-model
  "Returns the current EkekoModel singleton."
  []
  (EkekoModel/getInstance))

(defn 
  all-project-models
  "Returns all ProjectModel instances managed by the EkekoModel.
   Corresponds to all IProjects that have the Ekeko nature enabled."
  []
  (.getProjectModels (ekeko-model)))

(defn 
  queried-project-models
  "Returns the Seq of ProjectModel instances that are to be queried.
   Defaults to all ProjectModel instances gathered by the EkekoModel
   (see all-project-models), unless dynamic variable *queried-project-models*
   is set."
  []
  (if 
    *queried-project-models*
    @*queried-project-models*
    (all-project-models)))


(defn 
  register-listener
  "Registers EkekoModelUpdateListener ?l."
  [?l]
  (.addListener (ekeko-model) ?l))
  
(defn
  unregister-listener
  "Unregisters EkekoModelUpdateListener ?l."
  [?l]
  (.removeListener (ekeko-model) ?l))


;;helper to solve project models being out of sync
;;just remove them all
(defn remove-all-queried-models []
  (.clear (all-project-models)))



(defn
  model-update-listener
  [execute-upon-change]
  (reify 
    IEkekoModelUpdateListener
    (projectModelUpdated [this model]
                         (when
                           (instance? EkekoModelRemovedEvent model)
                           (do
                             (println "Project model no longer being queried:" model)
                             (.removeListener (ekeko-model) this)))
                         (execute-upon-change model))))

(defn
  incremental-node-manager
  "Was private (defn-); made public so damp.ekeko.baristamarkers (split out
   from this namespace, see below) can build on it."
  [node-retrieval-function
   nodes-added-function 
   nodes-removed-function]
  (let [previous-nodes (atom #{})]
    (fn [event]
      (let [nodes (into #{} (node-retrieval-function))]
        (nodes-removed-function (clojure.set/difference @previous-nodes nodes))
        (nodes-added-function (clojure.set/difference nodes @previous-nodes))
        (reset! previous-nodes nodes)))))


(defn
  incremental-node-printer
  [node-retrieval-function]
  (incremental-node-manager 
    node-retrieval-function
    (fn [added] (println "Found "  (count added) " new nodes."))
    (fn [removed] (println "Found "  (count removed) " removed nodes."))))


;; marker-utility, incremental-marker-updater and incremental-node-marker
;; used to live here, placing Eclipse IMarkers (squiggly underlines) on
;; nodes as they enter/leave a query's result set, backed by
;; baristaui.util.MarkerUtility. Moved to damp.ekeko.baristamarkers so that
;; this namespace -- required by the query engine's model-access layer --
;; can be loaded without baristaui (and therefore an Eclipse workbench)
;; present. incremental-node-manager/incremental-node-printer above (which
;; don't depend on it) are unaffected.

(comment

  (require '[damp.ekeko.baristamarkers :refer [incremental-node-marker]])
  (def
    listener
    (model-update-listener
      (incremental-node-marker
        (fn []
          (map first
            (damp.ekeko/ekeko [?m]
             (damp.ekeko.jdt.ast/ast :MethodDeclaration ?m)))))))

  (register-listener listener)
  ;change file
  (unregister-listener listener)


  )



