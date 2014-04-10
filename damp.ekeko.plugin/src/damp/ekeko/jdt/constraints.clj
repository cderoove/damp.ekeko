(ns damp.ekeko.jdt.constraints
  (:require [clojure.core.logic :as l]
            [clojure.core.logic.fd :as fd]
            [clojure.core.logic.protocols :as prot]
            [clojure.set :as set]
            [damp.ekeko.jdt 
             [reification :as reification]
             [javaprojectmodel :as javaprojectmodel]
             [astnode :as astnode]])
  (:import [changenodes.comparing.BreadthFirstNodeIterator]))

;;Constraints for Tree Structures
(defprotocol IChildable
  (-child [parentdom childdom])
  (-parent [childdom parentdom]))

(defprotocol IAstType
  (-intersection-ast-type [ast-domain type-domain]))

(defprotocol ITypeAst
  (-intersection-type-ast [type-domain ast-domain]))

(defprotocol IAstProperty
  (-intersection-ast-property [ast-domain property-domain])
  (-intersection-ast-parent-property [ast-domain property-domain]))
  
(defprotocol IPropertyAst
  (-intersection-property-ast [property-domain ast-domain])
  (-intersection-property-parent-ast [property-domain ast-domain]))

;;Helpers
(defn parent? [parent child]
  (if (nil? child)
    false
    (let [new-parent (.getParent child)]
      (if (= new-parent parent)
        true
        (recur parent new-parent)))))


(defn keyword-instance? [keyword x]
 (instance? (astnode/class-for-ekeko-keyword keyword) x))

(defn node-has-property? [node property]
  (contains? (astnode/reifiers node) property))

(defn node-accessible-via-property? [node property]
  (= property
     (astnode/ekeko-keyword-for-property-descriptor (astnode/owner-property node))))

;;ASTDomain
(declare 
  set->astdomain set->typedomain set->propertydomain
  singleton-node? singleton-type? singleton-property?)

(deftype ASTDomain [nodes]
  Object
  (equals [this that]
    (and (instance? ASTDomain that)
         (= nodes (:nodes that))))
  
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (case k
      :nodes nodes
      not-found))
  
  fd/ISet
  (-member? [this n]
    (contains? nodes n))

  (-disjoint? [this that]
    (if (singleton-node? that) 
      (not (fd/-member? this that))
      (empty? (set/intersection nodes (:nodes that)))))
  
  (-intersection [this that]
    (if (singleton-node? that)
      (when (fd/-member? this that) 
        that)
      (set->astdomain (set/intersection nodes (:nodes that)))))
    
  (-difference [this that]
    (if (singleton-node? that) 
      (set->astdomain (disj nodes that))
      (set->astdomain (set/difference nodes (:nodes that)))))
  
  prot/IMemberCount
  (-member-count [this] 
    (count nodes))
  
  prot/IMergeDomains
  (-merge-doms [this that]
    (fd/-intersection this that))
  
  fd/IIntervals
  (-intervals [_] (seq nodes))
  
  IChildable
  (-child [this that] ;;this must be a child of that
    (if (singleton-node? that) 
      (set->astdomain (set (filter #(parent? that %) nodes)))
      (let [other-nodes (:nodes that)]
        (set->astdomain
          (set
            (filter 
              (fn [child]
                (some (fn [parent] (parent? parent child)) other-nodes))
              nodes))))))
  
  (-parent [this that] ;;this must be a parent of that
    (if (singleton-node? that)
      (set->astdomain (set (filter #(parent? % that) nodes)))
      (let [other-nodes (:nodes that)]
        (set->astdomain
          (set
            (filter 
              (fn [parent]
                (some (fn [child] (parent? parent child)) other-nodes))
              nodes))))))
  
  IAstType
  (-intersection-ast-type [this that] ;;this must be of a type in that
    (if (singleton-type? that)
      (set->astdomain (set (filter #(keyword-instance? that %) nodes)))
      (let [types (:types that)]
        (set->astdomain
          (set
            (filter
              (fn [node]
                (some
                  (fn [type]
                    (keyword-instance? type node))
                  types))
              nodes))))))
  IAstProperty
  (-intersection-ast-property [this that]
    (if (singleton-property? that)
      (set->astdomain (set (filter #(node-has-property? % that) nodes)))
      (let [properties (:properties that)]
        (set->astdomain
          (set
            (filter
              (fn [node]
                (some
                  (fn [property]
                    (node-has-property? node property))
                  properties)
                nodes)))))))
  (-intersection-ast-parent-property [this that]
    (if (singleton-property? that)
      (set->astdomain (set (filter #(node-accessible-via-property? % that) nodes)))
      (let [properties (:properties that)]
        (set->astdomain
          (set
            (filter
              (fn [node]
                (some
                  (fn [property]
                    (node-accessible-via-property? node property))
                  properties))
              nodes)))))))
          

(extend-type org.eclipse.jdt.core.dom.ASTNode
  prot/IMemberCount
  (-member-count [this] 1)
  fd/ISet
  (-member? [this that]
    (if (singleton-node? that)
      (= this that)
      (fd/-member? that this)))
  (-disjoint? [this that]
    (if (singleton-node? that)
      (not= this that)
      (fd/-disjoint? that this)))
  (-intersection [this that]
    (if (singleton-node? that)
      (when (= this that)
        this)
      (fd/-intersection that this)))
  (-difference [this that]
    (if (singleton-node? that)
      (when (not= this that)
        this)
      (fd/-difference that this)))
  IChildable
  (-child [this that] ;;this must be a child of that
    (if (singleton-node? that) 
      (when (parent? that this)
        this)
      (-child that this)))
  (-parent [this that] ;;this must be a parent of that
    (if (singleton-node? that)
      (when (parent? this that)
        this)
      (-child that this)))
  IAstType
  (-intersection-ast-type [this that]
    (if (singleton-type? that)
      (when (keyword-instance? that this)
        this)
      (when (some #(keyword-instance? % this) (:types that))
        this)))
  IAstProperty
  (-intersection-ast-property [this that]
    (if (singleton-property? that)
      (when (node-has-property? this that)
        this)
      (when (some #(node-has-property? this %) (:properties that))
        this)))
  (-intersection-ast-parent-property [this that]
    (if (singleton-property? that)
      (when (node-accessible-via-property? this that)
        this)
      (when (some #(node-accessible-via-property? this %) (:properties that))
        this))))

(defn singleton-node? [a-node]
  (instance? org.eclipse.jdt.core.dom.ASTNode a-node))

(defn astdomain? [x]
  (or (singleton-node? x)
      (instance? ASTDomain x)))

(extend-protocol prot/IForceAnswerTerm
  ASTDomain
  (-force-ans [v x]
        ((fd/map-sum (fn [n] (l/ext-run-csg x n))) (seq (:nodes v)))))
  

(defn set->astdomain [a-set]
  (if (= (count a-set) 1)
    (first a-set)
    (ASTDomain. a-set)))



(deftype TypeDomain [types]
  Object
  (equals [this that]
    (and (instance? TypeDomain that)
         (= (:types that) types)))
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (case k
      :types types
      not-found))
 
  fd/ISet
  (-member? [this n]
    (contains? types n))

  (-disjoint? [this that]
    (if (singleton-type? that)
      (not (fd/-member? this that))
      (empty? (set/intersection types (:types that)))))
  
  (-intersection [this that]
    (if (singleton-type? that)
      (when (fd/-member? this that)
        that)
      (set->typedomain (set/intersection types (:types that)))))
    
  (-difference [this that]
    (set->typedomain (set/difference types (:types that))))
  
  prot/IMemberCount
  (-member-count [this] 
    (count types))
  
  prot/IMergeDomains
  (-merge-doms [this that]
    (set->typedomain (set/intersection types (:types that))))
  
  fd/IIntervals
  (-intervals [_] (seq types))
  
  ITypeAst
  (-intersection-type-ast [this that]
    (if (singleton-node? that)
      (set->typedomain
        (set (filter #(keyword-instance? % that) types)))
      (set->typedomain
        (set
          (filter 
            (fn [type]
              (some (fn [node]
                      (keyword-instance? type node))
                    (:nodes that)))
            types))))))


;;Singleton TypeDomain and PropertyDomain
(extend-type clojure.lang.Keyword
  fd/ISet
  (-member? [this n]
    (= this n))

  (-disjoint? [this that]
    (if (or (singleton-type? that) (singleton-property? that))
      (not= this that)   
      (fd/-disjoint? that this)))
  
  (-intersection [this that]
    (if (or (singleton-type? that) (singleton-property? that))
      (when (= that this)
        this)
      (fd/-intersection that this)))
      
  (-difference [this that]
    (if (or (singleton-type? that) (singleton-property? that))
      (when (not= that this)
        this)
      (fd/-difference that this)))
  
  prot/IMemberCount
  (-member-count [this] 
    1)
  
  prot/IMergeDomains
  (-merge-doms [this that]
    (fd/-intersection this that))
    
  ITypeAst
  (-intersection-type-ast [this that]
    (if (singleton-node? that)
      (when (keyword-instance? this that)
        this)
      (when (some #(keyword-instance? this %) (:nodes that))
        this)))
  IPropertyAst
  (-intersection-property-ast [this that]
    (if (singleton-node? that)
      (when (node-has-property? that this)
        this)
      (when (some #(node-has-property? % this) (:nodes that))
        this)))
  (-intersection-property-parent-ast [this that]
    (if (singleton-node? that)
      (when (node-accessible-via-property? that this)
        this)
      (when (some #(node-accessible-via-property? % this) (:nodes that))
        this))))

(defn singleton-type? [x]
  (keyword? x))

(defn singleton-property? [x]
  (keyword? x))

(extend-protocol prot/IForceAnswerTerm
  TypeDomain
  (-force-ans [v x]
    ((fd/map-sum (fn [n] (l/ext-run-csg x n))) (seq (:types v)))))


(defn set->typedomain [set]
  (if (= (count set) 1)
    (first set))
    (TypeDomain. set))

(defn keyword->typedomain [keyword]
  (set->typedomain (set (list keyword))))



(deftype PropertyDomain [properties]
  Object
  (equals [this that]
    (and (instance? PropertyDomain that)
         (= (:properties that) properties)))
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (case k
      :properties properties
      not-found))
 
  fd/ISet
  (-member? [this n]
    (contains? properties n))

  (-disjoint? [this that]
    (if (singleton-property? that)
      (not (fd/-member? this that))
      (empty? (set/intersection properties (:properties that)))))
  
  (-intersection [this that]
    (if (singleton-property? that)
      (when (fd/-member? this that)
        that)
      (set->propertydomain (set/intersection properties (:properties that)))))
    
  (-difference [this that]
    (set->propertydomain (set/difference properties (:properties that))))
  
  prot/IMemberCount
  (-member-count [this] 
    (count properties))
  
  prot/IMergeDomains
  (-merge-doms [this that]
    (set->propertydomain (set/intersection properties (:properties that))))
  
  fd/IIntervals
  (-intervals [_] (seq properties))
  
  IPropertyAst
  (-intersection-property-ast [this that]
    (if (singleton-node? that)
      (set->propertydomain
        (set (filter #(node-has-property? that %) properties)))
      (set->propertydomain
        (set
          (filter 
            (fn [property]
              (some (fn [node]
                      (node-has-property? node property))
                    (:nodes that)))
            properties))))))


(defn set->propertydomain [a-set]
  (if (= (count a-set) 1)
    (first a-set)
    (PropertyDomain. a-set)))


(extend-protocol prot/IForceAnswerTerm
  PropertyDomain
  (-force-ans [v x]
        ((fd/map-sum (fn [n] (l/ext-run-csg x n))) (seq (:properties v)))))
  

;;Childconstraint
(defn -childc [p c]
  (reify
    clojure.core.logic.protocols.IEnforceableConstraint
    prot/IConstraintStep
    (-step [this s]
      (l/let-dom s [p dp c dc]
        (reify
          clojure.lang.IFn
          (invoke [_ s]
           ((l/composeg
              (fd/process-dom p (-parent dp dc) dp)
              (fd/process-dom c (-child dc dp) dc)) s))
          prot/IEntailed
          (-entailed? [_]
            (and (singleton-node? dp)
                 (singleton-node? dc)
                 (-parent dp dc)))
          prot/IRunnable
          (-runnable? [_]
            (and dp dc)))))
   prot/IConstraintOp
   (-rator [_] `childc)
   (-rands [_] [p c])
   prot/IConstraintWatchedStores
   (-watched-stores [this]
     #{::l/subst ::l/fd})))

(defn childc [p c]
  (l/cgoal (-childc p c)))



(defn -astc [type node]
  (reify
    clojure.core.logic.protocols.IEnforceableConstraint
    prot/IConstraintStep
    (-step [this s]
        (l/let-dom s [node dnode type dtype]
          (reify
            clojure.lang.IFn
            (invoke [_ s]
              (let [type-domain (-intersection-type-ast dtype dnode)
                    node-domain (-intersection-ast-type dnode dtype)]
                ((l/composeg
                   (fd/process-dom type type-domain dtype)
                   (fd/process-dom node node-domain dnode)) s)))
            prot/IEntailed
            (-entailed? [_]
                        (and (singleton-node? dnode)
                             (singleton-type? dtype)
                             (keyword-instance? dtype dnode)))
            prot/IRunnable
            (-runnable? [_]
              (and dnode dtype)))))
   prot/IConstraintOp
   (-rator [_] `astc)
   (-rands [_] [type node])
   prot/IConstraintWatchedStores
   (-watched-stores [this]
     #{::l/subst ::l/fd})))

(defn astc [type node]
  (l/all
    (fd/dom type (set->typedomain (set astnode/ekeko-keywords-for-ast-classes)))
    (l/cgoal (-astc type node))))


(defn make-domain [node-type]
  (set->astdomain (set (reification/nodes-of-type node-type))))



(defn -hasc [property node child]
  (reify
    clojure.core.logic.protocols.IEnforceableConstraint
    prot/IConstraintStep
    (-step [this s]
        (l/let-dom s [node dnode property dproperty child dchild]
          (reify
            clojure.lang.IFn
            (invoke [_ s]
              (cond (and dnode dproperty)
                    (let [node-domain (-intersection-ast-property dnode dproperty)
                          property-domain (-intersection-property-ast dproperty dnode)]
                      ((l/composeg*
                         (fd/process-dom property property-domain dproperty)
                         (fd/process-dom node node-domain dnode)
                         (if (and (singleton-node? node-domain) (singleton-property? property-domain))
                           (fd/process-dom child ((property-domain (astnode/reifiers node-domain))) dchild)
                           identity))
                           s))
                    (and dchild dproperty)
                    (let [child-domain (-intersection-ast-parent-property dchild dproperty)
                          property-domain (-intersection-property-parent-ast dproperty dchild)]
                      ((l/composeg*
                         (fd/process-dom child child-domain dchild)
                         (fd/process-dom property property-domain dproperty)
                         (if (singleton-node? child-domain) ;;property should be a singleton now as well
                           (fd/process-dom node (astnode/owner child) dnode)
                           identity))
                        s))
                    (singleton-node? dnode)
                    (let [prop-domain (set->propertydomain 
                                        (set (keys (astnode/reifiers dnode))))]
                      ((fd/process-dom property prop-domain dproperty) s))))
            prot/IEntailed
            (-entailed? [_]
              (and (singleton-node? dnode)
                   (singleton-property? dproperty)
                   (= child (dproperty (astnode/reifiers dnode)))))
            prot/IRunnable
            (-runnable? [_]
              (or (and dnode dproperty)
                  (and (astdomain? dchild) dproperty)
                  (singleton-node? dnode))))))
              
   prot/IConstraintOp
   (-rator [_] `hasc)
   (-rands [_] [property node child])
   prot/IConstraintWatchedStores
   (-watched-stores [this]
     #{::l/subst ::l/fd})))


(defn hasc [property node child]
  (l/cgoal
    (-hasc property node child)))


;;Domain helpers
(defn get-nodes [ast]
  (iterator-seq (changenodes.comparing.BreadthFirstNodeIterator. ast)))

(defn ground-ast [node ast]
  (l/all
    (l/project [ast]
               (fd/dom node (set->astdomain (set (get-nodes ast)))))))