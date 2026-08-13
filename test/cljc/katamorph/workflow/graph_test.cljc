(ns katamorph.workflow.graph-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.workflow.graph :as graph])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.workflow.graph :as graph])))

(deftest acyclic-dataflow-releases-all-steps
  (is (= []
         (graph/blocked-by-cycle
          [{:step/id :load :step/in {}}
           {:step/id :use :step/in {:source [:step :load :out]}}]))))

(deftest self-and-mutual-cycles-remain-blocked
  (is (= [:self]
         (graph/blocked-by-cycle
          [{:step/id :self :step/in {:source [:step :self :out]}}])))
  (is (= [:a :b]
         (graph/blocked-by-cycle
          [{:step/id :a :step/in {:source [:step :b :out]}}
           {:step/id :b :step/in {:source [:step :a :out]}}]))))

(deftest only-step-references-carry-dependencies
  (is (= {:a #{}}
         (graph/dependencies
          [{:step/id :a :step/in {:source [:literal :a :value]}}]))
      "a step id in the second slot of a non-step reference is not an edge")
  (is (= []
         (graph/blocked-by-cycle
          [{:step/id :a :step/in {:source [:literal :a :value]}}]))
      "the reference is unsupported, which is wire's finding, not a cycle")
  (is (= {:a #{}}
         (graph/dependencies
          [{:step/id :a :step/in {:source :a}}]))
      "a reference that is not a vector names no step at all"))
