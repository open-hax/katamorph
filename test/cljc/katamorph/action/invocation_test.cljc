(ns katamorph.action.invocation-test
  #?(:clj (:require [clojure.test :refer [deftest is testing]]
                    [katamorph.action.invocation :as invocation])
     :cljs (:require [cljs.test :refer-macros [deftest is testing]]
                     [katamorph.action.invocation :as invocation])))

(deftest portable-invocation-request-preserves-existing-wire-shape
  (is (invocation/valid?
       {:operation/id :translation/transduce
        :operation/with {:target-locale :fr
                         "policy" {:mode :strict
                                   :labels #{:terminology :review}}}
        :operation/in {:source [:step :load :document]}})))

(deftest runtime-values-cannot-enter-invocation-data
  (testing "provider and handler identity are not fields on the request"
    (is (false?
         (invocation/valid?
          {:operation/id :translation/transduce
           :operation/provider :provider/local}))))
  (testing "code is not portable configuration"
    (is (false?
         (invocation/valid?
          {:operation/id :translation/transduce
           :operation/with {:callback (fn [] :runtime)}})))))

(deftest unsafe-jvm-only-numbers-fail-the-portable-boundary
  #?(:clj
     (is (false?
          (invocation/valid?
           {:operation/id :evaluation/score
            :operation/with {:weight 9007199254740992}})))
     :cljs
     (is true)))
