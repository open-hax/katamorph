(ns katamorph.action.binding-registry-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [katamorph.action.binding-registry :as registry]
                    [katamorph.action.binding-registry-shape :as shape])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [katamorph.action.binding-registry :as registry]
                     [katamorph.action.binding-registry-shape :as shape])))

(def actions
  {:render/html {:contract/kind :action
                 :contract/id :render/html
                 :action/category :representation}
   :translate {:contract/kind :action
               :contract/id :translate
               :action/category :transduction}})

(def providers
  {:provider/local {:provider/id :provider/local}
   :provider/cloud {:provider/id :provider/cloud}})

(def local-render
  {:binding/id :render/local
   :binding/action :render/html
   :binding/provider :provider/local})

(def cloud-render
  {:binding/id :render/cloud
   :binding/action :render/html
   :binding/provider :provider/cloud})

(deftest binding-registry-composition-is-fail-closed
  (is (= {} (:registry (shape/compose))))
  (is (:ok (shape/compose {:render/local local-render}
                          {:render/cloud cloud-render})))
  (let [result (shape/compose {:render/local local-render}
                              {:render/local local-render})]
    (is (false? (:ok result)))
    (is (= :binding-id-conflict (:reason result)))))

(deftest binding-registry-keys-must-match-binding-ids
  (let [result (shape/validate {:wrong local-render})]
    (is (false? (:ok result)))
    (is (= :binding/registry-id-mismatch
           (-> result :errors first :law/id)))))

(deftest binding-registries-must-be-maps
  (doseq [value [nil [] 42]]
    (let [result (shape/validate value)]
      (is (false? (:ok result)))
      (is (= :binding/invalid-registry
             (-> result :errors first :law/id)))))
  (is (= :invalid-binding-registry
         (:reason (shape/compose nil))))
  (is (= :invalid-binding-registry
         (:reason (shape/compose 42))))
  (is (= :invalid-binding-registry
         (:reason (registry/bind actions providers nil)))))

(deftest references-must-resolve-before-use
  (testing "unknown action"
    (let [result (registry/bind actions providers
                                {:bad (assoc local-render
                                             :binding/id :bad
                                             :binding/action :missing/action)})]
      (is (false? (:ok result)))
      (is (= :binding/unknown-action
             (-> result :errors first :law/id)))))
  (testing "unknown provider"
    (let [result (registry/bind actions providers
                                {:bad (assoc local-render
                                             :binding/id :bad
                                             :binding/provider :provider/missing)})]
      (is (false? (:ok result)))
      (is (= :binding/unknown-provider
             (-> result :errors first :law/id))))))

(deftest provider-candidates-are-deterministic-and-do-not-select
  (let [bindings {:render/local local-render
                  :render/cloud cloud-render
                  :render/disabled (assoc cloud-render
                                          :binding/id :render/disabled
                                          :binding/enabled? false)}
        result (registry/providers-for actions providers bindings :render/html)]
    (is (:ok result))
    (is (= [:render/cloud :render/local]
           (mapv :binding/id (:bindings result))))))

(deftest disabled-host-providers-are-not-candidates
  (let [provider-registry (assoc-in providers [:provider/cloud :enabled] false)
        bindings {:render/local local-render
                  :render/cloud cloud-render}
        result (registry/providers-for actions provider-registry bindings :render/html)]
    (is (:ok result))
    (is (= [:render/local]
           (mapv :binding/id (:bindings result))))))

(deftest requested-actions-must-resolve-before-candidate-selection
  (let [result (registry/providers-for actions providers {} :missing/action)]
    (is (false? (:ok result)))
    (is (= [{:law/id :binding/unknown-action
             :action/id :missing/action}]
           (:errors result)))))

(deftest candidate-order-is-total-across-contract-id-types
  (let [keyword-binding local-render
        string-binding (assoc cloud-render :binding/id ":render/local")
        keyword-first (array-map :render/local keyword-binding
                                 ":render/local" string-binding)
        string-first (array-map ":render/local" string-binding
                                :render/local keyword-binding)
        ids (fn [bindings]
              (->> (registry/providers-for actions providers bindings :render/html)
                   :bindings
                   (mapv :binding/id)))]
    (is (= [:render/local ":render/local"]
           (ids keyword-first)))
    (is (= [:render/local ":render/local"]
           (ids string-first)))))
