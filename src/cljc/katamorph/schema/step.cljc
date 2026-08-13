(ns katamorph.schema.step
  (:require [katamorph.schema.action :as action]))

(def StepOutputRef
  [:tuple [:= :step] action/ContractId action/ContractId])

(def StepInputs
  [:map-of keyword? StepOutputRef])

(def ActionStep
  [:map {:closed false}
   [:step/id {:optional true} action/ContractId]
   [:step/action [:or keyword? string?]]
   [:step/with {:optional true} [:map {:closed false}]]
   [:step/in {:optional true} StepInputs]])
