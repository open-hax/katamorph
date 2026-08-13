(ns katamorph.workflow.graph)

(defn dependencies [steps]
  (let [ids (set (keep :step/id steps))]
    (into {}
          (keep (fn [step]
                  (when-let [id (:step/id step)]
                    [id (->> (:step/in step)
                             vals
                             (keep second)
                             (filter ids)
                             set)])))
          steps)))

(defn blocked-by-cycle [steps]
  (loop [deps (dependencies steps)]
    (let [ready (set (keep (fn [[id needs]] (when (empty? needs) id)) deps))]
      (cond
        (empty? deps) []
        (empty? ready) (vec (sort-by str (keys deps)))
        :else (recur (into {}
                           (keep (fn [[id needs]]
                                   (when-not (ready id)
                                     [id (apply disj needs ready)])))
                           deps))))))
