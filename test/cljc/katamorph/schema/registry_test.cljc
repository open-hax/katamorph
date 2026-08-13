(ns katamorph.schema.registry-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [katamorph.schema.core :as core])
     :cljs
     (:require [cljs.test :refer [deftest is]]
               [katamorph.schema.core :as core])))

(deftest kind-normalization-is-portable
  (is (= :artifact (core/normalize-kind :artifact)))
  (is (= :artifact (core/normalize-kind "artifact"))))

(deftest registry-composition-is-conflict-aware
  (is (= {:ok true
          :registry {:artifact [:map] :relation [:map]}
          :conflicts []}
         (core/compose-registries {:artifact [:map]}
                                  {:relation [:map]})))

  (is (= [:artifact]
         (core/registry-conflicts
          [{:artifact [:map]}
           {:artifact [:map {:closed false}]}])))

  (is (= {:ok false :registry nil :conflicts [:artifact]}
         (core/compose-registries
          {:artifact [:map]}
          {:artifact [:map {:closed false}]}))))

(deftest registry-composition-has-an-empty-identity
  (is (= {:ok true :registry {} :conflicts []}
         (core/compose-registries))))

(deftest registry-composition-normalizes-schema-ids
  (is (= [:artifact]
         (core/registry-conflicts
          [{"artifact" [:map]}
           {:artifact [:map {:closed false}]}])))
  (is (= {:ok true :registry {:artifact [:map]} :conflicts []}
         (core/compose-registries {"artifact" [:map]})))
  (is (= [:map]
         (core/schema-for (:registry (core/compose-registries {"artifact" [:map]}))
                          :artifact))))
