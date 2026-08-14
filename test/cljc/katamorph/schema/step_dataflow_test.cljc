(ns katamorph.schema.step-dataflow-test
  #?(:clj
     (:require [clojure.test :refer [deftest is testing]]
               [katamorph.schema.step :as step]
               [katamorph.schema.validation :as validation])
     :cljs
     (:require [cljs.test :refer [deftest is testing]]
               [katamorph.schema.step :as step]
               [katamorph.schema.validation :as validation])))

(deftest step-inputs-reference-upstream-outputs
  (let [value {:step/id :review
               :step/action :review/sme
               :step/with {:rubric :translation/sme-v1}
               :step/in {:candidate [:step :translate :candidate]}}]
    (is (:ok (validation/validate-schema step/ActionStep value)))))

(deftest step-inputs-share-the-portable-input-value-language
  (doseq [source [[:event :subject]
                  [:workflow :input :document]
                  [:trigger :payload :repository]
                  [:resource :documents/foo]
                  [:literal {:mode :strict}]
                  [:domain :ordinary-literal]]]
    (is (:ok
         (validation/validate-schema
          step/ActionStep
          {:step/action :review/sme
           :step/in {:candidate source}}))
        (pr-str source))))

(deftest malformed-reserved-references-still-fail-at-shape-validation
  (doseq [source [[:step :translate]
                  [:step :translate "candidate"]
                  [:event]
                  [:workflow :output :document]
                  [:trigger :event]
                  [:resource]
                  [:literal]]]
    (is (false?
         (:ok
          (validation/validate-schema
           step/ActionStep
           {:step/action :review/sme
            :step/in {:candidate source}})))
        (pr-str source))))

(deftest string-output-ports-are-rejected
  ;; :action/provides is a PortMap, whose keys are keywords. A string output
  ;; could never resolve, so a step-output reference rejects it immediately.
  (is (false?
       (:ok
        (validation/validate-schema
         step/ActionStep
         {:step/action :review/sme
          :step/in {:candidate [:step :translate "candidate"]}}))))
  ;; the producer id keeps the full contract-id vocabulary
  (is (:ok
       (validation/validate-schema
        step/ActionStep
        {:step/action :review/sme
         :step/in {:candidate [:step "translate" :candidate]}}))))

(deftest step-input-port-names-remain-keywords
  (is (false?
       (:ok
        (validation/validate-schema
         step/ActionStep
         {:step/action :review/sme
          :step/in {"candidate" [:step :translate :candidate]}})))))

(deftest config-and-dataflow-remain-distinct
  (let [value {:step/action :translate
               :step/with {:target-locale :fr}
               :step/in {:source [:step :load :document]}}]
    (is (= :fr (get-in value [:step/with :target-locale])))
    (is (= [:step :load :document]
           (get-in value [:step/in :source])))))
