(ns katamorph.schema.kind)

(defn normalize-kind
  "Normalize the common string/keyword kind dialect without inventing a kind."
  [kind]
  (cond
    (keyword? kind) kind
    (string? kind) (keyword kind)
    :else kind))
