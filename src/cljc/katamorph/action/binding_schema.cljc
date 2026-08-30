(ns katamorph.action.binding-schema
  (:require [katamorph.schema.action :as action]
            [malli.core :as m]))

(def ActionBinding
  [:map {:closed true}
   [:binding/id action/ContractId]
   [:binding/action action/ContractId]
   [:binding/provider action/ContractId]
   [:binding/enabled? {:optional true} :boolean]])

(defn valid? [value]
  (m/validate ActionBinding value))
