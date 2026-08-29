(ns katamorph.workflow.graph)

(defn- referenced-step
  "The upstream step id a reference names, or nil when it names something else.

   Only `[:step producer output]` declares a dependency. Any other reference is
   wire's to reject, and reading its second element as a step id invents an edge
   the workflow never declared — `[:literal :a :value]` inside step `:a` would
   report a cycle on top of the unsupported reference that is the real fault."
  [reference]
  (when (and (vector? reference)
             (= :step (first reference)))
    (second reference)))

(defn dependencies [steps]
  (let [ids (set (keep :step/id steps))]
    (into {}
          (keep (fn [step]
                  (when-let [id (:step/id step)]
                    [id (->> (:step/in step)
                             vals
                             (keep referenced-step)
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
