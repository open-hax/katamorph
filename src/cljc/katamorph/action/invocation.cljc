(ns katamorph.action.invocation
  (:require [katamorph.action.input :as input]
            [katamorph.schema.action :as action]
            [katamorph.schema.condition :as condition]
            [malli.core :as m]))

(def ArgumentMap
  [:map-of [:or keyword? string?] condition/PortableValue])

(def InvocationRequest
  "Portable request to apply one semantic action.

   :operation/with is portable configuration. :operation/in is typed dataflow:
   reserved reference vectors must satisfy Katamorph's input-reference language;
   other portable values remain literals. Provider choice, handlers, runtime
   identity, timestamps, and effects do not belong here."
  [:map {:closed true}
   [:operation/id action/ContractId]
   [:operation/with {:optional true} ArgumentMap]
   [:operation/in {:optional true} input/InputMap]])

(defn valid? [value]
  (m/validate InvocationRequest value))
