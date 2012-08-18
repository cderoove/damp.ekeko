(ns damp.ekeko.jdt.grinder
  (:refer-clojure :exclude [== type])
  (:use clojure.core.logic)
  (:use [damp.ekeko logic ekekomodel])
  (:use [damp.ekeko.jdt astnode reification basic soot])
  (:use [damp qwal])
  (:import 
    [damp.ekeko JavaProjectModel]
    [org.eclipse.jface.text Document]
    [org.eclipse.text.edits TextEdit]
    [org.eclipse.jdt.core ICompilationUnit IJavaProject]
    [org.eclipse.jdt.core.dom BodyDeclaration Expression Statement ASTNode ASTParser AST CompilationUnit]
    [org.eclipse.jdt.core.dom.rewrite ASTRewrite]))


; AST modification data

(defn make-rewrite-for-cu 
  [cu]
  (ASTRewrite/create (.getAST cu)))

(def current-rewrites (atom {}))

(defn reset-rewrites! []
  (reset! current-rewrites {}))

(defn current-rewrite-for-cu [cu]
  (if-let [rw (get @current-rewrites cu)]
    rw
    (let [nrw (make-rewrite-for-cu cu)]
      (swap! current-rewrites assoc cu nrw)
      nrw))) 


;(defn apply-rewrite-to-node [rw node]
;  (let [cu (.getRoot ^ASTNode node)
;        icu (.getJavaElement ^CompilationUnit cu)
;        wc (.getWorkingCopy ^ICompilationUnit icu nil)
;        edit (.rewriteAST ^ASTRewrite rw)]
;    (do
;      (.applyTextEdit wc edit) ;No matching method found: applyTextEdit for class org.eclipse.jdt.internal.core.CompilationUnit>
;      (.commitWorkingCopy ^ICompilationUnit wc false nil)
;      (.discardWorkingCopy ^ICompilationUnit wc))))


(defn apply-rewrite-to-node [rw node]
  (JavaProjectModel/applyRewriteToNode rw node))

(defn apply-rewrites []
  (doseq [[cu rw] @current-rewrites]
    (apply-rewrite-to-node rw cu)))

(defn apply-and-reset-rewrites []
  (do
    (apply-rewrites)
    (reset-rewrites!)))


;; (Private) Logic interface

(defn cu-rewrite 
  [?cu ?rewrite]
  (all
    (ast :CompilationUnit ?cu)
    (equals ?rewrite (current-rewrite-for-cu ?cu))))
  
(defn ast-rewrite [?node ?rewrite]
  (fresh [?root]
         (ast-root ?node ?root)
         (cu-rewrite ?root ?rewrite)))




(defn ast-to-map [ast]
  (let [reifs (reifiers ast)
        keyw (ekeko-keyword-for-class-of ast)]
    (zipmap (cons :kind (keys reifs)) ;TODO: either ensure map is constructed lazily, or implement map unification as core.logic does
            (cons keyw (map (fn [f] (f)) (vals reifs)))))) ;TODO: this can't be idiomatic

;not linked to ast
(defn ast-map [?ast ?map]
  (fresh [?keyw] 
    (ast ?keyw ?ast)
    (equals ?map (ast-to-map ?ast))))            


(defn new-ast [?kind ?ast]
  (fresh [?factory ?class]
         (equals ?factory (AST/newAST AST/JLS4))
         (equals-without-exception ?class (class-for-ekeko-keyword ?kind))
         (equals ?ast (.createInstance ^AST ?factory ^Class ?class))))

                            
; (Public) Predicates for modifying an AST

(defn jdt-parse-string-as-kind-for-project 
  [^IJavaProject project kind s]
  (let [parser (ASTParser/newParser AST/JLS4)]                
    (.setProject parser project)
    (.setResolveBindings parser true)
    (.setKind parser kind)
    (.setSource parser (.toCharArray s))
    (.createAST parser nil)))

(defn ast-parser-kind [?node ?kind]
  (fresh [?keyword]
         (ast ?keyword ?node)
         (conda [(succeeds (instance? BodyDeclaration ?node)) (equals ?kind (ASTParser/K_CLASS_BODY_DECLARATIONS))]
                [(succeeds (instance? CompilationUnit ?node)) (equals ?kind (ASTParser/K_COMPILATION_UNIT))]
                [(succeeds (instance? Expression ?node)) (equals ?kind (ASTParser/K_EXPRESSION))]
                [(succeeds (instance? Statement ?node)) (equals ?kind (ASTParser/K_STATEMENTS))])))


(defn ast-becomes-in-rewrite-code [?node ?rewrite ?string]
  (fresh [?new ?kind ?project]
         (ast-project ?node ?project)
         (ast-parser-kind ?node ?kind)
         (equals ?new (jdt-parse-string-as-kind-for-project ?project ?kind ?string))
         (perform (.replace ^ASTRewrite ?rewrite ?node ?new nil))))

(defn ast-becomes-in-rewrite-ast [?node ?rewrite ?ast]
  (fresh [?new ?kind ?project]
         (perform (.replace ^ASTRewrite ?rewrite ?node ?ast nil))))

(defn ast-children-become-in-rewrite-mapseq
  [?ast ?rewrite ?mapseq]
  (conde [(== () ?mapseq)]
         [(fresh [?tail ?childkeyw ?property ?newchildorvalue]
                 (conso [?childkeyw ?newchildorvalue] ?tail ?mapseq)
                 (equals-without-exception ?property (node-property-descriptor-for-ekeko-keyword ?ast ?childkeyw))
                 (perform (.set ^ASTRewrite ?rewrite ?ast ?property ?newchildorvalue nil))
                 (ast-children-become-in-rewrite-mapseq ?ast ?rewrite ?tail))]))


(defn ast-becomes-in-rewrite-map 
  [?node ?rewrite ?map]
    (fresh [?nodekeyw ?mapkeyw ?mapkeywclass ?new ?newchildkeyw ?mapseq] 
           (ast ?nodekeyw ?node)
           (equals ?mapkeyw (get map :kind ::not-found))
           (equals ?mapseq (seq (dissoc ?map :kind)))
           (conda [(succeeds (or (= ?mapkeyw ::not-found) 
                                 (= ?mapkeyw ?nodekeyw)))
                   (== ?new ?node)]
                  [succeed 
                   (equals-without-exception ?mapkeywclass (class-for-ekeko-keyword ?mapkeyw))
                   (equals ?new (.createInstance ^AST (.getAST ?node) ^Class ?mapkeywclass))
                   (ast-becomes-in-rewrite-ast ?node ?rewrite ?new)])                   
           (ast-children-become-in-rewrite-mapseq ?new ?rewrite ?mapseq)))
         
  
  
(defn ast-becomes-in-rewrite [?node ?rewrite ?description]
  (conda [(v+ ?description)
          (conda [(succeeds (string? ?description))
                  (ast-becomes-in-rewrite-code ?node ?rewrite ?description)]
                 [(succeeds (map? ?description))
                  (ast-becomes-in-rewrite-map ?node ?rewrite ?description)]
                 [(succeeds (instance? ASTNode ?description))
                  (conda [(== ?description ?node)]
                         [(ast-becomes-in-rewrite-ast ?node ?rewrite ?description)])])]))

(defn ast-becomes [?node ?description]
  (fresh [?keyw ?rewrite]
         (ast ?keyw ?node)
         (ast-rewrite ?node ?rewrite)
         (ast-becomes-in-rewrite ?node ?rewrite ?description)))

;(defn has-now [?ast ?childkeyw ?newhas ?description]
;  (fresh [?property ?rewrite]
;         (ast-rewrite ?node ?rewrite)
;         (equals-without-exception ?property (node-property-descriptor-for-ekeko-keyword ?ast ?childkeyw))
;         (perform (.set ^ASTRewrite ?rewrite ?ast ?property ?newhas nil))))


;ast-becomes with string: easy: just parsing van een str, bekomen door apply str op ast nodes, bekomen door project te doen

;Rule applications
;Voor de eigenlijke regel-definities kibit checken
;https://github.com/jonase/kibit/blob/master/src/kibit/rules/util.clj



(comment

   (ekeko [?ast]
          (ast :SimpleName ?ast)
          (has :identifier ?ast "referencedField")
          (ast-becomes ?ast "foobarReferencedField"))
   (apply-and-reset-rewrites)

   (ekeko [?ast]
          (ast :SimpleName ?ast)
          (has :identifier ?ast "foobarReferencedField")
          (ast-becomes ?ast {:kind :SimpleName 
                             :identifier "referencedField"}))
   
   (ekeko [?ast]
          (ast :SimpleName ?ast)
          (has :identifier ?ast "foobarReferencedField")
          (ast-becomes ?ast {:identifier "referencedField"}))
   
      (apply-and-reset-rewrites)

     
   ;introduce explicit this
   (ekeko [?inv ?this ?m]
          (ast :MethodInvocation ?inv)
          (has :expression ?inv nil)
          
          (ast-containing-method ?inv ?m)
          (fails 
            (fresh [?mod]
                   (child :modifiers ?m ?mod)
                   (modifier-static ?mod)))
     
          (new-ast :ThisExpression ?this) ;new ast
          (ast-becomes ?inv {:expression ?this}))
   
    ;introduce explicit this
   (ekeko [?inv ?this ?m]
          (ast :MethodInvocation ?inv)
          (has :expression ?inv nil)
          
          (ast-containing-method ?inv ?m)
          (fails 
            (fresh [?mod]
                   (child :modifiers ?m ?mod)
                   (modifier-static ?mod)))
          
          (new-ast :ThisExpression ?this) ;new ast
          (ast-becomes ?inv {:expression ?this}))
    
     (ekeko [?inv ?this ?m]
          (ast :MethodInvocation ?inv)
          (has :expression ?inv nil)
          
          (ast-containing-method ?inv ?m)
          (fails 
            (fresh [?mod]
                   (child :modifiers ?m ?mod)
                   (modifier-static ?mod)))
          
          (new-ast :ThisExpression ?this) ;new ast
          (ast-becomes ?inv {:expression ?this}))
    
     
   
   
   
   
   



   
  )