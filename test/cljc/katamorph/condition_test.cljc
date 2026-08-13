(ns katamorph.condition-test
  #?(:clj (:require [clojure.test :refer [deftest is]]
                    [katamorph.condition :as condition])
     :cljs (:require [cljs.test :refer [deftest is]]
                     [katamorph.condition :as condition])))

(def context
  {:artifact {:kind :finding :status nil}
   :event {:type :artifact/changed}})

(deftest leaf-conditions
  (is (condition/match? context {:condition/op :eq
                                 :condition/path [:artifact :kind]
                                 :condition/value :finding}))
  (is (condition/match? context {:condition/op :not-eq
                                 :condition/path [:artifact :kind]
                                 :condition/value "finding"}))
  (is (condition/match? context {:condition/op :exists
                                 :condition/path [:artifact :status]}))
  (is (condition/match? context {:condition/op :in
                                 :condition/path [:event :type]
                                 :condition/values [:artifact/created :artifact/changed]}))
  (is (false? (condition/match? context {:condition/op :exists}))))

(deftest composed-conditions
  (is (condition/match?
       context
       {:condition/op :and
        :condition/clauses
        [{:condition/op :eq
          :condition/path [:artifact :kind]
          :condition/value :finding}
         {:condition/op :or
          :condition/clauses
          [{:condition/op :eq
            :condition/path [:event :type]
            :condition/value :artifact/created}
           {:condition/op :eq
            :condition/path [:event :type]
            :condition/value :artifact/changed}]}
         {:condition/op :not
          :condition/clause
          {:condition/op :eq
           :condition/path [:artifact :kind]
           :condition/value :other}}]})))
