(ns katamorph.action.input-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [katamorph.action.input :as input]
                    [katamorph.action.invocation :as invocation]
                    [malli.core :as m])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [katamorph.action.input :as input]
                     [katamorph.action.invocation :as invocation]
                     [malli.core :as m])))

(deftest supported-reference-forms-are-portable-inputs
  (doseq [value [[:step :load :document]
                 [:event :subject]
                 [:event :data :actor]
                 [:workflow :input :document]
                 [:trigger :payload]
                 [:trigger :payload :repository :id]
                 [:resource :documents/foo]
                 [:literal {:arbitrary [:portable :data]}]]]
    (is (m/validate input/InputValue value) (pr-str value))))

(deftest malformed-reserved-tuples-fail-instead-of-becoming-literals
  (doseq [value [[:step :load]
                 [:step :load "document"]
                 [:event]
                 [:workflow :output :document]
                 [:workflow :input "document"]
                 [:trigger :event]
                 [:resource]
                 [:literal]]]
    (is (false? (m/validate input/InputValue value)) (pr-str value))))

(deftest reserved-looking-data-has-an-explicit-literal-escape-hatch
  (is (m/validate input/InputValue
                  [:literal [:step :not-a-reference]]))
  (is (m/validate input/InputValue
                  [:literal [:event]])))

(deftest unreserved-portable-vectors-remain-ordinary-literals
  (is (m/validate input/InputValue [:domain :value 3])))

(deftest invocation-inputs-use-the-reference-language
  (is (invocation/valid?
       {:operation/id :evaluation/open-case
        :operation/in {:subject [:event :subject]
                       :document [:workflow :input :document]
                       :policy [:literal {:mode :strict}]}}))
  (is (false?
       (invocation/valid?
        {:operation/id :evaluation/open-case
         :operation/in {:subject [:event]}}))))

(deftest operation-configuration-does-not-interpret-reference-tuples
  (testing "reserved-looking vectors in :operation/with are just portable configuration"
    (is (invocation/valid?
         {:operation/id :evaluation/open-case
          :operation/with {:example [:event]}}))))
