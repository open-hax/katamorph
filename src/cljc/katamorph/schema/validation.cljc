(ns katamorph.schema.validation
  (:require [malli.core :as m]
            [malli.error :as me]
            [katamorph.schema.kind :as kind]
            [katamorph.schema.registry :as registry]))

(defn- collect-humanized-errors [prefix value]
  (cond
    (nil? value) []
    (string? value) [{:path prefix :message value}]
    (vector? value) (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value) (mapcat (fn [[k v]]
                           (collect-humanized-errors (conj prefix (str k)) v))
                         (sort-by (comp str key) value))
    :else [{:path prefix :message (pr-str value)}]))

(defn validation-errors
  "Return stable path/message diagnostics for value under schema."
  [schema value]
  (->> (m/explain schema value)
       me/humanize
       (collect-humanized-errors [])
       (mapv (fn [error]
               (update error :path #(mapv str %))))))

(defn validate-schema
  "Validate a value against one Malli schema."
  [schema value]
  (if (m/validate schema value)
    {:ok true :value value :errors []}
    {:ok false
     :value value
     :errors (validation-errors schema value)}))

(defn validate
  "Validate value against kind in registry without inventing a fallback kind."
  [schemas schema-kind value]
  (let [normalized (kind/normalize-kind schema-kind)]
    (if-let [schema (registry/schema-for schemas normalized)]
      (assoc (validate-schema schema value) :kind normalized)
      {:ok false
       :kind normalized
       :value value
       :errors [{:path []
                 :message "unknown schema kind"
                 :kind normalized
                 :known (vec (sort-by str (keys schemas)))}]})))

(defn valid?
  "Boolean validation for callers that do not need diagnostics."
  [schemas schema-kind value]
  (:ok (validate schemas schema-kind value)))
