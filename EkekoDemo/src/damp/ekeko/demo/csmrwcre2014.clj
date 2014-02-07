(ns damp.ekeko.demo.csmrwcre2014
  (:require [clojure.core.logic :as logic])
  (:refer-clojure :exclude [== type])
  (:use clojure.core.logic)
  (:use damp.ekeko)
  (:use damp.ekeko.visualization)
  (:use damp.ekeko.visualization.view)
  (:use [damp.ekeko logic])
  (:use [damp.ekeko.soot soot])
  (:use [damp.ekeko.jdt ast astnode structure aststructure soot convenience markers rewrites])
  (:import [org.eclipse.ui.IMarkerResolution])
  (:import [damp.ekeko.EkekoProblemFixer])
  (:import [org.eclipse.jdt.ui ISharedImages JavaUI]))
           

;;The archive containing the source code can be found here: http://soft.vub.ac.be/~resteven/csmrwcrecase.zip
;;Ensure it has the Ekeko Nature by rightclicking on the project, configure, include in Ekeko Queries

;;Start a REPL by selecting Ekeko->start nREPL from the menu
;;Connect to that REPL by selecting Window->Connect to REPL from the menu

;;Open this file in the  Clojure editor (by double clicking the file in the Eclipse package explorer)
;;Rightclick within the editor and select: Clojure->Load File in REPL
;;In the same menu select Switch REPL to File's namespace

;;You can run the code by just writing (demo)
;;This will mark all the incorrect nodes with an annotation marker
;;You can solve a marker by calling (marker-quick-fix (first @markers/markers))
;;Or by clicking on the warning popup and use the quick fix


(defn 
  typedeclaration|inhierarchy
  [?type-decl]
  (fresh [?astnode]
    (typedeclaration-name|qualified|string ?astnode "be.ac.chaq.model.ast.java.ASTNode")
    (typedeclaration-typedeclaration|super ?type-decl ?astnode)))

(defn 
  annotation|namedentityproperty
  [?annotation]
  (all
    (annotation-name|qualified|string ?annotation "be.ac.chaq.model.entity.EntityProperty")))

(defn 
  annotation|ep-typeliteral
  [?annotation ?type-literal]
  (fresh [?member-pair ?name]
    (annotation|namedentityproperty ?annotation)
    (child :values ?annotation ?member-pair)
    (has :name ?member-pair ?name)
    (name|simple-string ?name "value")
    (has :value ?member-pair ?type-literal)))
  

(defn 
  type-annotation|correct 
  [?asttype ?annotation]
  (fresh [?targ ?type-name ?annotypelit  ?annotype]
         (ast :ParameterizedType ?asttype)
         (child :typeArguments ?asttype ?targ)
         (annotation|ep-typeliteral ?annotation ?annotypelit)
         (typeliteral-type ?annotypelit ?annotype)
         (fresh [?typekind]
                (ast|type-type ?typekind ?targ ?annotype))))

(defn 
  field-declaration|incorrect
  [?field-declaration]
  (fresh [?type-declaration ?annotation ?field-type]
    (annotation|namedentityproperty ?annotation)
    (fielddeclaration-annotation ?field-declaration ?annotation)
    (ast-typedeclaration|encompassing ?field-declaration ?type-declaration)
    (typedeclaration|inhierarchy ?type-declaration)
    (has :type ?field-declaration ?field-type)
    (fails
      (type-annotation|correct ?field-type ?annotation))))



;;Install Marker
(defn 
  add-annotation-problem-marker 
  [astnode]
  (add-problem-marker astnode "Type parameter corresponding to Cha-Q annotation is missing" "annotation"))

;;Rewrite Magic

(defn
  fielddeclaration-getinfo
  [typedeclaration]
  (first
    (ekeko [?annotation ?type]
           (fresh [?typeliteral]
                  (child :modifiers typedeclaration ?annotation)
                  (annotation|namedentityproperty ?annotation)
                  (annotation|ep-typeliteral ?annotation ?typeliteral)
                  (has :type ?typeliteral ?type)))))
   
;should be moved to rewriting namespace
(defn 
  node-property
  [node keyword]
  (node-property-value
    node
    (node-property-descriptor-for-ekeko-keyword node keyword)))


;;Marker Fixing 

(defn marker-quick-fix [marker]
  (let [fielddecl (ekekomarker-astnode marker)
        type (node-property fielddecl :type)
        [anno anno-type] (fielddeclaration-getinfo fielddecl)
        ast (.getAST type)
        type-copy (copy-astnode type)
        anno-type-copy (copy-astnode anno-type)
        new-node (create-parameterized-type type-copy)]
    (change-property-node fielddecl :type new-node)
    (add-node-cu (.getRoot fielddecl) new-node :typeArguments anno-type-copy 0)
    (apply-and-reset-rewrites)
    (reset-and-delete-marker marker)))



(defn annotation-fixer []
  (create-quick-fix "Add type parameter from Cha-Q annotation" marker-quick-fix))

(defn install-fixer []
  (damp.ekeko.EkekoProblemFixer/installNewResolution "annotation" (annotation-fixer)))


;;Marker Demo
(defn 
  demo-marker
  []
  (map (comp add-annotation-problem-marker first) 
       (ekeko [?ast] (field-declaration|incorrect ?ast))))


;; Visualization Demo
(defn
  demo-visualization
  []
  (let [labelprovider (damp.ekeko.gui.EkekoLabelProvider.)]
    (ekeko-visualize
      ;nodes
      (ekeko [?typedeclaration] 
             (fresh [?type ?typeroot]
                    (type-name|qualified|string ?typeroot "be.ac.chaq.model.ast.java.Expression")
                    (type-type|super ?type ?typeroot)
                    (typedeclaration-type ?typedeclaration ?type)))
      ;edges
      (ekeko [?fromuser ?totype]
             (fresh [?anno ?annotypelit ?annotype]
                    (annotation|ep-typeliteral ?anno ?annotypelit)
                    (ast-typedeclaration|encompassing ?anno ?fromuser)
                    (typeliteral-type ?annotypelit ?annotype)
                    (typedeclaration-type ?totype ?annotype)))
      :node|label
      (fn [typedeclaration] 
        (.getText labelprovider  typedeclaration))
      :node|image 
      (fn [typedeclaration] 
        (.getImage labelprovider typedeclaration))
      :edge|style 
      (fn [src dest] edge|directed)
      :layout
      layout|horizontaltree)))
  


(install-fixer)



(comment

  ;; Actual demo script
  ;; ------------------

  
  ;;launches a logic query of which the solutions are all AST nodes of type :Annotation
  (ekeko [?node]
         (ast :Annotation ?node))
  
  ;;logic queries can be embedded in functional expressions
  (let [type :Annotation]
    (count 
      (ekeko [?node] (ast type ?node))))
  
  ;;solutions are tuples of variable bindings (1-tuple of binding for ?ast) that satisfy the constraints 
  (map 
    (comp class first) ;function composition, takes class of first element of 1-tuple
    (ekeko [?node] (ast :Annotation ?node)))
  ;;note that each ?ast binding is an instance of org.eclipse.jdt.core.dom.NormalAnnotation

  ;;open inspector on solutions 
  (ekeko* [?node ?parent] 
          (ast :Annotation ?node)
          (ast-parent ?node ?parent)) 
  
  ;;let's restrict solutions to annotations on non-fielddeclarations
  (ekeko* [?node ?parent] 
          (ast :Annotation ?node)
          (ast-parent ?node ?parent)
          (ast :FieldDeclaration ?parent)) 
    
  ;;and to annotations of the correct type
  (ekeko* [?annotation] 
          (fresh [?annotype] ;introduces fresh variable in scope, won't show up in solutions
                 (type-name|qualified|string ?annotype "be.ac.chaq.model.entity.EntityProperty") 
                 (ast|annotation-type ?annotation ?annotype)))

  ;;first use of predicate that sits above AST level
  ;;for other relations provided by the Ekeko library:
  ;;see documentation at http://cderoove.github.com/damp.ekeko/

  ;;show documentation pop-up
  
  ;;also provides functions for quick visualizations
  ;;nodes and edges correspond to query solutions
  (let [labelprovider (damp.ekeko.gui.EkekoLabelProvider.)]
    (ekeko-visualize
      ;nodes
      (ekeko [?typedeclaration] 
             (fresh [?type ?typeroot]
                    (type-name|qualified|string ?typeroot "be.ac.chaq.model.ast.java.Expression")
                    (type-type|super ?type ?typeroot)
                    (typedeclaration-type ?typedeclaration ?type)))
      ;edges
      (ekeko [?fromuser ?totype]
             (fresh [?anno ?annotypelit ?annotype]
                    (annotation|ep-typeliteral ?anno ?annotypelit)
                    (ast-typedeclaration|encompassing ?anno ?fromuser)
                    (typeliteral-type ?annotypelit ?annotype)
                    (typedeclaration-type ?totype ?annotype)))
      :node|label
      (fn [typedeclaration] 
        (.getText labelprovider  typedeclaration))
      :node|image 
      (fn [typedeclaration] 
        (.getImage labelprovider typedeclaration))
      :edge|style 
      (fn [src dest] edge|directed)
      :layout
      layout|horizontaltree))
  
   ;;find field declarations of which the type and the annotation do not correspond

   
   ;;functions for marking solutions in query
   (def
     marked-fields 
     (map (comp add-annotation-problem-marker first) 
          (ekeko [?ast] (field-declaration|incorrect ?ast))))
   
   ;;open editor on ReturnStatement
   ;;execute quick fix
   ;;described in paper

   (for [marked marked-fields] 
     (reset-and-delete-marker marked))
   

  ;;relies on functions for program manipulation
  ;;can be invoked directly
  ;;following fixes all at once
  (for [[field fieldtype annotype]
        ;;subjects of change
        (ekeko [?field ?fieldtype ?annotype] 
               (fresh [?anno ?annotypeliteral]
                      (field-declaration|incorrect ?field)
                      (has :type ?field ?fieldtype)
                      (fielddeclaration-annotation ?field ?anno)
                      (annotation|ep-typeliteral ?anno ?annotypeliteral)
                      (has :type ?annotypeliteral ?annotype)))]
    ;;actual changes
    (let [fieldtype-copy (copy-astnode fieldtype)
          annotype-copy (copy-astnode annotype)
          new-type (create-parameterized-type fieldtype-copy)]
      (change-property-node field :type new-type)
      (add-node-cu (.getRoot field) new-type :typeArguments annotype-copy 0)))
 
  ;;have scheduled changes
  ;;stil need to apply them
  (apply-and-reset-rewrites)
 
  
  ;;performance questions?
  (ekeko* [?c ?t] (classfile-type ?c ?t))
  

)


  
  
  
  
  
  
  
  
  
  
   