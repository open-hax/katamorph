(ns katamorph.schema.condition-test
  #?(:clj (:require [clojure.test :refer [deftest is]]
                    [katamorph.condition :as condition]
                    [katamorph.schema.condition :as schema])
     :cljs (:require [cljs.test :refer [deftest is]]
                     [katamorph.condition :as condition]
                     [katamorph.schema.condition :as schema])))

(defn- eq-on [value]
  {:condition/op :eq
   :condition/path [:n]
   :condition/value value})

(defn- exists-at [segment]
  {:condition/op :exists
   :condition/path [segment]})

(deftest doubles-and-safe-integers-are-portable
  (is (schema/portable-number? 0))
  (is (schema/portable-number? -42))
  (is (schema/portable-number? 3.14))
  (is (schema/portable-number? schema/max-safe-integer))
  (is (schema/portable-number? (- schema/max-safe-integer))))

(deftest portable-integer-boundary-is-exact
  (is (schema/portable-integer? 0))
  (is (schema/portable-integer? schema/max-safe-integer))
  (is (schema/portable-integer? (- schema/max-safe-integer)))
  (is (not (schema/portable-integer? (inc schema/max-safe-integer))))
  (is (not (schema/portable-integer? (dec (- schema/max-safe-integer)))))
  (is (not (schema/portable-integer? 1.0))
      "integer-valued doubles remain numbers, not path-index integers"))

(deftest numeric-path-segments-use-the-portable-integer-law
  (is (condition/condition? (exists-at schema/max-safe-integer)))
  (is (condition/condition? (exists-at (- schema/max-safe-integer))))
  (is (not (condition/condition? (exists-at (inc schema/max-safe-integer)))))
  (is (not (condition/condition? (exists-at 1.0)))))

(deftest keyword-and-string-path-segments-remain-lawful
  (is (condition/condition? {:condition/op :exists
                             :condition/path [:artifact "status"]})))

(deftest non-numbers-are-not-numbers
  (is (not (schema/portable-number? "42")))
  (is (not (schema/portable-number? :42)))
  (is (not (schema/portable-number? nil))))

(deftest portable-numbers-still-compose
  (is (condition/condition? (eq-on 42)))
  (is (condition/condition? {:condition/op :in
                             :condition/path [:n]
                             :condition/values [1 2.5 -3]}))
  (is (condition/match? {:n 2.5} (eq-on 2.5))))

#?(:clj
   (deftest jvm-only-numerics-are-not-portable
     (is (not (schema/portable-number? 1/3))
         "a ratio has no ClojureScript reading at all")
     (is (not (schema/portable-number? 1.5M))
         "BigDecimal precision does not survive the trip through a double")
     (is (not (schema/portable-number? 12345678901234567890N)))
     (is (not (schema/portable-number? (float 0.1)))
         "a float widened to double no longer equals the double it prints as")
     (is (not (schema/portable-number? (inc schema/max-safe-integer)))
         "past 2^53 JavaScript stops counting by ones")))

#?(:clj
   (deftest conditions-carrying-unportable-numbers-fail-closed
     (is (not (condition/condition? (eq-on 1/3))))
     (is (not (condition/condition? (eq-on (inc schema/max-safe-integer)))))
     (is (not (condition/condition? {:condition/op :in
                                     :condition/path [:n]
                                     :condition/values [1 1/3]}))
         "one unportable member is enough to make the set unportable")
     (is (false? (condition/match? {:n 1/3} (eq-on 1/3)))
         "matching locally would be the divergence, not the fix")))
