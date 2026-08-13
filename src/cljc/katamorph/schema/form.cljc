(ns katamorph.schema.form)

(defn type-of [schema]
  (if (vector? schema) (first schema) schema))

(defn props [schema]
  (when (and (vector? schema) (map? (second schema)))
    (second schema)))

(defn children [schema]
  (let [tail (if (vector? schema) (rest schema) [])]
    (if (map? (first tail)) (rest tail) tail)))
