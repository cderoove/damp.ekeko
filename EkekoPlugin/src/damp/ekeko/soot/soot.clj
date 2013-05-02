(ns 
  ^{:doc "Relations about SOOT whole-program analyses."
    :author "Coen De Roover"}
  damp.ekeko.soot.soot
  (:refer-clojure :exclude [== type])
  (:use [clojure.core.logic])
  (:use [damp.ekeko logic])
  (:require 
    [damp.ekeko.jdt [astnode :as astnode]] ;only for ekeko-keyword etc, should be refactored
    [damp.ekeko [ekekomodel :as ekekomodel]]
    [damp.ekeko.soot [projectmodel :as projectmodel]])
  (:import 
    [java.util Iterator]
    [damp.ekeko JavaProjectModel EkekoModel]
    [damp.ekeko.soot SootProjectModel]
    [soot Context MethodOrMethodContext PointsToAnalysis SootMethodRef Local Scene PointsToSet SootClass SootMethod SootField Body Unit ValueBox Value PatchingChain]
    [soot.jimple Stmt JimpleBody StaticFieldRef InstanceFieldRef]
    [soot.tagkit SourceLnPosTag LineNumberTag]
    [soot.jimple.internal JAssignStmt JimpleLocal JInstanceFieldRef JIdentityStmt]
    [soot.toolkits.graph ExceptionalUnitGraph]
    [soot.jimple.toolkits.callgraph Edge CallGraph]
    [soot.jimple.toolkits.pointer StrongLocalMustAliasAnalysis LocalMustNotAliasAnalysis LocalMustAliasAnalysis]
    ))

;; Soot reification predicates
;; ---------------------------


(defn
  soot-model 
  "Unifies ?model with the current Soot model available to Ekeko,
   or fails when there is currently none."
  [?model]
  (all 
    (!= nil ?model)
    (equals ?model ^SootProjectModel  (projectmodel/current-soot-model))))

(defn 
  soot-model-scene
  "Relation between the current Soot model ?model and its scene.
  
  See also:
  API documentation of soot.Scene
  Unary predicate soot-model/1"
  [?model ?scene]
  (all 
    (soot-model ?model)
    (equals ?scene (.getScene ^SootProjectModel ?model))))

(declare soot-class-method)
(declare soot-class-field)


(defn 
  soot
  "Relation between a SootClass, SootMethod or SootField instance
   ?node and a keyword ?keyword describing its kind. 
   Keyword is one of :class, :method or :field. The instances
   originate from the current Soot model available to Ekeko.

  Examples:
  ;;all SootClass instances
  (ekeko* [?c] (soot :class ?c))
  ;;all SootMethod instances
  (ekeko* [?m] (soot :method ?m))"
  [?keyword ?node]
  (fresh [?model ?scene]
         (soot-model-scene ?model ?scene)
         (conda [(== ?keyword :class) 
                 (conda [(v+ ?node) (all
                                      (succeeds (instance? SootClass ?node))
                                      (succeeds (.isApplicationClass ^SootClass ?node)))]
                        [(v- ?node) (fresh [?classes]
                                           (equals ?classes (.getApplicationClasses ^Scene ?scene))
                                           (contains ?classes ?node))])]
                [(== ?keyword :method) 
                 (conda [(v+ ?node) (fresh [?class]
                                      (succeeds (instance? SootMethod ?node))
                                      (equals ?class (.getDeclaringClass ^ SootMethod ?node))
                                      (!= nil ?class)
                                      (succeeds (.isApplicationClass ^SootClass ?class)))]
                        [(v- ?node) (fresh [?class]
                                           (soot-class-method ?class ?node))])]
                 [(== ?keyword :field) 
                 (fresh [?class]
                        (soot-class-field ?class ?node))]
                )))

;TODO: macro abstracting communalities between next two
(defn 
  soot-class-method
  "Relation between SootClass instances ?class and 
   their SootMethod instances ?method.

  Examples:
  ;;for each class, all methods
  (ekeko* [?c ?m] (soot-class-method ?c ?m))
  "
  [?class ?method]
  (conda [(v+ ?method) (all 
                          (succeeds (instance? SootMethod ?method))
                          (equals ?class (.getDeclaringClass ^SootMethod ?method))
                          (soot :class ?class))]
         [(v- ?method) (fresh [?methods]
                         (soot :class ?class)
                         (equals ?methods (.getMethods ^SootClass ?class))
                         (contains ?methods ?method))]))

(defn
  soot-class-field
  "Relation between SootClass instances ?class 
   and their SootField instances ?field.

  Examples:
  ;;for each class, all fields
  (ekeko* [?c ?f] (soot-class-field ?c ?f))
  "
  [?class ?field]
  (conda [(v+ ?field) (all 
                          (succeeds (instance? SootField ?field))
                          (equals ?class (.getDeclaringClass ^SootField ?field))
                          (soot :class ?class))]
         [(v- ?field) (fresh [?fields]
                         (soot :class ?class)
                         (equals ?fields (.getFields ^SootClass ?class))
                         (contains ?fields ?field))]))

(defn 
  soot-field-signature
  "Relation between SootField instances ?field 
   and their signature string ?signature."
  [?field ?signature]
  (conda [(v+ ?field) (equals-without-exception ?signature (.getSignature ^SootField ?field))]         
         [(v- ?field)
          (conda [(v+ ?signature) 
                  (fresh [?model ?scene]
                         (soot-model-scene ?model ?scene)
                         (equals-without-exception ?field (.getField ^Scene ?scene ?signature)))]
                 [(v- ?signature) 
                  (fresh [?class]
                         (soot-class-field ?class ?field)
                         (soot-field-signature ?field ?signature))])]))

(defn
  soot-class-name
  "Relation between SootClass instances ?class 
   and their fully qualified name string ?name."
  [?class ?name]
  (fresh [?model ?scene]
          (!= nil ?class)
          (!= nil ?name)
          (soot-model-scene ?model ?scene)
          (conda [(v+ ?class) (all (equals ?name (.getName ^SootClass ?class)))]
                 [(v- ?class) 
                  (conda [(v+ ?name) (all (equals-without-exception ?class (.getSootClass ^Scene ?scene ?name)))]
                         [(v- ?name) (all (soot :class ?class) 
                                          (soot-class-name ?class ?name))])])))

(defn 
  soot-method-signature
  "Relation between SootMethod instances ?method 
   and their signature string ?signature."
  [?method ?signature]
  (fresh [?model ?scene]
          (!= nil ?method)
          (!= nil ?signature)
          (soot-model-scene ?model ?scene)
          (conda [(v+ ?method) (all (equals ?signature (.getSignature ^SootMethod ?method)))]
                 [(v- ?method) 
                  (conda [(v+ ?signature) (all (equals-without-exception ?method (.getMethod ^Scene ?scene ?signature)))]
                         [(v- ?signature) (all (soot :method ?method) 
                                          (soot-method-signature ?method ?signature))])])))
(defn 
  soot-method-name
  "Relation between SootMethod instances ?method 
   and their short name string ?name."
  [?method ?name]
  (all
    (soot :method ?method)
    (equals ?name (.getName ^SootMethod ?method))))     

                                                  
(defn 
  soot-entry-method 
  "Unifies ?method with the SootMethod instance that was 
   used as the entry point for Soot's whole-program analyses."
  [?method]
  (fresh [?model ?scene]
         (soot-model-scene ?model ?scene)
         (equals ?method (.getMainMethod ^Scene ?scene))))

(defn 
  soot-class-initializer-method
  "Relation between SootClass instances and 
   their static initializer SootMethod."
  [?class ?method]
  (fresh [?name]
         (soot-class-method ?class ?method) 
         (soot-method-name ?method ?name)
         (equals ?name (SootMethod/staticInitializerName))))

(defn 
  soot-class-constructor-method
  "Relation between SootClass instances 
   and their SootMethod constructors."
  [?class ?method]
  (fresh [?name]
    (soot-class-method ?class ?method) 
    (soot-method-name ?method ?name)
    (equals ?name (SootMethod/constructorName))))


(defn 
  soot-method-body
  "Relation between active SootMethod 
   instances and their JimpleBody.

  Examples: 
  ;;all methods with an active jimple body
  (ekeko* [?m ?b] (soot-method-body ?m ?b))

  See also:
  API reference of soot.jimple.JimpleBody"
  [?method ?body] 
  (conda [(v+ ?body) (all 
                          (succeeds (instance? JimpleBody ?body))
                          (equals ?method (.getMethod ^Body ?body))
                          (soot :method ?method))]
         [(v- ?body) (all
                         (soot :method ?method)
                         (succeeds (.hasActiveBody ^SootMethod ?method))
                         (equals ?body (.getActiveBody ^SootMethod ?method)))]))

(defn-
  qwal-graph-from-soot-cfg 
  [soot-cfg]
  {:soot-cfg soot-cfg
   :predecessors (fn 
                   [node to]
                   (all
                     (project [node]
                              (== to (seq (.getPredsOf ^ExceptionalUnitGraph soot-cfg node))))))
   :successors (fn 
                 [node to ]
                 (all 
                   (project [node] 
                            (== to (seq (.getSuccsOf ^ExceptionalUnitGraph soot-cfg node))))))})


(defn
   soot-method-cfg
   "Relation between a SootMethod ?method and its 
    ExceptionUnitGraph control flow graph, in a 
    format that is suitable for being queried 
    using regular path expressions provided by
    the damp.qwal library.
   
 
    Examples:
    ;;all units on every path through a SOOT control flow graph from an entry point ?entry
    ;;to an exit point ?exit where the exit point uses a value ?defval defined
    ;;by a previous Soot statement ?unit that uses a value ?usedval of type ?keyw
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
                                     (soot-valuebox-value ?exitbox ?defval))))))
  See also:
  Documentation of the damp.qwal library.
  Predicates soot-method-cfg-entry and soot-method-cfg-exit which quantify over the 
  Soot heads and tails of the control flow graph.
  API reference of soot.toolkits.graph.ExceptionalUnitGraph"
   [?method ?cfg]
   (conde [(v+ ?cfg)
            (fresh [?body]
                   (equals ?body (.getBody ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
                   (soot-method-body ?method ?body))]
           [(v- ?cfg)
            (fresh [?body]
                   (soot-method-body ?method ?body)
                   (equals ?cfg (qwal-graph-from-soot-cfg (ExceptionalUnitGraph. ^Body ?body))))]))


(defn 
  soot-method-cfg-entry
   "Relation between a Soot ExceptionalUnitGraph 
    (in a format compatible with damp.qwal) and its entry points.
   
    See also:
    Binary predicate soot-method-cfg/2
    API reference of soot.toolkits.graph.ExceptionalUnitGraph"
  [?method ?cfg ?entry]
  (fresh [?entries]
         (soot-method-cfg ?method ?cfg)
         (equals ?entries (.getHeads ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
         (contains ?entries ?entry)))
  
(defn 
  soot-method-cfg-exit
   "Relational between a Soot ExceptionalUnitGraph
    (in a format compatible with damp.qwal) and its exit points.
   
    See also:
    Binary predicate soot-method-cfg/2
    API reference of soot.toolkits.graph.ExceptionalUnitGraph"
  [?method ?cfg ?exit]
  (fresh [?exits]
         (soot-method-cfg ?method ?cfg)
         (equals ?exits (.getTails ^ExceptionalUnitGraph (:soot-cfg ?cfg)))
         (contains ?exits ?exit)))


;; SOOT Units and Values
;; ---------------------


(defn 
  soot-method-units 
  "Relation between a SootMethod ?m and the PatchingChain of 
   units ?units in its active JimpleBody."
  [?m ?units]
  (fresh [?body] 
    (soot-method-body ?m ?body)
    (equals ?units (.getUnits ^Body ?body))))


(defn
  soot|method-soot|unit
  "Relation between a SootMethod ?m and one of the units ?unit
   in its active JimpleBody."
  [?m ?unit]
  (fresh [?units]
         (soot-method-units ?m ?units)
         (contains ?units ?unit)))
                


(defn 
  soot-method-unit-trailing-unit
  "Relation between a SootMethod ?m and two of its units 
   such that unit ?trailing comes after ?unit in 
   the PatchingChain of its JimpleBody."
  [?m ?unit ?trailing]
  (fresh [?units ?trailingUnits]
         (soot-method-units ?m ?units)
         (equals ?trailingUnits (iterator-seq (.iterator ^PatchingChain ?units ?unit)))
         (contains ?trailingUnits ?trailing)))
         
(defn 
  soot-unit 
  "Relation between soot unit ?u and a keyword ?keyw describing its kind.
   The instances originate from the current Soot model available to Ekeko.
   
   See also: 
   API documentation of soot.Unit"
  [?keyw ?u]
  (all
    (conda
      [(v+ ?u)
       (succeeds (instance? Unit ?u))]
      [(v- ?u)
       (fresh [?m ?units]
              (soot-method-units ?m ?units)
              (contains ?units ?u))])
    (equals ?keyw (astnode/ekeko-keyword-for-class (class ?u)))))

(defn
  soot-unit-defbox
  "Relation between a soot Unit ?u and one of the ValueBox instances it defines.

  See also:
  predicate soot-unit-usebox/2"
  [?u ?b]
  (fresh [?boxes ?keyw]
         (soot-unit ?keyw ?u)
         (equals ?boxes (.getDefBoxes ^Unit ?u))
         (contains ?boxes ?b)))

(def soot|unit|writes-soot|valuebox soot-unit-defbox)

(defn 
  soot-unit-usebox
  "Relation between a soot Unit ?u and one of the ValueBox instances it uses.

  See also:
  predicate soot-unit-defbox/2"
  [?u ?b]
  (fresh [?boxes ?keyw]
         (soot-unit ?keyw ?u)
         (equals ?boxes (.getUseBoxes ^Unit ?u))
         (contains ?boxes ?b)))

(def soot|unit|reads-soot|valuebox soot-unit-usebox)


(defn 
  soot-valuebox 
  "Non-relational. Verifies that ?b is a soot ValueBox."
  [?b]
  (all
    (succeeds (instance? ValueBox ?b))))

(defn 
  soot-valuebox-value
  "Non-relational. Unifies ?v with the Value inside the ValueBox ?b."
  [?b ?v]
  (all
    (soot-valuebox ?b)
    (equals ?v (.getValue ^ValueBox ?b))))

(defn 
  soot-value
  "Non-relational. Unifies ?keyw with the keyword 
   that represents the kind of the soot Value ?v."
  [?keyw ?v]
  (all
    (succeeds (instance? Value ?v))
    (equals ?keyw (astnode/ekeko-keyword-for-class (class ?v)))))


; Assignment units

(defn 
  soot-unit-assign-leftop
  "Relation between the soot JAssignStmt unit ?u 
   and its left operand ?local."
  [?u ?local]
  (all
     (soot-unit :JAssignStmt ?u)
     (equals ?local (.getLeftOp ^JAssignStmt ?u))))

(defn 
  soot-unit-assign-rightop
   "Relation between the soot JAssignStmt unit ?u 
   and its right operand ?local."
  [?u ?local]
  (all
     (soot-unit :JAssignStmt ?u)
     (equals ?local (.getRightOp  ^JAssignStmt ?u))))



;; Soot Call Graph
;; ---------------

;(defn soot-call-graph [?g]
;  (fresh [?model ?scene]
;    (soot-model-scene ?model ?scene)
;    (equals ?g (.getCallGraph ^Scene ?scene))
;    (!= ?g nil)))


;TODO: see comment in Java model, otherwise could have implemented it in core.logic rather than in Java
;something is wrong with the implementation of equals(Object) on Edge
(defn 
  soot-method-calls-method
  "Relation between SootMethod ?m and one of the SootMethod instances ?callee
   that are called from within ?m.
  
   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-called-by-method/2 is declaratively equivalent, 
   but operationally more efficient when the callee is known and the callers aren't."
  [?m ?callee] 
  (fresh [?model ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?methods (iterator-seq (.dynamicMethodCallees ^SootProjectModel ?model ?m)))
         (contains ?methods ?callee)
         (soot :method ?callee) ;application methods only
         ))


          
          
;declaratively, same as above
(defn 
  soot-method-called-by-method
   "Relation between SootMethod ?m and one of the SootMethod instances ?caller
   that invoke ?m.
  
   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-calls-method/2 is declaratively equivalent, 
   but operationally more efficient when the caller is known and the callees aren't."
  [?m ?caller] 
  (fresh [?model ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?methods (iterator-seq (.dynamicMethodCallers ^SootProjectModel ?model ?m)))
         (contains ?methods ?caller)
         (soot :method ?caller) ;application methods only
         ))

(def
  soot|method-soot|method|caller
  soot-method-called-by-method)

(defn 
  soot-method-called-by-unit 
  "Relation between SootMethod ?m and one of the Unit instances ?unit 
   it is invoked by.

   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-unit-calls-method/2 is declaratively equivalent, 
   but operationally more efficient when the unit is known and the method isn't.
  "
  [?m ?unit] 
  (fresh [?model ?keyw ?scene ?units]
         (soot-model-scene ?model ?scene)
         (soot :method ?m)
         (equals ?units (iterator-seq (.dynamicUnitCallers ^SootProjectModel ?model ?m)))
         (contains ?units ?unit)
         (soot-unit ?keyw  ?unit) 
         ))

(def
  soot|method-soot|unit|caller
  soot-method-called-by-unit)

;declaratively, same as above
(defn 
  soot-unit-calls-method
  "Relation between Unit ?unit and one of the SootMethod instances ?m
   it may invoke. 

   The relation is computed based on the points-to set of the receiver (i.e., based
   on an approximation of the dynamic rather than the static type of the receiver). 

   Note that library methods are not included. 

   See also: 
   Predicate soot-method-called-by-unit/2 is declaratively equivalent, 
   but operationally more efficient when the method is known and the unit isn't.
  "
  [?unit ?m] 
  (fresh [?model ?keyw ?scene ?methods]
         (soot-model-scene ?model ?scene)
         (soot-unit ?keyw ?unit)
         (equals ?methods (iterator-seq (.dynamicUnitCallees ^SootProjectModel ?model ?unit)))
         (contains ?methods ?m)
         (soot :method ?m) ;application methods only
         ))


  
  (defn-
    sootbody-firstunit
    [sootbody]
    (.getFirst ^PatchingChain (.getUnits ^Body sootbody)))
  

    
  (defn-
    sootbody-lastunit
    [sootbody]
    (.getLast ^PatchingChain (.getUnits ^Body sootbody)))


(defn
  jdk-node?
  [node]
  (.isJavaLibraryMethod ^SootMethod (:method node)))
  
;todo:
; -get it working
; -filter out non-app methods
; -only store call/return nodes, retrieve others on the fly
; -comptue call/return nodes from edges 



(defn
  trim-stack
  [stack]
  (vec 
    (reduce (fn [trimmed el]
              (let [el-unit (:unit el)]
                (if-let [ek (some (fn [e] (when (=  (:unit e) el-unit) e))  trimmed)]
                  (drop  (.indexOf trimmed ek) trimmed)
                  (cons el trimmed))))
            '()
            (rseq stack))))

(defn 
  make-frame 
  [unit method] 
  [unit method])

(defn 
  frame-unit
  [frame]
  (nth frame 0))

(defn 
  frame-method
  [frame]
  (nth frame 1))

(defn 
  make-icfgnode  
  [unit method stack]
  [unit method stack])

(defn
  icfgnode-unit
  [node]
  (nth node 0))

(defn
  icfgnode-method
  [node]
  (nth node 1))

(defn
  icfgnode-stack
  [node]
  (nth node 2))


(defn- 
  make-soot-icfg-successors 
  "Returns a successor function for SOOT inter-procedural CFGs. 
   Nodes are of the form {:unit aSootUnit :method aSootMethodForUnit :stack aVector}"
  []
  (let [;cache avoids multiple cfg instances per method
        method2graph
        (atom {})
        method-cfg 
        (fn [method] 
          (or
            (get @method2graph method)
            (let [graph (ExceptionalUnitGraph. (.getActiveBody ^SootMethod method))]
              (swap! method2graph assoc method graph)
              graph)))
        successors-noninvoking
        (fn  [method unit stack]
          (let [successors (.getSuccsOf ^ExceptionalUnitGraph (method-cfg method) unit)]
            (if
              (seq successors)
              (map (fn [successor]
                     (make-icfgnode successor method stack)) 
                   successors)
              (if 
                ;return to caller
                (seq stack)
                (let [frame (peek stack)
                      calling-unit (frame-unit frame) 
                      calling-method (frame-method frame)
                      successors-of-caller (.getSuccsOf ^ExceptionalUnitGraph (method-cfg calling-method) calling-unit)
                      reduced-callstack (pop stack)]
                  (map (fn [successor] 
                         (make-icfgnode successor calling-method reduced-callstack))
                       successors-of-caller))
                []))))]
    (fn 
      [icfgnode]
      (let [method (icfgnode-method icfgnode)
            unit (icfgnode-unit icfgnode)
            stack (icfgnode-stack icfgnode)]
        ;(println stack)
        (if 
          ;unit containing a method invocation
          (and 
            (.containsInvokeExpr ^Stmt unit)
            (not-any? (fn [frame] (= unit (frame-unit frame))) stack))
          
          (let [model (projectmodel/current-soot-model)
                methods (iterator-seq (.dynamicUnitCallees ^SootProjectModel model unit))
                active-methods (filter (fn [method] (.hasActiveBody ^SootMethod method)) methods)
                nonjdk-activemethods (remove (fn [method] (.isJavaLibraryMethod method)) active-methods)
                expanded-callstack  (conj stack (make-frame unit method))]
            (if
              ;(seq nonjdk-activemethods)
              (seq nonjdk-activemethods)
              (map
                (fn [callee]
                  (make-icfgnode (sootbody-firstunit (.getActiveBody callee)) callee expanded-callstack))
                nonjdk-activemethods)
              (successors-noninvoking method unit stack)))
          
          (successors-noninvoking method unit stack))))))



(defn
  node-str
  [node]
 ; (str (icfgnode-unit node) "|" (.getName (icfgnode-method node))))
  (.getName (icfgnode-method node)))

;todo: predecessors
(defn-
  qwal-interprocedural-graph-from-soot-method
  [starting-method]
  (let [successorf 
        (make-soot-icfg-successors)
        tracesuccessorf
        (fn [node]
          (let [tos (successorf node)]
            (println (node-str node) "=>" (map node-str tos))
            tos))]
    {:soot-method starting-method
     :successors (fn [node tos]
                   (all
                     (project [node]
                              (== tos (successorf node))
                              )))}))

(defn
  soot-method-icfg
  "Relation between a SootMethod and the program's interprocedural
   control flow graph starting in that method, in a format that is suitable for being
   queried using regular path expressions provided by the damp.qwal library."
  [?method ?icfg]
  (conde [(v+ ?icfg)
          (equals ?method (:soot-method ?icfg))]
         [(v- ?icfg)
          (soot :method ?method)
          (equals ?icfg (qwal-interprocedural-graph-from-soot-method ?method))]))

(defn
  soot-method-icfg-entry
  [?method ?icfg ?entry]
  (fresh [?body]
         (soot-method-icfg ?method ?icfg)
         (soot-method-body ?method ?body)
         (equals ?entry (make-icfgnode (sootbody-firstunit ?body) ?method [])))) 

(defn
  soot-method-icfg-exit
  [?method ?icfg ?exit]
  (fresh [?cfg  ?body]
         (soot-method-icfg ?method ?icfg)        
         (soot-method-body ?method ?body)
         (equals ?exit (make-icfgnode (sootbody-lastunit ?body) ?method [])))) 

;; ALIASING
;; --------

(defn 
  soot-method-local-must-alias-analysis
  "Relation between a SootMethod ?m and 
  its intra-procedural must alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (LocalMustAliasAnalysis. ?unitgraph))))

(defn 
  soot-method-local-must-not-alias-analysis 
  "Relation between a SootMethod ?m and 
  its intra-procedural must-not alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (LocalMustNotAliasAnalysis. ?unitgraph))))

(defn 
  soot-method-local-must-alias-analysis-strong
  "Relation between a SootMethod ?m and 
  its intra-procedural strong must alias analysis ?a."
  [?m ?a]
  (fresh [?cfg ?unitgraph]
         (soot-method-cfg ?m ?cfg)
         (equals ?unitgraph (:soot-cfg ?cfg))
         (equals ?a (StrongLocalMustAliasAnalysis. ?unitgraph))))



;; Basic relations
;; ---------------

(defn
  soot|unit|reads-soot|field
  "Relation between a Soot unit and the Soot field it reads from."
  [?unit ?field]
  (fresh [?usebox ?value]
         (soot-unit-usebox ?unit ?usebox)
         (soot-valuebox-value ?usebox ?value)
         (succeeds (instance? soot.jimple.FieldRef ?value))
         (equals ?field (.getField ?value))))

(defn
  soot|unit|writes-soot|field
  "Relation between a Soot unit and the Soot field it writes to."
  [?unit ?field]
  (fresh [?usebox ?value]
         (soot-unit-defbox ?unit ?usebox)
         (soot-valuebox-value ?usebox ?value)
         (succeeds (instance? soot.jimple.FieldRef ?value))
         (equals ?field (.getField ?value))))


      
              