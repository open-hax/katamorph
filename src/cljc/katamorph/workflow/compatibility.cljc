(ns katamorph.workflow.compatibility
  (:require [katamorph.schema.relation :as relation]))

(defn compatible-contract? [provided required]
  (relation/compatible? provided required))

(defn compatible-port? [producer output-name consumer input-name]
  (let [provides (:action/provides producer)
        requires (:action/requires consumer)]
    (and (contains? provides output-name)
         (contains? requires input-name)
         (compatible-contract? (get provides output-name)
                               (get requires input-name)))))
