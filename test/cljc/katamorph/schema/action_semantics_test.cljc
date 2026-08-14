(ns katamorph.schema.action-semantics-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.schema.action :as action]
               [katamorph.schema.validation :as validation])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.schema.action :as action]
               [katamorph.schema.validation :as validation])))

(deftest actions-declare-category-and-ports
  (is (:ok
       (validation/validate-schema
        action/ActionSemantics
        {:contract/kind :action
         :contract/id :translate
         :action/category :transduction
         :action/requires {:source :any}
         :action/provides {:candidate :any}
         :action/traits #{:generative :lossy}}))))

(deftest category-is-required
  (is (false?
       (:ok
        (validation/validate-schema
         action/ActionSemantics
         {:contract/kind :action
          :contract/id :translate})))))

(deftest ports-and-category-are-declared-together
  (is (action/category-declared-with-ports?
       {:action/category :transduction :action/requires {:source :any}}))
  (is (not (action/category-declared-with-ports?
            {:action/requires {:source :any}})))
  (is (not (action/category-declared-with-ports?
            {:action/provides {:candidate :any}})))
  (is (not (action/category-declared-with-ports?
            {:action/traits #{:lossy}}))))

(deftest actions-without-ports-need-no-category
  (is (action/category-declared-with-ports?
       {:contract/kind :action :contract/id :knoxx/dispatch :action/kind :handler})
      "the law constrains the port vocabulary, not every action ever written"))
