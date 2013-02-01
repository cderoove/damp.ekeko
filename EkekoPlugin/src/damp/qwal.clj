(ns
    ^{:doc "(Quantified) regular path expressions over graphlike structures"
      :author "Reinout Stevens"}
  damp.qwal
  (:refer-clojure :exclude [==])
  (:use [clojure.core.logic]))


(defn get-successors [graph node next]
  ((:successors graph) node next))

(defn get-predecessors [graph node pred]
  ((:predecessors graph) node pred))

;;;; QRPE
;; Atm I am not sure whether adding quantifiers actually adds anything.
;; The universal and existential quantifier are dual.
;; Saying that an expression has to hold on all the paths between A and B
;; is the same as finding a path between A and B where the exps does not hold.


(defn
  ^{:doc "succeeds when next is a direct successor of node" }
  trans [graph node next]
  (fresh [nodes]
         (project [node]
                  (get-successors graph node nodes)
                  (membero next nodes))))

(defn
  ^{:doc "succeeds when previous is a direct predecessor of node" }
  rev-trans [graph node previous]
  (fresh [nodes]
         (project [node]
                  (get-predecessors graph node nodes)
                  (membero previous nodes))))


(defn
  default-solve-goal [graph current next goal]
  (all
   (goal graph current next)))



(defn
  ^{:doc "solves goal in the current world.
Arguments to the goal are goal, current and next.
Goal should ground next."}
  solve-goal [graph current next goal]
  (let [goal-solver (:goal-solver graph)]
    (if goal-solver
      (goal-solver graph current next goal)
      (default-solve-goal graph current next goal))))



(defn
  ^{:doc "goals is a list of goals.
Each goal is called, passing the next version of the previous goal as the
current version of the current goal."}
  solve-goals [graph curr end goals]
  (conde [(emptyo goals)
          (== curr end)]
         [(fresh [h t next]
                 (conso h t goals)
                 (project [ curr h t ]
                          (solve-goal graph curr next h)
                          (solve-goals graph next end t)))]))

(defn q=>
  ^{:doc "fancier syntax for trans"}
  [graph current next]
  (all
   (trans graph current next)))


(defn q<=
  ^{:doc "reverse transition"}
  [graph current previous]
  (all
   (rev-trans graph current previous)))

  (def q*loop
    (tabled
     [graph current end goals]
     (conde
      [(fresh [next]
              (solve-goals graph current next goals)
              (q*loop graph next end goals))] ;;goals may succeed an arbitrary nr of times
      [(== current end)])))


(defn
  ^{:doc "goals may succeed zero to multiple times.
Should detect loops by using tabled/slg resolution.
q* is greedy, meaning it tries the longest path for which goals holds.
see q*? for the reluctant variant
BUG: currently greedy behaves just as the reluctant version,
as conde interleaves between its choices." }
  q* [& goals]
  (fn [graph current next]
    (q*loop graph current next goals)))


(def q*?loop
  (tabled
    [graph current end goals]
    (conde
      [(== current end)]
      [(fresh [next]
              (solve-goals graph current next goals)
              (q*?loop graph next end goals))])))

(defn
  ^{:doc "reluctant/non-greedy version of q*"}
  q*? [& goals]
  (fn [graph current next]
    (all
     (q*?loop graph current next goals))))


(defn
  ^{:doc "see q*, but also calls q=> at the end of goals"}
  q=>* [& goals]
  (apply q* (concat goals [q=>])))


(defn
  ^{:doc "see q*?, but also calls q=> at the end of goals"}
  q=>*? [& goals]
  (apply q*? (concat goals [q=>])))


(defn
  ^{:doc "see q*, but also calls q<= at the end of goals"}
  q<=* [& goals]
  (apply q* (concat goals [q<=])))

(defn
  ^{:doc "see q*?, but also calls q<= at the end of goals"}
  q<=? [& goals]
  (apply q*? (concat goals [q<=])))


(defn
  ^{:doc "same as q*, except goals should succeed at least once"}
  q+ [& goals]
  (fn [graph current end]
    (fresh [next]
           (solve-goals graph current next goals)
           ((apply q* goals) graph next end))))


(defn
  ^{:doc "same as q*?, except goals should succeed at least once"}
  q+? [& goals]
  (fn [graph current end]
    (fresh [next]
           (solve-goals graph current next goals)
           ((apply q*? goals) graph next end))))

(defn
  ^{:doc "see q+, but also calls q=> at the end of goals"}
  q=>+ [& goals]
  (apply q+ (concat goals [q=>])))

(defn
  ^{:doc "see q+?, but also calls q=> at the end of goals"}
  q=>+? [& goals]
  (apply q+? (concat goals [q=>])))


(defn
  ^{:doc "see q+, but also calls q<= at the end of goals"}
  q<=+ [& goals]
  (apply q+ (concat goals [q<=])))

(defn
  ^{:doc "see q+?, but also calls q<= at the end of goals"}
  q<=+? [& goals]
  (apply q+? (concat goals [q<=])))


(defn
  ^{:doc "goals may succeed or not"}
  q? [& goals]
  (fn [graph curr next]
    (conde [(solve-goals graph curr next goals)]
           [(== curr next)])))


;;one may argue about tabling this or not
(defn
  ^{:doc "goals has to succeed times times"}
  qtimes [times & goals]
  (defn times-bound-loop [graph curr next number]
    (fresh [neext]
           (conde [(== number times)
                   (== curr next)]
                  [(== true (< number times))
                   (solve-goals graph curr neext goals)
                   (times-bound-loop graph neext next (inc number))])))
  (defn times-unbound-loop [graph curr next number]
    (fresh [neext]
           (conde [(== number times)
                   (== curr next)]
                  [(solve-goals graph curr neext goals)
                   (times-unbound-loop graph neext next (inc number))])))
  (fn [graph curr next]
    (project [times]
             (if (lvar? times) ;;unbound
               (times-unbound-loop graph curr next 0)
               (times-bound-loop graph curr next 0)))))

(defn
  ^{:doc "see qtimes, but also calls q=> at the end of goals"}
  qtimes=> [times & goals]
  (apply qtimes times
         (concat goals [q=>])))

(defn
^{:doc "see qtimes, but also calls q=> at the end of goals"}
  qtimes<= [times & goals]
  (apply qtimes times
         (concat goals [q<=])))
  
              
    



(defn
  ^{:doc "implementing naf using conda"}
  qfail [& goals]
  (fn [graph current next]
    (conda
     [(solve-goals graph current next goals)
      fail]
     [(== current next)])))


(defmacro
  ^{:doc "reverse of qwhile.
Goals are executed until conditions hold in current."}
  quntil [current [ & conditions ] & goals]
  `(qwhile ~current [ (qfail (all ~@conditions)) ] ~@goals))



(defmacro
  ^{:doc "calls goals as long as conditions holds.
Current is bound to the current world and can thus be used in conditions.
Note that when & goals doesn't go to a successor zero results are found."}
  qwhile [current [& conditions] & goals]
  (let [graphvar (gensym "graph")
        nextvar (gensym "next")
        endvar (gensym "end")
        loopvar (gensym "qwhile-loop")
        realgoals (if (nil? goals) '() goals)]
    `(fn [~graphvar ~current ~endvar]
       (def ~loopvar
         (tabled [ ~graphvar ~current ~endvar ]
                 (project [~current]
                          (conda [~@conditions
                                  (fresh [~nextvar]
                                         ;;for reasons unknown this doesnt work when you just use ~realgoals
                                         (solve-goals ~graphvar ~current ~nextvar (list ~@realgoals))
                                         (~loopvar ~graphvar ~nextvar ~endvar))]
                                 [(== ~current ~endvar)]))))
       (~loopvar ~graphvar ~current ~endvar))))




(defn
  ^{:doc "main rule that solves a qrpe"}
  solve-qrpe [graph start end & goals ]
  (conde  [(fresh [h t next]
                  (!= nil goals)
                  (conso h t goals)
                  (project [start h t]
                           (solve-goal graph start next h)
                           (apply solve-qrpe  graph next end t)))]
          [(== nil goals) ;; (emptyo goals) doesnt work for reasons unknown to the author
           (== start end)]))



;;Macros for nicer syntax, because sugar is good for you
(defmacro
  ^{:doc "A macro on top of solve-qrpe that allows for nicer syntax.
Graph holds the graph, and should at least understand :nodes, :successors and :predecessors.
Start node must be a member of the graph.
End node is assumed to be a member of the graph.
Bindings are the new introduced variables that are kept throughout the pathexpression.
Exps are the actual goals that should hold on the path through the graph.
Each goal should be a rule that takes 2 variables.
First variable is the current world, and will be ground.
Second variable is the next world, and goal must ground this." }
  qwal [graph start end bindings & exps ]
  (let [genstart (gensym "start")
        genend (gensym "end")
        graphvar (gensym "graph")]
    `(let [~graphvar ~graph] ;;evaluate ~graph which either returns 
       (project [~graphvar]
                (fresh  ~bindings
                        (fresh [~genstart ~genend]
                               (== ~start ~genstart)
                               (== ~end ~genend)
                               (solve-qrpe
                                ~graphvar
                                ~genstart
                                ~genend
                                ~@exps)))))))


(defmacro
  ^{:doc "macro to evaluate a series of conditions in the same world"}
  qin-current [& conditions]
  (let [world (gensym "world")]
    `(qcurrent [~world]
               ~@conditions)))


(defmacro
  ^{:doc "macro that evaluates a series of conditions in the current world. current is bound to the current world"}
  qcurrent [[current] & conditions]
  (let [next (gensym "next")
        graph (gensym "graph")]
    `(fn [~graph ~current ~next]
       (project [~current]
                ~@conditions
                (== ~current ~next)))))

(defmacro
  ^{:doc "macro that evaluated a series of conditions in the current worls. current is unified with the current world, and wrapped inside a project"}
  qcurrento [[current] & conditions]
  `(fn [graph# curr# next#]
    (all
     (== ~current curr#)
     (project [~current]
              ~@conditions
              (== curr# next#)))))


(defn
  ^{:doc "Helper function that creates a goal that solves all goals passed as argument"}
  all-goals [& goals]
  (fn [graph current next]
    (solve-goals graph current next goals)))
      
    
(comment
  "example usage"

  "constructing example graph with a loop"
  (defn has-info [current info]
    (project [current]
             (all
              (== current info))))
  
  (defn
    ^{:doc "succeeds when to is the list of nodes that are direct successors of node" }
    to-node [node to]
    (conde [(== node :foo)
            (== to '(:bar))]
           [(== node :bar)
            (== to '(:baz))]
           [(== node :baz)
            (== to '(:quux :rein))]
           [(== node :quux)
            (== to '(:foo))]))

  (defn
    from-node [node from]
    (conde [(== node :foo)
            (== from '(:quux))]
           [(== node :bar)
            (== from '(:foo))]
           [(== node :baz)
            (== from '(:bar))]
           [(== node :rein)
            (== from '(:baz))]
           [(== node :quux)
            (== from '(:baz))]))
  
  (def graph
    (let [nodes (list :foo :bar :baz :quux :rein)]
      {:nodes nodes
       :successors to-node
       :predecessors from-node}))

  (run* [end]
        (qwal graph (first (:nodes graph)) end
              [info curro]
              (q=>*)
              (q=>* (qcurrent [curr] succeed))
              (q=>* (qcurrent [curr] (fresh [info] (has-info curr info))))
              (qcurrent [curr] (has-info curr :foo))
              q=>
              (qcurrent [curr] (has-info curr :bar))
              q=>
              (q? (qcurrent [curr] (has-info curr :foo)) q=>)
              (qcurrent [curr] (has-info curr :baz))
              q=> q=>
              (qcurrento [curro] (has-info curro info))))
  )
