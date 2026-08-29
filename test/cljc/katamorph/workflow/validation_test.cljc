(ns katamorph.workflow.validation-test
  #?(:clj
     (:require [clojure.test :refer [deftest is testing]]
               [katamorph.workflow.compatibility :as compatibility]
               [katamorph.workflow.validation :as validation])
     :cljs
     (:require [cljs.test :refer [deftest is testing]]
               [katamorph.workflow.compatibility :as compatibility]
               [katamorph.workflow.validation :as validation])))

(def actions
  {:load {:contract/kind :action
          :contract/id :load
          :action/category :repository
          :action/provides {:document :artifact/text}}
   :translate {:contract/kind :action
               :contract/id :translate
               :action/category :transduction
               :action/requires {:source :artifact/text}
               :action/provides {:candidate :artifact/text}}
   :evaluate {:contract/kind :action
              :contract/id :evaluate
              :action/category :evaluation
              :action/requires {:candidate :artifact/text}}
   :evaluate-audio {:contract/kind :action
                    :contract/id :evaluate-audio
                    :action/category :evaluation
                    :action/requires {:candidate :artifact/audio}}})

(def valid-steps
  [{:step/id :load
    :step/action :load}
   {:step/id :translate
    :step/action :translate
    :step/in {:source [:step :load :document]}}
   {:step/id :evaluate
    :step/action :evaluate
    :step/in {:candidate [:step :translate :candidate]}}])

(deftest exact-contract-compatibility-is-the-v1-law
  (is (compatibility/compatible-contract? :artifact/text :artifact/text))
  (is (false? (compatibility/compatible-contract?
               :artifact/text
               :artifact/audio))))

(deftest validates-a-typed-value-flow
  (is (:ok (validation/validate-steps actions valid-steps))))

(deftest required-inputs-must-be-wired
  (let [result (validation/validate-steps
                actions
                [{:step/id :translate :step/action :translate}])]
    (is (false? (:ok result)))
    (is (= :workflow/missing-required-input
           (-> result :errors first :law/id)))))

(deftest referenced-producers-and-outputs-must-exist
  (testing "producer step"
    (let [result (validation/validate-steps
                  actions
                  [{:step/id :translate
                    :step/action :translate
                    :step/in {:source [:step :missing :document]}}])]
      (is (= :workflow/unknown-producer-step
             (-> result :errors first :law/id)))))
  (testing "producer output"
    (let [result (validation/validate-steps
                  actions
                  [{:step/id :load :step/action :load}
                   {:step/id :translate
                    :step/action :translate
                    :step/in {:source [:step :load :missing-output]}}])]
      (is (= :workflow/undeclared-output
             (-> result :errors first :law/id))))))

(deftest incompatible-port-contracts-fail-closed
  (let [result (validation/validate-steps
                actions
                [{:step/id :load :step/action :load}
                 {:step/id :evaluate
                  :step/action :evaluate-audio
                  :step/in {:candidate [:step :load :document]}}])]
    (is (false? (:ok result)))
    (is (= :workflow/incompatible-port-contracts
           (-> result :errors first :law/id)))))

(deftest unsupported-references-do-not-also-report-a-cycle
  (let [result (validation/validate-steps
                actions
                [{:step/id :translate
                  :step/action :translate
                  :step/in {:source [:literal :translate :value]}}])
        law-ids (set (map :law/id (:errors result)))]
    (is (false? (:ok result)))
    (is (contains? law-ids :workflow/unsupported-reference))
    (is (not (contains? law-ids :workflow/cyclic-dataflow))
        "the step id sits in a reference kind the graph must not read as an edge")))

(deftest malformed-references-return-structured-findings
  (doseq [reference [:source
                     [:step :translate]
                     [:step :translate :document :extra]]]
    (let [result (validation/validate-steps
                  actions
                  [{:step/id :translate
                    :step/action :translate
                    :step/in {:source reference}}])
          law-ids (set (map :law/id (:errors result)))]
      (is (false? (:ok result)))
      (is (= :workflow/unsupported-reference
             (-> result :errors first :law/id)))
      (is (= reference
             (-> result :errors first :reference)))
      (is (not (contains? law-ids :workflow/cyclic-dataflow))
          "malformed step references must not add dependency edges"))))

(deftest duplicate-step-identities-are-ambiguous
  (let [result (validation/validate-steps
                actions
                [{:step/id :same :step/action :load}
                 {:step/id :same :step/action :load}])]
    (is (false? (:ok result)))
    (is (= :workflow/duplicate-step-id
           (-> result :errors first :law/id)))))
