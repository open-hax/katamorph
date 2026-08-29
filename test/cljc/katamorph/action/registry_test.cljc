(ns katamorph.action.registry-test
  #?(:clj (:require [clojure.test :refer [deftest is]]
                    [katamorph.action.registry :as registry])
     :cljs (:require [cljs.test :refer [deftest is]]
                     [katamorph.action.registry :as registry])))

(def read-action
  {:contract/kind :action
   :contract/id :repository/read
   :action/category :repository
   :action/provides {:artifact :artifact/document}})

(def translate-action
  {:contract/kind :action
   :contract/id :translation/transduce
   :action/category :transduction
   :action/requires {:source :artifact/document}
   :action/provides {:candidate :artifact/document}
   :action/traits #{:generative :lossy}})

(deftest empty-registry-is-composition-identity
  (is (= {:ok true :registry {} :conflicts []}
         (registry/compose))))

(deftest independent-action-registries-compose
  (let [result (registry/compose {:repository/read read-action}
                                 {:translation/transduce translate-action})]
    (is (:ok result))
    (is (= read-action
           (registry/resolve-action (:registry result) :repository/read)))))

(deftest duplicate-action-ids-fail-closed
  (let [result (registry/compose {:repository/read read-action}
                                 {:repository/read read-action})]
    (is (= :action-id-conflict (:reason result)))
    (is (= [:repository/read] (:conflicts result)))))

(deftest registry-key-must-equal-contract-id
  (let [result (registry/compose {:wrong read-action})]
    (is (= :invalid-action-registry (:reason result)))
    (is (= :action/registry-id-mismatch
           (-> result :errors first :law/id)))))

(deftest action-contracts-must-satisfy-portable-semantics
  (let [result (registry/compose
                {:bad {:contract/kind :action
                       :contract/id :bad}})]
    (is (= :invalid-action-registry (:reason result)))
    (is (= :action/invalid-contract
           (-> result :errors first :law/id)))))

(deftest runtime-bindings-are-not-semantic-action-data
  (let [result (registry/compose
                {:repository/read
                 (assoc read-action :action/handler "runtime.ns/read!")})]
    (is (= :invalid-action-registry (:reason result)))
    (is (= :action/runtime-binding-in-semantic-registry
           (-> result :errors first :law/id)))
    (is (= [:action/handler]
           (-> result :errors first :keys)))))
