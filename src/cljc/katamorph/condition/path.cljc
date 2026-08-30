(ns katamorph.condition.path
  (:require [katamorph.schema.condition :as schema]))

(defn valid-path? [path]
  (and (vector? path)
       (every? #(or (keyword? %) (string? %) (schema/portable-integer? %)) path)))

(defn value-at [context path]
  (if-not (valid-path? path)
    {:found? false}
    (loop [current context segments (seq path)]
      (if-not segments
        {:found? true :value current}
        (let [segment (first segments)]
          (cond
            (map? current)
            (if (contains? current segment)
              (recur (get current segment) (next segments))
              {:found? false})

            (vector? current)
            (if (and (schema/portable-integer? segment)
                     (<= 0 segment)
                     (< segment (count current)))
              (recur (nth current (int segment)) (next segments))
              {:found? false})

            :else {:found? false}))))))
