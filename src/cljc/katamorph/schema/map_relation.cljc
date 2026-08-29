(ns katamorph.schema.map-relation
  (:require [katamorph.schema.form :as form]))

(defn- entry [value]
  (let [[k second-value & more] value]
    (if (map? second-value)
      [k {:optional? (true? (:optional second-value))
          :schema (first more)}]
      [k {:optional? false :schema second-value}])))

(defn- entry-form? [value]
  (and (vector? value)
       (case (count value)
         2 (not (map? (second value)))
         3 (map? (second value))
         false)))

(defn- entries [schema]
  (let [children (vec (form/children schema))]
    (when (and (every? entry-form? children)
               (= (count children)
                  (count (set (map first children)))))
      (into {} (map entry children)))))

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
    (and (some? provided-entries)
         (some? required-entries)
         fields-ok?
         closed-ok?)))
