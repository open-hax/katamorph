(ns katamorph.schema.step
  (:require [katamorph.schema.action :as action]))

;; The producer is a step id, which may be a string or a keyword like any other
;; contract id. The output is a key in that producer's :action/provides, and
;; PortMap keys are keywords — so a string output can never resolve and the
;; reference is rejected here rather than surviving to wire validation.
(def StepOutputRef
  [:tuple [:= :step] action/ContractId keyword?])

(def StepInputs
  [:map-of keyword? StepOutputRef])

(def ActionStep
  [:map {:closed false}
   [:step/id {:optional true} action/ContractId]
   [:step/action [:or keyword? string?]]
   [:step/with {:optional true} [:map {:closed false}]]
   [:step/in {:optional true} StepInputs]])
