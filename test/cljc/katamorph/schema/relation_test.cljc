(ns katamorph.schema.relation-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [katamorph.workflow.compatibility :as compatibility])
     :cljs (:require [cljs.test :refer [deftest is testing]]
                     [katamorph.workflow.compatibility :as compatibility])))

(deftest nominal-contracts-remain-nominal
  (is (compatibility/compatible-contract? :artifact/text :artifact/text))
  (is (false? (compatibility/compatible-contract?
               :artifact/text :artifact/audio))))

(deftest enums-and-unions-can-be-proven-narrower
  (is (compatibility/compatible-contract?
       [:= :ready]
       [:enum :ready :done]))
  (is (compatibility/compatible-contract?
       [:enum :ready :review]
       [:enum :ready :review :done]))
  (is (compatibility/compatible-contract?
       [:or [:= :a] [:= :b]]
       [:or [:enum :a :c] [:enum :b :d]])))

(deftest enum-subsets-support-falsey-members
  (is (compatibility/compatible-contract?
       [:enum false]
       [:enum false true]))
  (is (compatibility/compatible-contract?
       [:enum nil false]
       [:enum nil false true]))
  (is (false?
       (compatibility/compatible-contract?
        [:enum false]
        [:enum true]))))

(deftest map-providers-must-satisfy-required-fields
  (let [provided [:map {:closed true}
                  [:id :string]
                  [:status [:= :ready]]]
        required [:map
                  [:id :string]
                  [:status [:enum :ready :review]]]]
    (is (compatibility/compatible-contract? provided required)))

  (testing "required fields cannot become optional"
    (is (false?
         (compatibility/compatible-contract?
          [:map [:id {:optional true} :string]]
          [:map [:id :string]]))))

  (testing "open providers cannot omit consumer-constrained optional fields"
    (let [required [:map [:id {:optional true} :string]]]
      (is (false?
           (compatibility/compatible-contract?
            [:map]
            required)))
      (is (compatibility/compatible-contract?
           [:map {:closed true}]
           required))))

  (testing "a closed consumer cannot accept an open producer"
    (is (false?
         (compatibility/compatible-contract?
          [:map [:id :string]]
          [:map {:closed true} [:id :string]])))))

(deftest malformed-map-entries-fail-closed
  (doseq [malformed [[:map :bogus]
                     [:map [:id]]
                     [:map [:id {:optional true}]]
                     [:map [:id :string :extra]]
                     [:map [:id :string] [:id :string]]]]
    (is (false?
         (compatibility/compatible-contract?
          malformed
          [:map])))
    (is (false?
         (compatibility/compatible-contract?
          malformed
          malformed)))))

(deftest malformed-supported-combinators-fail-closed
  (doseq [malformed [[:or]
                     [:and]
                     [:=]
                     [:= :ready :done]
                     [:enum]
                     [:any :bogus]
                     [:or [:and]]
                     [:or 42]
                     [:and false]
                     [:or :string {:title "misplaced"}]
                     [:or (list :string)]
                     [:map [:payload [:or]]]
                     [:map [:payload 42]]]]
    (is (false?
         (compatibility/compatible-contract?
          malformed
          :artifact/text)))
    (is (false?
         (compatibility/compatible-contract?
          :artifact/text
          malformed)))
    (is (false?
         (compatibility/compatible-contract?
          malformed
          malformed)))))

(deftest invalid-scalar-and-opaque-schema-forms-fail-closed
  (doseq [malformed [nil
                     false
                     42
                     []
                     [42]
                     [:vector 42]
                     [:vector [:bogus]]
                     [:tuple [:bogus] :artifact/text]
                     [:map-of :artifact/key [:bogus]]
                     [:map [:payload [:vector [:bogus]]]]]]
    (is (false?
         (compatibility/compatible-contract?
          malformed
          :any)))
    (is (false?
         (compatibility/compatible-contract?
          malformed
          malformed)))))

(deftest valid-nominal-and-opaque-schema-forms-remain-exact
  (doseq [schema [:artifact/text
                  "artifact/text"
                  'artifact/text
                  string?
                  [:vector :artifact/text]
                  [:vector [:vector :artifact/text]]
                  [:vector [:= [1 2]]]
                  [:map-of :artifact/key :artifact/value]
                  [:tuple [:= [1 2]] :artifact/text]
                  [:> 0]]]
    (is (compatibility/compatible-contract? schema schema))))

(deftest unknown-relations-fail-closed
  (is (false?
       (compatibility/compatible-contract?
        [:vector :string]
        [:sequential :string]))))
