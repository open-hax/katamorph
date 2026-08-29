(ns katamorph.schema.registry
  (:require [katamorph.schema.kind :as kind]))

(defn- normalized-entries [registry]
  (map (fn [[schema-id schema]]
         [(kind/normalize-kind schema-id) schema])
       registry))

(defn schema-for
  "Resolve kind in registry, returning nil when it is not declared.

   Registries built by compose-registries carry canonical keyword ids, so the
   direct lookup answers. A registry handed in raw may still key on strings —
   a single contributor has no reason to compose — and a miss rescans by
   normalized id rather than calling the kind undeclared."
  [registry schema-kind]
  (let [normalized (kind/normalize-kind schema-kind)]
    (if (contains? registry normalized)
      (get registry normalized)
      (some (fn [[schema-id schema]]
              (when (= normalized (kind/normalize-kind schema-id))
                schema))
            registry))))

(defn registry-conflicts
  "Return normalized schema ids declared by more than one registry."
  [registries]
  (->> registries
       (mapcat normalized-entries)
       (map first)
       frequencies
       (keep (fn [[schema-id n]]
               (when (> n 1) schema-id)))
       (sort-by str)
       vec))

(defn compose-registries
  "Compose registries without replacing an existing normalized schema id."
  [& registries]
  (let [conflicts (registry-conflicts registries)]
    (if (seq conflicts)
      {:ok false :registry nil :conflicts conflicts}
      {:ok true
       :registry (into {} (mapcat normalized-entries registries))
       :conflicts []})))
