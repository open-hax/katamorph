(ns katamorph.jvm-test-runner
  (:require [clojure.test :as test]
            [katamorph.schema.action-semantics-test]
            [katamorph.schema.registry-test]
            [katamorph.schema.step-dataflow-test]
            [katamorph.schema.validation-test]))

(defn -main [& _]
  (let [result (test/run-tests 'katamorph.schema.action-semantics-test
                               'katamorph.schema.registry-test
                               'katamorph.schema.step-dataflow-test
                               'katamorph.schema.validation-test)]
    (when (pos? (+ (:fail result) (:error result)))
      (throw (ex-info "Katamorph JVM tests failed" result)))))
