(ns katamorph.condition
  (:require [malli.core :as m]
            [katamorph.condition.path :as path]
            [katamorph.schema.condition :as schema]))

(declare eval-condition)

(defn- leaf-value [context condition]
  (path/value-at context (:condition/path condition)))

(defn- eval-condition [context condition]
  (case (:condition/op condition)
    :eq
    (let [{:keys [found? value]} (leaf-value context condition)]
      (and found? (= value (:condition/value condition))))

    :not-eq
    (let [{:keys [found? value]} (leaf-value context condition)]
      (and found? (not= value (:condition/value condition))))

    :exists
    (:found? (leaf-value context condition))

    :in
    (let [{:keys [found? value]} (leaf-value context condition)]
      (and found?
           (boolean (some #(= value %) (:condition/values condition)))))

    :and
    (every? #(eval-condition context %) (:condition/clauses condition))

    :or
    (boolean (some #(eval-condition context %) (:condition/clauses condition)))

    :not
    (not (eval-condition context (:condition/clause condition)))

    false))

(defn condition? [condition]
  (m/validate schema/Condition condition))

(defn match?
  "Evaluate a valid portable condition against Clojure data; malformed input fails closed."
  [context condition]
  (and (condition? condition)
       (boolean (eval-condition context condition))))
