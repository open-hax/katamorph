(ns katamorph.workflow.index)

(defn index-steps
  "Index named steps and report duplicate ids as data.

   Anonymous steps remain valid workflow steps; they simply cannot be the
   target of a [:step <id> <output>] reference."
  [steps]
  (let [named (keep (fn [step]
                      (when-let [step-id (:step/id step)]
                        [step-id step]))
                    steps)
        duplicates (->> named
                        (map first)
                        frequencies
                        (keep (fn [[step-id n]]
                                (when (> n 1) step-id)))
                        (sort-by str)
                        vec)]
    (if (seq duplicates)
      {:ok false
       :steps nil
       :errors (mapv (fn [step-id]
                       {:law/id :workflow/duplicate-step-id
                        :step/id step-id})
                     duplicates)}
      {:ok true
       :steps (into {} named)
       :errors []})))
