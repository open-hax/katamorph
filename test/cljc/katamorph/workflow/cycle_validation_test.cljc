(ns katamorph.workflow.cycle-validation-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.workflow.validation :as validation])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.workflow.validation :as validation])))

(def actions
  {:loop {:contract/kind :action
          :contract/id :loop
          :action/category :transduction
          :action/requires {:source :artifact/text}
          :action/provides {:out :artifact/text}}})

(deftest cyclic-dataflow-is-not-a-valid-workflow
  (let [result (validation/validate-steps
                actions
                [{:step/id :self
                  :step/action :loop
                  :step/in {:source [:step :self :out]}}])]
    (is (false? (:ok result)))
    (is (= :workflow/cyclic-dataflow
           (-> result :errors first :law/id)))
    (is (= [:self]
           (-> result :errors first :step/ids)))))
