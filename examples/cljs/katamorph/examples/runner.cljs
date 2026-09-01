(ns katamorph.examples.runner
  "Executable examples for the manifest, schema, and action-interpreter seams."
  (:require [cljs.reader :as reader]
            [katamorph.action.interpreter :as action]
            [katamorph.manifest :as manifest]
            [katamorph.schema :as schema]
            ["node:fs" :as fs]))

(def example-paths
  ["examples/hello-world.edn"
   "examples/host-runtime.edn"
   "examples/github-interaction.edn"])

(defn read-example
  [path]
  (reader/read-string (.readFileSync fs path "utf8")))

(defn definitions
  [namespace-file]
  (schema/assert! :namespace namespace-file)
  (let [resource-definitions (manifest/namespace-file-definitions namespace-file)]
    (doseq [{:resource/keys [kind definition]} resource-definitions]
      (schema/assert! kind definition))
    resource-definitions))

(defn example-report
  [path]
  (let [namespace-file (read-example path)
        resource-definitions (definitions namespace-file)]
    {:path path
     :namespace (:namespace namespace-file)
     :resources (count resource-definitions)
     :kinds (->> resource-definitions (map :resource/kind) distinct vec)}))

(defn hello-config
  "Small dependency map proving that Katamorph's interpreter stays pure:
   the application supplies the side-effecting action runner."
  [resource-definitions]
  {:contract-runtime/deps
   {:run-action!
    (fn [_ctx action]
      (let [{:keys [greeting name]} (:action/with action)]
        (js/Promise.resolve
         {:message (str greeting ", " name "!")
          :handled-by (:action/kind action)})))
    :get-action #(when (= :demo/greet %) :demo/greet-handler)
    :get-scope-declaration (constantly nil)
    :filter-fn (constantly nil)
    :load-resources (constantly resource-definitions)
    :get-store (fn [_config _store-id] nil)}})

(defn run-hello!
  []
  (let [resource-definitions (definitions (read-example (first example-paths)))
        config (hello-config resource-definitions)
        trigger (->> resource-definitions
                     (filter #(= :trigger (:resource/kind %)))
                     first
                     :resource/definition)]
    (action/execute!
     {:config config}
     {:action/kind (:trigger/action trigger)
      :action/with (:trigger/with trigger)})))

(defn ^:async main
  []
  (let [reports (mapv example-report example-paths)
        result (await (run-hello!))]
    (println "Validated Katamorph examples:")
    (doseq [{:keys [path namespace resources kinds]} reports]
      (println " -" path namespace resources kinds))
    (println "Executed hello-world:" (pr-str result))))
