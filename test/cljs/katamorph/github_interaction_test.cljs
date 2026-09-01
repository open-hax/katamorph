(ns katamorph.github-interaction-test
  (:require [cljs.reader :as reader]
            [cljs.test :refer [deftest is testing]]
            [katamorph.manifest :as manifest]
            [katamorph.store.protocol :as store]
            [katamorph.store.registry :as store-registry]
            ["node:fs" :as fs]
            ["node:path" :as path]))

(def github-interaction-path
  (.resolve path js/__dirname ".." "examples" "github-interaction.edn"))

(defn- resource-definitions
  []
  (-> (.readFileSync fs github-interaction-path "utf8")
      reader/read-string
      manifest/namespace-file-definitions))

(defn- runtime-config
  []
  {:contract-runtime/deps
   {:load-resources (fn [_config] (resource-definitions))}})

(def valid-record
  {:record/id "urn:uuid:2d86247b-8d86-493e-ad8f-a1c97ae62ba2"
   :stream/id "axxium:stream:github-installation-1"
   :stream/position 1
   :event/id "github:delivery:example"
   :event/kind :github/delivery-received
   :event/observed-at "2026-09-01T17:00:00Z"
   :source/provider :github
   :source/object-id "repository:123"
   :payload {:delivery/id "example"}})

(deftest ^:async github-event-ledger-resource-is-executable
  (store-registry/reset-stores!)
  (let [event-ledger (store-registry/get-store!
                      (runtime-config)
                      :open-hax.github/event-ledger)]
    (testing "the declared store resolves through the default memory runtime"
      (is (some? event-ledger)))
    (testing "a conforming event record can be admitted and read back"
      (is (= valid-record (await (store/insert! event-ledger valid-record))))
      (is (= [valid-record]
             (await (store/find-docs event-ledger
                                     {:event/id "github:delivery:example"})))))
    (testing "the executable schema rejects malformed provider records"
      (try
        (await (store/insert! event-ledger
                              (assoc valid-record :source/provider :discord)))
        (is false "expected the GitHub provider guard to reject the record")
        (catch :default error
          (is (some? (:errors (ex-data error)))))))))
