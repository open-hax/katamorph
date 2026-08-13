(ns katamorph.schema.registry
  (:require [katamorph.schema.kind :as kind]))

(defn schema-for
  "Resolve kind in registry, returning nil when it is not declared."
  [registry schema-kind]
  (get registry (kind/normalize-kind schema-kind)))

(defn registry-conflicts
  "Return schema ids declared by more than one registry."
  [registries]
  (->> registries
       (mapcat keys)
       frequencies
       (keep (fn [[schema-id n]]
               (when (> n 1) schema-id)))
       (sort-by str)
       vec))

(defn compose-registries
  "Compose registries without replacing an existing schema id."
  [& registries]
  (let [conflicts (registry-conflicts registries)]
    (if (seq conflicts)
      {:ok false :registry nil :conflicts conflicts}
      {:ok true :registry (apply merge registries) :conflicts []})))
