(ns katamorph.workflow.validation
  (:require [katamorph.workflow.graph :as graph]
            [katamorph.workflow.index :as index]
            [katamorph.workflow.wire :as wire]))

(defn- missing-input-errors [action-registry step]
  (let [action (get action-registry (:step/action step))
        required (set (keys (:action/requires action)))
        wired (set (keys (:step/in step)))]
    (->> required
         (remove wired)
         (sort-by str)
         (mapv (fn [input-name]
                 {:law/id :workflow/missing-required-input
                  :step/id (:step/id step)
                  :input input-name})))))

(defn- step-errors [action-registry step-index step]
  (if-not (contains? action-registry (:step/action step))
    [{:law/id :workflow/unknown-consumer-action
      :step/id (:step/id step)
      :step/action (:step/action step)}]
    (into (missing-input-errors action-registry step)
          (keep (fn [[input-name reference]]
                  (wire/validate-wire action-registry
                                      step-index
                                      step
                                      input-name
                                      reference)))
          (:step/in step))))

(defn- cycle-errors [steps]
  (let [blocked (graph/blocked-by-cycle steps)]
    (if (seq blocked)
      [{:law/id :workflow/cyclic-dataflow
        :step/ids blocked}]
      [])))

(defn validate-steps
  "Validate typed value flow across action steps.

   This checks data dependencies only. :job/needs remains scheduling metadata."
  [action-registry steps]
  (let [indexed (index/index-steps steps)]
    (if-not (:ok indexed)
      indexed
      (let [step-index (:steps indexed)
            errors (vec (concat
                         (mapcat #(step-errors action-registry step-index %) steps)
                         (cycle-errors steps)))]
        {:ok (empty? errors)
         :steps step-index
         :errors errors}))))
