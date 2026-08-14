(ns katamorph.action.invocation
  (:require [katamorph.schema.action :as action]
            [katamorph.schema.condition :as condition]
            [malli.core :as m]))

(def ArgumentMap
  [:map-of [:or keyword? string?] condition/PortableValue])

(def InvocationRequest
  "Portable request to apply one semantic action.

   The :operation/* wire keys are retained for compatibility with existing
   callers. :operation/id resolves an ActionSemantics contract id; this value
   contains no provider choice, handler, runtime identity, timestamp, or effect."
  [:map {:closed true}
   [:operation/id action/ContractId]
   [:operation/with {:optional true} ArgumentMap]
   [:operation/in {:optional true} ArgumentMap]])

(defn valid? [value]
  (m/validate InvocationRequest value))
