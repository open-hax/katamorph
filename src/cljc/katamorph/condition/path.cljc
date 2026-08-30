(ns katamorph.condition.path
  (:require [katamorph.schema.condition :as schema]))

(defn valid-path? [path]
  (and (vector? path)
       (every? #(or (keyword? %) (string? %) (schema/portable-integer? %)) path)))

(defn- canonical-integer [value]
  #?(:clj (long value) :cljs value))

(defn- map-entry-at [m segment]
  (if-not (schema/portable-integer? segment)
    (find m segment)
    (let [canonical-segment (canonical-integer segment)
          match (reduce-kv
                 (fn [found key value]
                   (if (and (schema/portable-integer? key)
                            (= canonical-segment (canonical-integer key)))
                     (if found
                       (reduced ::ambiguous)
                       [key value])
                     found))
                 nil
                 m)]
      (when-not (= ::ambiguous match)
        match))))

(defn value-at [context path]
  (if-not (valid-path? path)
    {:found? false}
    (loop [current context segments (seq path)]
      (if-not segments
        {:found? true :value current}
        (let [segment (first segments)]
          (cond
            (map? current)
            (if-let [entry (map-entry-at current segment)]
              (recur (second entry) (next segments))
              {:found? false})

            (vector? current)
            (if (and (schema/portable-integer? segment)
                     (<= 0 segment)
                     (< segment (count current)))
              (recur (nth current (int segment)) (next segments))
              {:found? false})

            :else {:found? false}))))))
