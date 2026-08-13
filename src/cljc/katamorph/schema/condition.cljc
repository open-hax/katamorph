(ns katamorph.schema.condition)

(def PathSegment
  [:or keyword? string? int?])

(def PortableValue
  "Recursive data that can cross runtime boundaries without carrying code or host objects."
  [:schema
   {:registry
    {::portable-value
     [:or nil?
      boolean?
      number?
      string?
      keyword?
      uuid?
      [:vector [:ref ::portable-value]]
      [:set [:ref ::portable-value]]
      [:map-of [:or keyword? string?] [:ref ::portable-value]]]}}
   [:ref ::portable-value]])

(def Condition
  [:schema
   {:registry
    {::condition
     [:multi {:dispatch :condition/op}
      [:eq
       [:map {:closed true}
        [:condition/op [:= :eq]]
        [:condition/path [:vector PathSegment]]
        [:condition/value PortableValue]]]
      [:not-eq
       [:map {:closed true}
        [:condition/op [:= :not-eq]]
        [:condition/path [:vector PathSegment]]
        [:condition/value PortableValue]]]
      [:exists
       [:map {:closed true}
        [:condition/op [:= :exists]]
        [:condition/path [:vector PathSegment]]]]
      [:in
       [:map {:closed true}
        [:condition/op [:= :in]]
        [:condition/path [:vector PathSegment]]
        [:condition/values [:vector PortableValue]]]]
      [:and
       [:map {:closed true}
        [:condition/op [:= :and]]
        [:condition/clauses [:vector {:min 1} [:ref ::condition]]]]]
      [:or
       [:map {:closed true}
        [:condition/op [:= :or]]
        [:condition/clauses [:vector {:min 1} [:ref ::condition]]]]]
      [:not
       [:map {:closed true}
        [:condition/op [:= :not]]
        [:condition/clause [:ref ::condition]]]]]]}}
   ::condition])
