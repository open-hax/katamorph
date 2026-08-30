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

(deftest diagnostic-order-is-total-across-port-key-types
  (let [action (assoc translate :action/requires {})
        request {:operation/id :translation/transduce
                 :operation/in (array-map ":source" :string
                                          :source :keyword)}]
    (is (= [{:law/id :action/application-undeclared-input
             :input :source}
            {:law/id :action/application-undeclared-input
             :input ":source"}]
           (:errors (application/validate action request))))))

#?(:clj
   (deftest diagnostic-order-distinguishes-keyword-components
     (let [shallow (keyword "a" "b/c")
           nested (keyword "a/b" "c")
           action (assoc translate :action/requires {})
           request {:operation/id :translation/transduce
                    :operation/in (array-map shallow :shallow
                                             nested :nested)}]
       ;; These are distinct JVM keywords that both print as :a/b/c.
       ;; ClojureScript interns both constructions as the same keyword, so the
       ;; collision fixture only exists on the host named by the finding.
       (is (= [shallow nested]
              (mapv :input (:errors (application/validate action request))))))))

(deftest diagnostic-order-distinguishes-keyword-namespace-presence
  (let [unqualified (keyword "x")
        empty-namespace (keyword "" "x")
        action (assoc translate :action/requires {})
        request {:operation/id :translation/transduce
                 :operation/in (array-map unqualified :unqualified
                                          empty-namespace :empty)}]
    (is (= [empty-namespace unqualified]
           (mapv :input (:errors (application/validate action request)))))))

(deftest configuration-is-not-confused-with-dataflow
  (let [request (assoc valid-request :operation/with
                       {:source :configuration-value
                        :target-locale :fr})
        result (application/validate translate request)]
    (is (:ok result))
    (is (= translate (:action result)))
    (is (= request (:invocation result)))))

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
