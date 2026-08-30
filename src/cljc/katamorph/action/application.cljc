(ns katamorph.action.application
  (:require [clojure.set :as set]
            [katamorph.action.invocation :as invocation]
            [katamorph.schema.action :as action]
            [malli.core :as m]))

(defn- required-inputs [action-contract]
  (set (keys (:action/requires action-contract {}))))

(defn- supplied-inputs [request]
  (set (keys (:operation/in request {}))))

(defn errors
  "Return structural errors proving whether one InvocationRequest may apply an ActionSemantics contract.

   This validates port names only. Input values may still be references whose
   values are unavailable until a workflow/runtime resolves them."
  [action-contract request]
  (cond
    (not (m/validate action/ActionSemantics action-contract))
    [{:law/id :action/application-invalid-action}]

    (not (invocation/valid? request))
    [{:law/id :action/application-invalid-invocation}]

    :else
    (let [expected-id (:contract/id action-contract)
          actual-id (:operation/id request)
          required (required-inputs action-contract)
          supplied (supplied-inputs request)]
      (vec
       (concat
        (when (not= expected-id actual-id)
          [{:law/id :action/application-id-mismatch
            :expected expected-id
            :actual actual-id}])
        (for [port (sort-by str (set/difference required supplied))]
          {:law/id :action/application-missing-input
           :input port})
        (for [port (sort-by str (set/difference supplied required))]
          {:law/id :action/application-undeclared-input
           :input port}))))))

(defn validate [action-contract request]
  (let [problems (errors action-contract request)]
    {:ok (empty? problems)
     :action action-contract
     :invocation request
     :errors problems}))
