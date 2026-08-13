(ns katamorph.schema.validation-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.schema.core :as core])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.schema.core :as core])))

(def schemas
  {:artifact
   [:map {:closed false}
    [:artifact/id [:or keyword? string?]]
    [:artifact/kind keyword?]]})

(deftest validates-open-extensible-artifacts
  (let [value {:artifact/id :doc/readme
               :artifact/kind :document
               :project/extension {:anything true}}
        result (core/validate schemas :artifact value)]
    (is (:ok result))
    (is (= :artifact (:kind result)))
    (is (= value (:value result)))))

(deftest invalid-values-return-structured-errors
  (let [result (core/validate schemas :artifact
                              {:artifact/id :doc/readme})]
    (is (false? (:ok result)))
    (is (seq (:errors result)))
    (is (every? vector? (map :path (:errors result))))))

(deftest diagnostics-have-stable-path-order
  (let [schema [:map
                [:z-field int?]
                [:a-field string?]]
        result (core/validate-schema schema {:z-field "bad"
                                             :a-field 42})]
    (is (= [[":a-field"] [":z-field"]]
           (mapv :path (:errors result))))))

(deftest unknown-kinds-fail-closed-as-data
  (let [result (core/validate schemas :unknown {})]
    (is (false? (:ok result)))
    (is (= :unknown (:kind result)))
    (is (= "unknown schema kind" (-> result :errors first :message)))
    (is (= [:artifact] (-> result :errors first :known)))))
