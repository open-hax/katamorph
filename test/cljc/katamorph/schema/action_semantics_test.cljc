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
