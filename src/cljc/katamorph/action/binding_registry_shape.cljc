(ns katamorph.action.binding-registry-shape
  (:require [katamorph.action.binding-schema :as binding]))

(defn errors [registry]
  (if-not (map? registry)
    [{:law/id :binding/invalid-registry}]
    (->> registry
         (mapcat
          (fn [[id value]]
            (cond-> []
              (not= id (:binding/id value))
              (conj {:law/id :binding/registry-id-mismatch
                     :registry/id id
                     :binding/id (:binding/id value)})
              (not (binding/valid? value))
              (conj {:law/id :binding/invalid-shape
                     :registry/id id}))))
         (sort-by #(str (:registry/id %)))
         vec)))

(defn validate [registry]
  (let [problems (errors registry)]
    {:ok (empty? problems) :registry registry :errors problems}))

(defn conflicts [registries]
  (->> registries (mapcat keys) frequencies
       (keep (fn [[id n]] (when (> n 1) id)))
       (sort-by str) vec))

(defn compose [& registries]
  (let [errors (vec (mapcat #(-> % validate :errors) registries))]
    (if (seq errors)
      {:ok false :reason :invalid-binding-registry :errors errors}
      (let [duplicates (conflicts registries)]
        (if (seq duplicates)
          {:ok false :reason :binding-id-conflict :conflicts duplicates}
          {:ok true :registry (apply merge {} registries) :conflicts []})))))
