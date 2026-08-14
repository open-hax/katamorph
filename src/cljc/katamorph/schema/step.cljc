(ns katamorph.schema.step
  (:require [katamorph.action.input :as input]
            [katamorph.schema.action :as action]))

;; Backward-compatible public alias. The generalized input language now owns
;; reference forms because step outputs are only one possible value source.
(def StepOutputRef input/StepOutputRef)

;; Step input names remain action port keywords even though direct invocation
;; maps retain string-key compatibility at their outer wire boundary.
(def StepInputs
  [:map-of keyword? input/InputValue])

(def ActionStep
  [:map {:closed false}
   [:step/id {:optional true} action/ContractId]
   [:step/action [:or keyword? string?]]
   [:step/with {:optional true} [:map {:closed false}]]
   [:step/in {:optional true} StepInputs]])
