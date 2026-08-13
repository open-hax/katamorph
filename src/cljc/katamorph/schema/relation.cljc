(ns katamorph.schema.relation
  (:require [katamorph.schema.form :as form]
            [katamorph.schema.map-relation :as map-relation]))

(declare compatible?)

(defn compatible?
  "Return true only when the supported schema forms prove provided <: required."
  [provided required]
  (let [provided-type (form/type-of provided)
        required-type (form/type-of required)]
    (cond
      (= provided required) true
      (= required :any) true

      (= required-type :or)
      (boolean (some #(compatible? provided %) (form/children required)))

      (= provided-type :or)
      (every? #(compatible? % required) (form/children provided))

      (= required-type :and)
      (every? #(compatible? provided %) (form/children required))

      (= provided-type :and)
      (boolean (some #(compatible? % required) (form/children provided)))

      (and (= provided-type :=) (= required-type :enum))
      (contains? (set (form/children required))
                 (first (form/children provided)))

      (and (= provided-type :enum) (= required-type :enum))
      (every? (set (form/children required))
              (set (form/children provided)))

      (and (= provided-type :map) (= required-type :map))
      (map-relation/compatible? provided required compatible?)

      :else false)))
