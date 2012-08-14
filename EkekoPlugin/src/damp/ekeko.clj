(ns 
  ^{:doc "Main Ekeko namespace."
    :author "Coen De Roover"}
  damp.ekeko
  (:refer-clojure :exclude [==])
  (:use clojure.core.logic)
  (:use [damp.ekeko logic gui])
  (:use [damp.ekeko.jdt reification basic soot])
  (:use [damp.qwal])
  (:import [org.eclipse.core.runtime.jobs Job]
           [org.eclipse.core.resources ResourcesPlugin]))

(set! *warn-on-reflection* true)

;TODO: (core.logic 0.8 enables multiple variables in run)
;(def #^{:macro true} ekeko #'run)

(defmacro 
  ekeko
  "Equivalent to core.logic's run* with disabled occurs check,
   but takes multiple logic variables as its first argument.
   
   Example usage:
   (ekeko [?inv ?child]
     (ast :MethodInvocation ?inv) 
     (child :arguments ?inv ?child))

   Will become a mere alias for run* in core.logic > 0.8

   See also:
   ekeko* which opens a graphical view on the solutions."
  [logicvars & goals]
  `(run-nc* [resultvar#] 
         (fresh [~@logicvars]
                (equals resultvar# [~@logicvars])
                ~@goals)))

(defmacro 
  ekeko*
  "Equivalent to core.logic's run* with disabled occurs check,
   but takes multiple logic variables as its first argument
   and opens an Eclipse view on the solutions.
 
   Example usage:
   (ekeko* [?inv ?child]
     (ast :MethodInvocation ?inv) 
     (child :arguments ?inv ?child))

   See also:
   ekeko which doesn't open a view on the solutions"
  [vars & goals]
  `(let [querystr# (str '(ekeko* [~@vars] ~@goals))
         start# (System/nanoTime)
         resultsqc# (run-nc* [resultvar#] 
                          (fresh [~@vars]
                                 (equals resultvar# [~@vars])
                                 ~@goals))
         elapsed#  (/ (double (- (System/nanoTime) start#)) 1000000.0)
         cnt# (count resultsqc#)]
     (eclipse-uithread-return (fn [] (open-barista-results-viewer* querystr# '(~@vars) resultsqc# elapsed# cnt#)))))
      
;TODO: eliminate code duplication
(defmacro 
  ekeko-n* 
  "Equivalent to core.logic's run with disabled occurs check,
   hence computing at most the number of solutions indicated
   by its first argument ,
   but takes multiple logic variables as its second argument
   and opens an Eclipse view on the solutions.
 
   Example usage:
   (ekeko-n* 100 [?inv ?child]
     (ast :MethodInvocation ?inv) 
     (child :arguments ?inv ?child))

   See also:
   ekeko which doesn't open a view on the solutions"
  [n vars & goals]
  `(let [querystr# (str '( ekeko-n* [~@vars] ~@goals))
         start# (System/nanoTime)
         resultsqc# (run-nc ~n [resultvar#] 
                          (fresh [~@vars]
                                 (equals resultvar# [~@vars])
                                 ~@goals))
         elapsed#  (/ (double (- (System/nanoTime) start#)) 1000000.0)
         cnt# (count resultsqc#)]
     (eclipse-uithread-return (fn [] (open-barista-results-viewer* querystr# '(~@vars) resultsqc# elapsed# cnt#)))))



     

;Example repl session
(comment
  (use 'damp.ekeko)
  (in-ns 'damp.ekeko)
  
  (ekeko* [?m] (ast :MethodDeclaration ?m))
  (ekeko* [?cu] (ast :CompilationUnit ?cu))
  (ekeko* [?cu] (ast :StringLiteral ?cu))
  
  (ekeko* [?t ?b] (ast :TypeDeclaration ?t) (equals ?b (.resolveBinding ?t)))
  
  (ekeko* [?cu ?key ?child] (ast :CompilationUnit ?cu) (has ?key ?cu ?child))
  
  (ekeko* [?node ?nodetype ?childtype ?child] (ast ?nodetype ?node) (has ?childtype ?node ?child))
  
  (ekeko* [?node ?child ?keyword] (child ?keyword ?node ?child)) 
  
  (ekeko* [?cu ?exp] (ast :MethodDeclaration ?cu) (child+ ?cu ?exp) (succeeds (damp.ekeko.jdt.astnode/actual-expression? ?exp)))   
  
  (ekeko* [?inv] (ast :MethodInvocation ?inv) (has :expression ?inv nil))
  
  (ekeko* [?inv ?child] (ast :MethodInvocation ?inv) (child :arguments ?inv ?child))

  (ekeko* [?m ?o] (ast :MethodDeclaration ?m) (fresh [?os]
                                                  (equals ?os (damp.ekeko.jdt.javaprojectmodel/method-overriders ?m))
                                                  (contains ?os ?o)))
  
  (ekeko* [?i ?m] (ast :MethodInvocation ?i) (fresh [?ts]
                                                  (equals ?ts (damp.ekeko.jdt.javaprojectmodel/invocation-targets ?i))
                                                  (contains ?ts ?m)))
  
  (ekeko* [?import] (ast :ImportDeclaration ?import))
  
  (ekeko* [?import ?binding] (import-declaration-imports-binding ?import ?binding))
  
  (ekeko* [?n ?key ?binding] (ast-declares-binding  ?n ?key ?binding))
  
  (ekeko* [?import ?package] (import-declaration-imports-package ?import ?package))
  
  (ekeko* [?import ?package ?name] (import-declaration-imports-package ?import ?package) (equals ?name (.getName ?package)))
  
  (ekeko* [?import ?package ?components] (import-declaration-imports-package ?import ?package) (equals ?components (vec (.getNameComponents ?package))))
    
  
  (ekeko* [?qualifiedPackageName ?qualifiedTypeName ?typeNode] 
          (fresh [?typeBinding ?packageBinding]
                 (ast :Type ?typeNode) 
                 (!= nil ?typeBinding)
                 (equals ?typeBinding (.resolveBinding ?typeNode))
                 (equals false (.isFromSource ?typeBinding))
                 (equals false (.isPrimitive ?typeBinding))
                 (equals ?qualifiedTypeName (.getQualifiedName ?typeBinding))
                 (!= nil ?packageBinding)
                 (equals ?packageBinding (.getPackage ?typeBinding))
                 (equals ?qualifiedPackageName (.getName ?packageBinding)) 
                 ))
  
  
  (ekeko* [?m ?cfg] (method-cfg ?m ?cfg))
  
  (ekeko* [?m ?cfg ?entry ?end]
          (method-cfg ?m ?cfg) 
          (method-cfg-entry ?m ?entry)
          (fresh [?beforeReturn ?return]
                 (qwal ?cfg ?entry ?end []
                       (qcurrent [currentStatement]
                                 (equals currentStatement ?beforeReturn))
                       q=>
                       (qcurrent [currentStatement]
                                 (equals currentStatement ?return)
                                 (ast :ReturnStatement ?return)))))
  
  (ekeko-n* 100 [?m ?beforeReturn ?return]
            (fresh [?end ?entry ?cfg]
                   (method-cfg ?m ?cfg)
                   (method-cfg-entry ?m ?entry)
                   (qwal ?cfg ?entry ?end 
                         []
                         (q=>*)
                         (qcurrent [currentStatement] (equals currentStatement ?beforeReturn))
                         (q=>*)
                         (qcurrent [currentStatement] (equals currentStatement ?beforeReturn))
                         (q=>*)
                         (qcurrent [currentStatement] (equals currentStatement ?return) (ast :ReturnStatement ?return)))))
  
  ;following assume whole-program analyses have been enabled on a single project
  (ekeko* [?c] (soot :class ?c))
  
  (ekeko* [?m] (soot :method ?m))
  
  (ekeko* [?c ?m] (soot-class-method ?c ?m))
  
  (ekeko* [?c ?f] (soot-class-field ?c ?f))
  
  (ekeko* [?m ?b] (soot-method-body ?m ?b))
  
  (ekeko* [?m ?cfg] (soot-method-cfg ?m ?cfg))
  
  (ekeko* [?m ?cfg ?entry] (soot-method-cfg-entry ?m ?cfg ?entry))
  
  (ekeko* [?m ?cfg ?exit] (soot-method-cfg-exit ?m ?cfg ?exit))
  
  
  ;Query quantifies over all paths through a SOOT CFG from an entry point ?entry to an exit point ?exit where the exit point uses a value ?defval defined by a previous Soot statement ?unit that uses a value ?usedval of type ?keyw.
  (ekeko*  [?m ?entry ?exit ?unit ?keyw]
           (fresh [?cfg ?defbox ?exitbox ?usebox ?defval ?usedval]
                  (soot-method-cfg-entry ?m ?cfg ?entry)
                  (soot-method-cfg-exit ?m ?cfg ?exit)
                             (qwal ?cfg ?entry ?exit 
                                   []
                                   (q=>*)
                                   (qcurrent [curr] 
                                             (equals curr ?unit) 
                                             (soot-unit-defbox ?unit ?defbox) 
                                             (soot-valuebox-value ?defbox ?defval) 
                                             (soot-unit-usebox ?unit ?usebox)
                                             (soot-valuebox-value ?usebox ?usedval) 
                                             (soot-value ?keyw ?usedval))
                                   (q=>+)
                                   (qcurrent [curr]
                                             (equals curr ?exit) 
                                             (soot-unit-usebox ?exit ?exitbox) 
                                             (soot-valuebox-value ?exitbox ?defval)))))
  
  (ekeko*  [?ast ?unit ?local] 
           (fresh [?model]
                  (ast-reference-model-soot-unit-local ?ast ?model ?unit ?local)))
  
  
  ;Eclipse<->Soot mapping is still very basic
  (ekeko*  [?ast ?unit ?local ?unit1 ?local1] 
           (fresh [?model]
                  (ast-reference-model-soot-unit-local ?ast ?model ?unit ?local)
                  (ast-reference-model-soot-unit-local ?ast ?model ?unit1 ?local1)
                  (!= ?local ?local1)))
  
  (ekeko*  [?ast ?set]
           (fresh [?model]
                  (ast-reference-soot-model-points-to ?ast ?model ?set)))
  
  ;DANGER: slow on non-trivial programs
  (ekeko-n* 100
            [?ast1 ?ast2]
            (fresh [?model]
                   (ast-references-soot-model-may-alias ?ast1 ?ast2 ?model)))
  
  
  
  ;TODO: following gives no results
  ;     (ekeko* [?m ?p ?owner ?parentKeyInParent ?grandParentNode ?parentNode]
  ;        (ast :MethodDeclaration ?m) (child :parameters ?m ?p) (has ?parentKeyInParent ?parentNode ?p))
  ;while the child-variants loops
  ;        (ekeko* [?m ?p ?owner ?parentKeyInParent ?grandParentNode ?parentNode]
  ;       (ast :MethodDeclaration ?m) (child :parameters ?m ?p) (child ?parentKeyInParent ?parentNode ?p))
  
  
  ;manually determine ProjectModel instances to be queried 
  ;(will be filtered to JavaProjectModel instances by ast reification predicates)
  (binding [damp.ekeko.ekekomodel/*queried-project-models* 
            (filter (fn [project-model] 
                      (= "JHotDraw51" (.getName (.getProject project-model))))
                    (all-project-models))]
    (ekeko* [?cu] (ast :CompilationUnit ?cu)))
  
  
  
  )    


