(ns katamorph.action.binding-registry
  (:require [katamorph.action.binding-registry-shape :as shape]
            [katamorph.action.registry :as actions]))

(defn reference-errors [action-registry provider-registry registry]
  (->> registry
       vals
       (mapcat
        (fn [value]
          (cond-> []
            (nil? (actions/resolve-action action-registry (:binding/action value)))
            (conj {:law/id :binding/unknown-action
                   :binding/id (:binding/id value)
                   :action/id (:binding/action value)})
            (not (contains? provider-registry (:binding/provider value)))
            (conj {:law/id :binding/unknown-provider
                   :binding/id (:binding/id value)
                   :provider/id (:binding/provider value)}))))
       (sort-by (juxt #(str (:binding/id %)) #(str (:law/id %))))
       vec))

(defn bind [action-registry provider-registry registry]
  (let [checked (shape/validate registry)]
    (if-not (:ok checked)
      {:ok false :reason :invalid-binding-registry :errors (:errors checked)}
      (let [errors (reference-errors action-registry provider-registry registry)]
        {:ok (empty? errors) :registry registry :errors errors}))))

(defn providers-for [action-registry provider-registry registry action-id]
  (let [bound (bind action-registry provider-registry registry)]
    (if-not (:ok bound)
      bound
      {:ok true
       :bindings (->> registry vals
                      (filter #(= action-id (:binding/action %)))
                      (filter #(not= false (:binding/enabled? %)))
                      (sort-by #(str (:binding/id %)))
                      vec)})))
