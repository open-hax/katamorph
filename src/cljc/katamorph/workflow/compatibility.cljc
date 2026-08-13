(ns katamorph.workflow.compatibility)

(defn compatible-contract?
  "Return true when a provided port contract satisfies a required port contract.

   V1 is deliberately conservative: contracts must be equal. Keep this seam
   separate so structural/schema subsumption can replace equality later without
   changing workflow graph validation."
  [provided required]
  (= provided required))

(defn compatible-port?
  "Return true when producer/output can satisfy consumer/input."
  [producer output-name consumer input-name]
  (let [provides (:action/provides producer)
        requires (:action/requires consumer)]
    (and (contains? provides output-name)
         (contains? requires input-name)
         (compatible-contract? (get provides output-name)
                               (get requires input-name)))))
