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

  (testing "a closed consumer cannot accept an open producer"
    (is (false?
         (compatibility/compatible-contract?
          [:map [:id :string]]
          [:map {:closed true} [:id :string]])))))

(deftest unknown-relations-fail-closed
  (is (false?
       (compatibility/compatible-contract?
        [:vector :string]
        [:sequential :string]))))
