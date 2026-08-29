(ns katamorph.workflow.wire
  (:require [katamorph.workflow.compatibility :as compatibility]))

(defn validate-wire
  "Validate one consumer input wired from one producer step output.
   Returns nil when valid, otherwise one structured finding."
  [action-registry step-index consumer input-name reference]
  (let [[ref-kind producer-id output-name] reference
        consumer-action (get action-registry (:step/action consumer))
        producer (get step-index producer-id)
        producer-action (when producer
                          (get action-registry (:step/action producer)))]
    (cond
      (nil? consumer-action)
      {:law/id :workflow/unknown-consumer-action
       :step/action (:step/action consumer)}

      (not (contains? (:action/requires consumer-action) input-name))
      {:law/id :workflow/undeclared-input
       :step/id (:step/id consumer)
       :input input-name}

      (not= :step ref-kind)
      {:law/id :workflow/unsupported-reference
       :step/id (:step/id consumer)
       :input input-name
       :reference reference}

      (nil? producer)
      {:law/id :workflow/unknown-producer-step
       :step/id (:step/id consumer)
       :input input-name
       :producer-step producer-id}

      (nil? producer-action)
      {:law/id :workflow/unknown-producer-action
       :producer-step producer-id
       :step/action (:step/action producer)}

      (not (contains? (:action/provides producer-action) output-name))
      {:law/id :workflow/undeclared-output
       :producer-step producer-id
       :output output-name}

      (not (compatibility/compatible-port? producer-action output-name
                                           consumer-action input-name))
      {:law/id :workflow/incompatible-port-contracts
       :step/id (:step/id consumer)
       :input input-name
       :producer-step producer-id
       :output output-name
       :required (get-in consumer-action [:action/requires input-name])
       :provided (get-in producer-action [:action/provides output-name])}

      :else nil)))
