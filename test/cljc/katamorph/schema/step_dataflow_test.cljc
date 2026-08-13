(ns katamorph.schema.step-dataflow-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.schema.step :as step]
               [katamorph.schema.validation :as validation])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.schema.step :as step]
               [katamorph.schema.validation :as validation])))

(deftest step-inputs-reference-upstream-outputs
  (let [value {:step/id :review
               :step/action :review/sme
               :step/with {:rubric :translation/sme-v1}
               :step/in {:candidate [:step :translate :candidate]}}]
    (is (:ok (validation/validate-schema step/ActionStep value)))))

(deftest arbitrary-vectors-are-not-step-references
  (is (false?
       (:ok
        (validation/validate-schema
         step/ActionStep
         {:step/action :review/sme
          :step/in {:candidate [:translate :candidate]}})))))

(deftest config-and-dataflow-remain-distinct
  (let [value {:step/action :translate
               :step/with {:target-locale :fr}
               :step/in {:source [:step :load :document]}}]
    (is (= :fr (get-in value [:step/with :target-locale])))
    (is (= [:step :load :document]
           (get-in value [:step/in :source])))))
