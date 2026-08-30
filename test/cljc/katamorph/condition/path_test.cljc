(ns katamorph.condition.path-test
  #?(:clj (:require [clojure.test :refer [deftest is]]
                    [katamorph.condition.path :as path])
     :cljs (:require [cljs.test :refer [deftest is]]
                     [katamorph.condition.path :as path])))

(deftest explicit-path-law
  (let [context {:a {:b nil} :xs [:x :y]}]
    (is (= {:found? true :value nil} (path/value-at context [:a :b])))
    (is (= {:found? true :value :numeric} (path/value-at {1 :numeric} [1.0])))
    (is (= {:found? true :value :numeric} (path/value-at {1.0 :numeric} [1])))
    #?(:clj
       (is (= {:found? false}
              (path/value-at {1 :integer, 1.0 :double} [1]))
           "distinct JVM keys with one portable identity are ambiguous"))
    (is (= {:found? true :value :y} (path/value-at context [:xs 1])))
    (is (= {:found? true :value :y} (path/value-at context [:xs 1.0])))
    (is (= {:found? false} (path/value-at context nil)))
    (is (= {:found? true :value context} (path/value-at context [])))))
