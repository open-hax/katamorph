(ns katamorph.action.application-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [katamorph.action.application :as application])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [katamorph.action.application :as application])))

(def translate
  {:contract/kind :action
   :contract/id :translation/transduce
   :action/category :transduction
   :action/requires {:source :artifact/text
                     :terminology :artifact/terminology}
   :action/provides {:candidate :artifact/text}})

(def valid-request
  {:operation/id :translation/transduce
   :operation/with {:target-locale :fr}
   :operation/in {:source [:step :load :document]
                  :terminology [:step :load :terms]}})

(deftest invocation-must-name-the-action-it-applies
  (let [result (application/validate
                translate
                (assoc valid-request :operation/id :evaluation/review))]
    (is (false? (:ok result)))
    (is (= :action/application-id-mismatch
           (-> result :errors first :law/id)))))

(deftest every-required-input-must-be-named
  (let [result (application/validate
                translate
                (update valid-request :operation/in dissoc :terminology))]
    (is (false? (:ok result)))
    (is (= [{:law/id :action/application-missing-input
             :input :terminology}]
           (:errors result)))))

(deftest undeclared-input-ports-fail-closed
  (let [result (application/validate
                translate
                (assoc-in valid-request [:operation/in :mystery] :value))]
    (is (false? (:ok result)))
    (is (= [{:law/id :action/application-undeclared-input
             :input :mystery}]
           (:errors result)))))

(deftest input-port-names-are-not-silently-coerced
  (let [request {:operation/id :translation/transduce
                 :operation/in {"source" [:step :load :document]
                                :terminology [:step :load :terms]}}
        law-ids (set (map :law/id (:errors (application/validate translate request))))]
    (is (contains? law-ids :action/application-missing-input))
    (is (contains? law-ids :action/application-undeclared-input))))

(deftest configuration-is-not-confused-with-dataflow
  (let [result (application/validate
                translate
                (assoc valid-request :operation/with
                       {:source :configuration-value
                        :target-locale :fr}))]
    (is (:ok result))))

(deftest malformed-boundaries-fail-before-port-analysis
  (testing "invalid action"
    (let [result (application/validate
                  (dissoc translate :action/category)
                  valid-request)]
      (is (= [{:law/id :action/application-invalid-action}]
             (:errors result)))))
  (testing "invalid invocation"
    (let [result (application/validate
                  translate
                  (assoc valid-request :operation/provider :provider/local))]
      (is (= [{:law/id :action/application-invalid-invocation}]
             (:errors result))))))
