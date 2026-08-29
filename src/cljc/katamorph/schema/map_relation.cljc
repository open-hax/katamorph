(ns katamorph.schema.map-relation
  (:require [katamorph.schema.form :as form]))

(defn- entry [value]
  (let [[k second-value & more] value]
    (if (map? second-value)
      [k {:optional? (true? (:optional second-value))
          :schema (first more)}]
      [k {:optional? false :schema second-value}])))

(defn- entries [schema]
  (into {} (map entry (form/children schema))))

(defn compatible?
  [provided required schema-compatible?]
  (let [provided-entries (entries provided)
        required-entries (entries required)
        fields-ok?
        (every?
         (fn [[k required-entry]]
           (if-let [provided-entry (get provided-entries k)]
             (and (or (:optional? required-entry)
                      (not (:optional? provided-entry)))
                  (schema-compatible? (:schema provided-entry)
                                      (:schema required-entry)))
             (and (:optional? required-entry)
                  (true? (:closed (form/props provided))))))
         required-entries)
        closed-ok?
        (or (not (true? (:closed (form/props required))))
            (and (true? (:closed (form/props provided)))
                 (every? #(contains? required-entries %)
                         (keys provided-entries))))]
    (and fields-ok? closed-ok?)))
