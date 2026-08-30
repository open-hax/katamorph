(ns katamorph.jvm-test-runner
  (:require [clojure.test :as test]
            [katamorph.action.application-test]
            [katamorph.action.binding-registry-test]
            [katamorph.action.invocation-test]
            [katamorph.action.registry-test]
            [katamorph.condition-test]
            [katamorph.condition.path-test]
            [katamorph.schema.action-semantics-test]
            [katamorph.schema.condition-test]
            [katamorph.schema.registry-test]
            [katamorph.schema.relation-test]
            [katamorph.schema.step-dataflow-test]
            [katamorph.schema.validation-test]
            [katamorph.workflow.cycle-validation-test]
            [katamorph.workflow.graph-test]
            [katamorph.workflow.validation-test]))

(def suites
  ['katamorph.condition-test
   'katamorph.condition.path-test
   'katamorph.schema.condition-test
   'katamorph.schema.registry-test
   'katamorph.schema.validation-test
   'katamorph.schema.relation-test
   'katamorph.schema.action-semantics-test
   'katamorph.action.registry-test
   'katamorph.action.binding-registry-test
   'katamorph.action.application-test
   'katamorph.action.invocation-test
   'katamorph.schema.step-dataflow-test
   'katamorph.workflow.validation-test
   'katamorph.workflow.graph-test
   'katamorph.workflow.cycle-validation-test])

(defn -main [& _]
  (let [result (apply test/run-tests suites)]
    (when (pos? (+ (:fail result) (:error result)))
      (throw (ex-info "Katamorph JVM tests failed" result)))))
