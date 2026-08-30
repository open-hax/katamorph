(ns katamorph.schema.relation
  (:require [katamorph.schema.form :as form]
            [katamorph.schema.map-relation :as map-relation]
            [malli.core :as m]
            [malli.registry :as mr]))

(declare compatible?)

(def ^:private nominal-schema
  (m/schema :any))

(def ^:private nominal-registry
  (mr/lazy-registry
   m/default-registry
   (fn [schema-id _]
     (when (or (keyword? schema-id)
               (string? schema-id)
               (symbol? schema-id))
       nominal-schema))))

(defn- opaque-well-formed? [schema]
  (try
    (boolean (m/schema schema {:registry nominal-registry}))
    (catch #?(:clj Throwable :cljs :default) _
      false)))

(defn- well-formed? [schema]
  (let [schema-type (form/type-of schema)
        children (form/children schema)]
    (case schema-type
      :any (empty? children)
      :or (and (boolean (seq children))
               (every? well-formed? children))
      :and (and (boolean (seq children))
                (every? well-formed? children))
      := (= 1 (count children))
      :enum (boolean (seq children))
      :map (map-relation/well-formed? schema well-formed?)
      (opaque-well-formed? schema))))

(defn compatible?
  "Return true only when the supported schema forms prove provided <: required."
  [provided required]
  (let [provided-type (form/type-of provided)
        required-type (form/type-of required)]
    (if-not (and (well-formed? provided)
                 (well-formed? required))
      false
      (cond
        (and (= provided-type :map) (= required-type :map))
        (map-relation/compatible? provided required compatible?)

        (= provided required) true
        (= required-type :any) true

        (= provided-type :or)
        (every? #(compatible? % required) (form/children provided))

        (= required-type :or)
        (boolean (some #(compatible? provided %) (form/children required)))

        (= required-type :and)
        (every? #(compatible? provided %) (form/children required))

        (= provided-type :and)
        (boolean (some #(compatible? % required) (form/children provided)))

        (and (= provided-type :=) (= required-type :enum))
        (contains? (set (form/children required))
                   (first (form/children provided)))

        (and (= provided-type :enum) (= required-type :enum))
        (let [required-values (set (form/children required))]
          (every? #(contains? required-values %)
                  (form/children provided)))

        :else false))))
