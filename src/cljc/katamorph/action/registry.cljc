(ns katamorph.action.registry
  (:require [katamorph.schema.action :as action]
            [malli.core :as m]))

(def runtime-binding-keys
  #{:action/fn :action/handler :action/implementation})

(defn- runtime-bindings [contract]
  (->> runtime-binding-keys
       (filter #(contains? contract %))
       (sort-by str)
       vec))

(defn entry-errors [registry]
  (->> registry
       (mapcat
        (fn [[registry-id contract]]
          (let [bindings (runtime-bindings contract)]
            (cond-> []
              (not= registry-id (:contract/id contract))
              (conj {:law/id :action/registry-id-mismatch
                     :registry/id registry-id
                     :contract/id (:contract/id contract)})

              (not (m/validate action/ActionSemantics contract))
              (conj {:law/id :action/invalid-contract
                     :registry/id registry-id})

              (seq bindings)
              (conj {:law/id :action/runtime-binding-in-semantic-registry
                     :registry/id registry-id
                     :keys bindings})))))
       (sort-by #(str (:registry/id %)))
       vec))

(defn validate [registry]
  (let [errors (entry-errors registry)]
    {:ok (empty? errors) :registry registry :errors errors}))

(defn conflicts [registries]
  (->> registries
       (mapcat keys)
       frequencies
       (keep (fn [[action-id n]] (when (> n 1) action-id)))
       (sort-by str)
       vec))

(defn compose [& registries]
  (let [errors (vec (mapcat #(-> % validate :errors) registries))
        duplicate-ids (conflicts registries)]
    (cond
      (seq errors) {:ok false :reason :invalid-action-registry :errors errors}
      (seq duplicate-ids) {:ok false :reason :action-id-conflict :conflicts duplicate-ids}
      :else {:ok true :registry (apply merge {} registries) :conflicts []})))

(defn resolve-action [registry action-id]
  (get registry action-id))
