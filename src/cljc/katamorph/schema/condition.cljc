(ns katamorph.schema.condition)

(def max-safe-integer
  "2^53 - 1. Past this a JavaScript number stops representing consecutive
   integers, so a larger integer cannot cross to ClojureScript unchanged."
  9007199254740991)

(defn portable-integer?
  "True for integer values both CLJ and CLJS can preserve exactly."
  [x]
  #?(:clj (cond
            (integer? x)
            (<= (- max-safe-integer) x max-safe-integer)

            (instance? Double x)
            (and (Double/isFinite (double x))
                 (== x (Math/rint (double x)))
                 (<= (- max-safe-integer) x max-safe-integer))

            :else false)
     :cljs (and (number? x)
                (integer? x)
                (<= (- max-safe-integer) x max-safe-integer))))

(def PortableInteger
  [:fn {:error/message "a portable integer is within the JavaScript safe-integer range"}
   portable-integer?])

(def PathSegment
  [:or keyword? string? PortableInteger])

(defn portable-number?
  "True for numbers both runtimes hold identically.

   ClojureScript has one numeric type, the IEEE-754 double, so every value it
   can produce is already representable on the JVM. The JVM is the side that
   holds what ClojureScript cannot: ratios and BigDecimal, which have no
   reading there at all; floats, which do not compare equal to the double they
   widen into; and integers past the safe range, which round silently. Each of
   those changes :eq and :in results across the boundary this kernel promises
   is portable, so none of them is a portable value."
  [x]
  #?(:clj (cond
            (instance? Double x) true
            (portable-integer? x) true
            :else false)
     :cljs (number? x)))

(def PortableNumber
  [:fn {:error/message (str "a portable number is a double or an integer within "
                            "the JavaScript safe-integer range")}
   portable-number?])

(def PortableValue
  "Recursive data that can cross runtime boundaries without carrying code or host objects."
  [:schema
   {:registry
    {::portable-value
     [:or nil?
      boolean?
      PortableNumber
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
        [:condition/clause [:ref ::condition]]]]]}}
   ::condition])
