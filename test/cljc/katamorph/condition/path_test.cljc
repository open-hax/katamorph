(ns katamorph.condition.path-test
  #?(:clj (:require [clojure.test :refer [deftest is]]
                    [katamorph.condition.path :as path])
     :cljs (:require [cljs.test :refer [deftest is]]
                     [katamorph.condition.path :as path])))

(deftest explicit-path-law
  (let [context {:a {:b nil} :xs [:x :y]}]
    (is (= {:found? true :value nil} (path/value-at context [:a :b])))
    (is (= {:found? true :value :y} (path/value-at context [:xs 1])))
    (is (= {:found? false} (path/value-at context nil)))
    (is (= {:found? true :value context} (path/value-at context [])))))
