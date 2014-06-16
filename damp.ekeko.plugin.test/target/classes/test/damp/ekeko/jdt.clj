(ns 
  ^{:doc "Test suite for Ekeko JDT relations."
    :author "Coen De Roover"}
  test.damp.ekeko.jdt
  (:refer-clojure :exclude [== type declare])
  (:require 
    [clojure.core.logic :exclude [is] :as l]
    [test.damp [ekeko :as test]]
    [damp.ekeko [logic :as logic]]
    [damp.ekeko.jdt 
     [astnode :as astnode]
     [ast :as ast]
     [structure :as structure]
     [aststructure :as aststructure]
     [astbindings :as astbindings]
     [convenience :as convenience]
     ])
  (:use clojure.test))

;; AST Tests

(deftest
  reification-ast
  ^{:doc "Tests relational nature of the second argument of ast/2."}
  (doseq [kind astnode/ekeko-keywords-for-ast-classes]
    (test/tuples-are
      (damp.ekeko/ekeko [?ast]
             (ast/ast kind ?ast))
      (damp.ekeko/ekeko [?ast]
             (ast/ast kind ?ast) 
             (ast/ast kind ?ast)))
    ))
  

(deftest
  ^{:doc "Tests whether has/3 is relational."}
  reification-has-relational
  (doseq [[node property child shouldbenode shouldbeproperty]  
          (damp.ekeko/ekeko [?node ?property ?child ?shouldbenode ?shouldbeproperty] 
                  (l/fresh [?kind]
                         (ast/ast ?kind ?node) 
                         (ast/has ?property ?node ?child) 
                         (ast/has ?shouldbeproperty ?shouldbenode ?child)))]
    (is (identical? node shouldbenode)
    (is (identical? property shouldbeproperty)))))


(deftest
  ^{:doc "Tests whether ast/2 quantifies over each node encountered during 
          a recursive descent of all compilation units."}
   reification-ast-child+
  (test/tuples-are 
    (damp.ekeko/ekeko [?ast]
                      (l/fresh [?kind]
                               (ast/ast ?kind ?ast)))
    (damp.ekeko/ekeko [?ast]
                      (l/fresh [?cu]
                               (ast/ast :CompilationUnit ?cu)
                               (l/conde 
                                 [(clojure.core.logic/== ?cu ?ast)]
                                 [(ast/child+ ?cu ?ast)])))))

(deftest
  reification-value
  ^{:doc "Tests relational nature of value/1, 
          value|list/1, value|null/1, value|primitive/1."}
  (doseq [valpred [ast/value ast/value|list
                   ast/value|null ast/value|primitive]]
    (test/tuples-are
      (damp.ekeko/ekeko [?val]
                        (valpred ?val))
      (damp.ekeko/ekeko [?val]
                        (valpred ?val) 
                        (valpred ?val)))))
  


(deftest 
  reification-child-parent 
  ^{:doc "Tests relation between child/3 and parent/2."}
  (doseq 
    [[node child parent]
     (damp.ekeko/ekeko [?node ?child ?parent] 
                       (l/fresh [?kind ?property]
                              (ast/ast ?kind ?node)
                              (ast/child ?property ?node ?child)
                              (ast/ast-parent ?child ?parent)))]
    (is (identical? node parent))))


(deftest
  reification-ast-specialkinds
  ^{:doc "Tests relational nature of ast/resolvable/2"}
  (doseq [valpred [ 
                   ast/ast|declaration
                   ast/ast|fieldaccess
                   ast/ast|type
                   ast/ast|declaration|resolveable
                   ast/ast|invocation
                   ast/ast|parameter|name
                   ]]
    (test/tuples-are
      (damp.ekeko/ekeko [?val]
                        (l/fresh [?kind]
                               (valpred ?kind ?val)))
      (damp.ekeko/ekeko [?val]
                        (l/fresh [?kind]
                               (valpred ?kind ?val) 
                               (valpred ?kind ?val))))))

(deftest
  reification-ast-subnodekinds
  ^{:doc "Tests whether ast/2 supports AST nodes of non-leaf kinds (non-exhaustive)."}
  (doseq [[subkind kind]
          [[:SingleVariableDeclaration :VariableDeclaration]
           [:VariableDeclarationFragment :VariableDeclaration]
           [:ReturnStatement :Statement]
           [:SimpleType :Type]]]
    (test/tuples-aresubset
      (damp.ekeko/ekeko [?ast] (ast/ast subkind ?ast))
      (damp.ekeko/ekeko [?ast] (ast/ast kind ?ast)))))
  



;; IBinding tests


(deftest 
  reification-ast-bindings
  ^{:doc "Simply tests whether binding-related ternary predicates execute exception-free."}
  (doseq [bindingpred [
                       astbindings/ast|type-binding|type
                       astbindings/ast|fieldaccess-binding|variable
                       astbindings/ast|invocation-binding|method
                       astbindings/ast|declaration-binding
                       ]]
    (doseq [[binding] (damp.ekeko/ekeko [?binding]
                                      (l/fresh [?kind ?ast]
                                             (bindingpred ?kind ?ast ?binding)))]
      (is (instance? org.eclipse.jdt.core.dom.IBinding binding)))))



;; Java Model Tests

(deftest
  reification-modelunaries
  ^{:doc "Tests that unary Java model reification predicates do not throw 
          an exception (testing whether they are relational takes a long time)."}
  (doseq [modelpred [structure/packagefragment|root
                     structure/packagefragment|root|binary
                     structure/packagefragment|root|source
                     structure/packagefragment
                     structure/classfile
                     structure/compilationunit
                     structure/type
                     structure/type|enum
                     structure/type|binary
                     structure/type|source
                     structure/method
                     structure/method|source
                     structure/method|binary
                     structure/field
                     structure/field|source
                     structure/field|binary]]
    (damp.ekeko/ekeko [?t] (modelpred ?t))))


(deftest 
  reification-modelelement
  ^{:doc "Tests relational nature of element/1."}
  (test/tuples-are 
    (damp.ekeko/ekeko [?k ?e] 
                      (structure/element ?k ?e))
    (damp.ekeko/ekeko [?k ?e] 
                      (structure/element ?k ?e)
                      (structure/element ?k ?e))))


(deftest
  structure-subtype
  ^{:doc "Tests type-type|sub+ relation."}
  (test/tuples-correspond 
    (damp.ekeko/ekeko [?subtypename]
           (l/fresh [?type ?subtype]
                  (structure/type-name|qualified|string ?type "test.damp.ekeko.cases.compositevisitor.Component")
                  (structure/type-type|sub+ ?type ?subtype)
                  (structure/type-name|simple|string ?subtype ?subtypename)))
    "#{(\"MustAliasLeaf\") (\"PrototypicalLeaf\") (\"SuperLogLeaf\") (\"Composite\") (\"MayAliasLeaf\") (\"EmptyLeaf\") (\"OnlyLoggingLeaf\")}"
    ))


  
(deftest
  convenience-type-identifier
  ^{:doc "Tests typedeclaration-identifier/2 relation."}
  (test/tuples-correspond 
    (damp.ekeko/ekeko [?t ?m ?o] 
           (convenience/typedeclaration-identifier ?t "OnlyLoggingLeaf"))
    "#{(\"public class OnlyLoggingLeaf extends Component {\\n  public void acceptVisitor(  ComponentVisitor v){\\n    System.out.println(\\\"Only logging.\\\");\\n  }\\n}\\n\" \"_0\" \"_1\")}"))


(deftest
  convenience-type-bodydeclaration-identifier
  ^{:doc "Tests typedeclaration-identifier-bodydeclaration-identifier/4 relation."}
  (test/tuples-correspond 
    (damp.ekeko/ekeko [?t ?m]
                      (convenience/typedeclaration-identifier-bodydeclaration-identifier ?t "OnlyLoggingLeaf" ?m "acceptVisitor" ))
    "#{(\"public class OnlyLoggingLeaf extends Component {\\n  public void acceptVisitor(  ComponentVisitor v){\\n    System.out.println(\\\"Only logging.\\\");\\n  }\\n}\\n\" \"public void acceptVisitor(ComponentVisitor v){\\n  System.out.println(\\\"Only logging.\\\");\\n}\\n\")}"
    ))



;; Test suite


(deftest
  test-suite
  (let [visitorproject "TestCase-JDT-CompositeVisitor"]
    
    
    ;Reification
    ;-----------
    
    ;AST    
    (test/against-project-named visitorproject false reification-ast)
    (test/against-project-named visitorproject false reification-has-relational)
    (test/against-project-named visitorproject false reification-ast-child+)
    (test/against-project-named visitorproject false reification-value)
    (test/against-project-named visitorproject false reification-child-parent)
    (test/against-project-named visitorproject false reification-ast-specialkinds)
    (test/against-project-named visitorproject false reification-ast-subnodekinds)
    
    ;IBinding
    (test/against-project-named visitorproject false reification-ast-bindings)
    
    
    ;Model
    (test/against-project-named visitorproject false reification-modelunaries)
    (test/against-project-named visitorproject false structure-subtype)
    
    
    
    ;http://dev.clojure.org/jira/browse/LOGIC-111
    ;(test/against-project-named visitorproject false reification-modelelement)
    
    
    ;Convenience
    (test/against-project-named visitorproject false convenience-type-identifier)
    (test/against-project-named visitorproject false convenience-type-bodydeclaration-identifier)
    
    
    
    ;silly eclipse!!!
    ;how can this be??
    ;(let [o 
    ;     (ffirst (ekeko [?m]
    ;                    (fresh [?t]
    ;                           (typedeclaration-identifier-bodydeclaration-identifier ?t "OnlyLoggingLeaf" ?m "acceptVisitor"))))
    ;     ob (.resolveBinding o)
    ;     m
    ;     (ffirst (ekeko [?m]
    ;                    (fresh [?t]
    ;                           (typedeclaration-identifier-bodydeclaration-identifier ?t "Component" ?m "acceptVisitor"))))
    ;     mb (.resolveBinding m)
    ;     ]
    ; (.overrides ob mb))
    
    
    )
    
   
    
    )
  
(defn 
  test-ns-hook 
  [] 
  (test/with-ekeko-disabled test-suite))


(comment  
  
  (run-tests)
  
  )


  
