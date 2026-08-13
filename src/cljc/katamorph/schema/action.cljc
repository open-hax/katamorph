(ns katamorph.schema.action)

(def ContractId
  [:or string? keyword?])

(def PortMap
  [:map-of keyword? :any])

(def ActionSemantics
  [:map {:closed false}
   [:contract/kind [:= :action]]
   [:contract/id ContractId]
   [:action/category keyword?]
   [:action/requires {:optional true} PortMap]
   [:action/provides {:optional true} PortMap]
   [:action/traits {:optional true} [:set keyword?]]])
