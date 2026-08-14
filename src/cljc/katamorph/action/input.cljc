(ns katamorph.action.input
  (:require [katamorph.schema.action :as action]
            [katamorph.schema.condition :as condition]))

(def reserved-heads
  #{:step :event :workflow :trigger :resource :literal})

(defn reserved-vector? [value]
  (and (vector? value)
       (contains? reserved-heads (first value))))

(def StepOutputRef
  [:tuple [:= :step] action/ContractId keyword?])

(def EventPathRef
  [:and
   [:vector {:min 2} condition/PathSegment]
   [:fn {:error/message "an event reference begins with :event"}
    #(= :event (first %))]])

(def WorkflowInputRef
  [:tuple [:= :workflow] [:= :input] keyword?])

(def TriggerPayloadRef
  [:and
   [:vector {:min 2} condition/PathSegment]
   [:fn {:error/message "a trigger reference begins with [:trigger :payload]"}
    #(and (= :trigger (first %))
          (= :payload (second %)))]])

(def ResourceRef
  [:tuple [:= :resource] [:or action/ContractId uuid?]])

(def LiteralRef
  [:tuple [:= :literal] condition/PortableValue])

(def InputReference
  [:or StepOutputRef
   EventPathRef
   WorkflowInputRef
   TriggerPayloadRef
   ResourceRef
   LiteralRef])

(def PortableLiteral
  [:and
   condition/PortableValue
   [:fn {:error/message "reserved input-reference vectors must satisfy their reference shape or be wrapped in [:literal ...]"}
    #(not (reserved-vector? %))]])

(def InputValue
  [:or InputReference PortableLiteral])

(def InputMap
  [:map-of [:or keyword? string?] InputValue])
