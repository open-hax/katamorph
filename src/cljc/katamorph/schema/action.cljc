(ns katamorph.schema.action)

(def ContractId
  [:or string? keyword?])

(def PortMap
  [:map-of keyword? :any])

;; The port vocabulary is defined once here and spliced into every map schema
;; that admits an action, so the strict ActionSemantics below and the registered
;; :action kind cannot drift into two contracts describing the same keys.

(def PortKeys
  "Typed port entries an action may declare, category left optional."
  [[:action/category {:optional true} keyword?]
   [:action/requires {:optional true} PortMap]
   [:action/provides {:optional true} PortMap]
   [:action/traits   {:optional true} [:set keyword?]]])

(defn declares-ports?
  "True when an action opts into the typed port vocabulary."
  [action]
  (and (map? action)
       (boolean (some #(contains? action %)
                      [:action/requires :action/provides :action/traits]))))

(defn category-declared-with-ports?
  "Ports describe what an action consumes and produces within a category. An
   action that declares ports without one is not classifiable, so the pair is
   required together. Actions predating the vocabulary declare neither."
  [action]
  (or (not (declares-ports? action))
      (contains? action :action/category)))

(def PortedActionLaw
  [:fn {:error/message (str "an action declaring :action/requires, :action/provides, "
                            "or :action/traits must also declare :action/category")}
   category-declared-with-ports?])

(def ActionSemantics
  "Strictly typed action: category is mandatory rather than conditional."
  (into [:map {:closed false}
         [:contract/kind [:= :action]]
         [:contract/id ContractId]
         [:action/category keyword?]]
        (remove (fn [[k]] (= :action/category k)) PortKeys)))
