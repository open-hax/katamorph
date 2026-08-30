(ns katamorph.schema-test
  (:require [cljs.test :refer [deftest is]]
            [katamorph.schema :as schema]))

(deftest infer-contract-class-by-kind
  (is (= :agent (schema/infer-contract-class {:contract/kind :agent})))
  (is (= :actor (schema/infer-contract-class {:contract/kind :actor})))
  (is (= :policy (schema/infer-contract-class {:contract/kind :policy})))
  (is (= :fulfillment (schema/infer-contract-class {:contract/kind :fulfillment})))
  (is (= :trigger (schema/infer-contract-class {:contract/kind :trigger}))))

(deftest infer-contract-class-by-kind-string
  (is (= :agent (schema/infer-contract-class {:contract/kind "agent"})))
  (is (= :policy (schema/infer-contract-class {:contract/kind "policy"}))))

(deftest infer-contract-class-by-heuristic
  (is (= :actor (schema/infer-contract-class {:actor/id "user1"})))
  (is (= :role (schema/infer-contract-class {:role/id :admin})))
  (is (= :capability (schema/infer-contract-class {:cap/id :read})))
  (is (= :model (schema/infer-contract-class {:model/id "gpt-5"})))
  (is (= :model-family (schema/infer-contract-class {:model-family/id "gpt"})))
  (is (= :generator (schema/infer-contract-class {:generator/id "gen1"})))
  (is (= :schedule (schema/infer-contract-class {:schedule/id "sched1"})))
  (is (= :sub-agent (schema/infer-contract-class {:parent-agent "parent1"})))
  (is (= :agent (schema/infer-contract-class {:contract/id "my-agent"}))))

(deftest infer-contract-class-fallback
  (is (= :agent (schema/infer-contract-class {})))
  (is (= :agent (schema/infer-contract-class "not-a-map")))
  (is (= :agent (schema/infer-contract-class nil))))

(deftest schema-for-known-kinds
  (is (some? (schema/schema-for :agent)))
  (is (some? (schema/schema-for :actor)))
  (is (some? (schema/schema-for :role)))
  (is (some? (schema/schema-for :policy)))
  (is (some? (schema/schema-for :fulfillment)))
  (is (some? (schema/schema-for :trigger)))
  (is (some? (schema/schema-for :model)))
  (is (some? (schema/schema-for :model-family))))

(deftest schema-for-string-kind
  (is (some? (schema/schema-for "agent")))
  (is (some? (schema/schema-for "actor"))))

(deftest schema-for-throws-on-unknown
  (is (thrown-with-msg? js/Error #"Unknown contract kind"
        (schema/schema-for :nonexistent))))

(deftest validate-agent-contract
  (let [agent {:contract/id "test" :contract/kind :agent}]
    (is (:ok (schema/validate agent)))))

(deftest validate-actor-contract
  (let [actor {:actor/id "user1" :actor/kind :user}]
    (is (:ok (schema/validate actor)))))

(deftest validate-fails-missing-required
  (let [result (schema/validate :actor {:actor/kind :user})]
    (is (not (:ok result)))
    (is (seq (:errors result)))))

(deftest validate-with-explicit-class
  (let [result (schema/validate :role {:role/id :admin})]
    (is (:ok result)))
  (let [result (schema/validate :role {})]
    (is (not (:ok result)))))

(deftest validate-infers-class-when-nil
  (let [result (schema/validate nil {:actor/id "u" :actor/kind :user})]
    (is (:ok result))
    (is (= :actor (schema/infer-contract-class (:value result))))))

(deftest assert!-passes-valid
  (is (= {:contract/id "x" :contract/kind :agent}
         (schema/assert! :agent {:contract/id "x" :contract/kind :agent}))))

(deftest assert!-throws-on-invalid
  (is (thrown? js/Error
        (schema/assert! :actor {}))))

;; ── v0.2.0: new kinds ─────────────────────────────────────────────────────────

(deftest infer-new-kinds-by-heuristic
  (is (= :mcp-server (schema/infer-contract-class {:mcp-server/id "knoxx"})))
  (is (= :mcp-server (schema/infer-contract-class {:mcp_server/id "knoxx"})))
  (is (= :provider (schema/infer-contract-class {:provider/id :proxx})))
  (is (= :source-mode (schema/infer-contract-class {:source-mode/id :social})))
  (is (= :runtime-feature (schema/infer-contract-class {:runtime-feature/id "receipts"}))))

(deftest validate-mcp-server-contract
  (is (:ok (schema/validate :mcp-server
                            {:contract/id "knoxx-mcp"
                             :contract/kind :mcp-server
                             :mcp-server/transport :http
                             :mcp-server/url "http://127.0.0.1:8000/mcp"
                             :enabled true})))
  ;; underscore spelling routes to the same schema
  (is (:ok (schema/validate :mcp_server
                            {:contract/id "knoxx-mcp"
                             :mcp_server/transport "http"
                             :mcp_server/url "http://127.0.0.1:8000/mcp"}))))

(deftest validate-provider-contract
  (is (:ok (schema/validate :provider
                            {:provider/id :proxx
                             :provider/label "Proxx"
                             :provider/base-url "http://127.0.0.1:8789"
                             :provider/api-shape :openai-chat
                             :provider/auth {:auth/mode :bearer :auth/env "PROXX_TOKEN"}
                             :provider/models-endpoint "/v1/models"
                             :provider/model-prefix-allowlist ["glm-5" "gpt-5"]})))
  (is (not (:ok (schema/validate :provider {:provider/label "no id"}))))
  (is (not (:ok (schema/validate :provider
                                 {:provider/id :p
                                  :provider/auth {:auth/env "X"}})))))

(deftest validate-model-catalog-contracts
  (is (:ok (schema/validate :model-family
                            {:model-family/id :gpt
                             :model-family/prefixes ["gpt-"]})))
  (is (:ok (schema/validate :model
                            {:model/id "gpt-5"
                             :model/family :open-hax/gpt
                             :model/provider :open-hax/proxx}))))

(deftest validate-source-mode-and-runtime-feature
  (is (:ok (schema/validate :source-mode
                            {:contract/kind :source-mode
                             :contract/id "social-replies"
                             :source-mode/id :social})))
  (is (:ok (schema/validate :runtime-feature
                            {:contract/kind :runtime-feature
                             :contract/id "receipt-river"
                             :eta-mu/extension :receipt-river
                             :enabled true}))))

(deftest validate-cms-contract
  (is (:ok (schema/validate :cms-templates
                            {:contract/id "site-templates"
                             :contract/kind :cms-templates
                             :templates {}})))
  (is (not (:ok (schema/validate :cms-templates
                                 {:contract/id "bad" :contract/kind :not-cms})))))

;; ── v0.2.0: dialect tolerance (knoxx/sol disk contracts) ──────────────────────

(deftest validate-flat-sub-agent-dialect
  ;; sol/knoxx flat form: no :parent-agent, :sub-agent/* fields, string role.
  (is (:ok (schema/validate :sub-agent
                            {:contract/id "test_sub_agent"
                             :contract/kind :sub-agent
                             :sub-agent/parent-capabilities :restrict
                             :sub-agent/capabilities [:read :semantic_query]
                             :sub-agent/model "gpt-4o"
                             :sub-agent/thinking "medium"
                             :sub-agent/role "researcher"
                             :sub-agent/timeout-ms 30000
                             :sub-agent/mode :await
                             :agent {:role "researcher" :model "gpt-4o" :thinking "medium"}}))))

(deftest validate-flat-policy-dialect
  ;; knoxx/sol flat policy: string id, invariant check maps, no :policy/outcome.
  (is (:ok (schema/validate :policy
                            {:contract/id "review-gate"
                             :contract/kind :policy
                             :policy/invariants [{:id "no-force-push"
                                                  :severity :block
                                                  :message "force push blocked"
                                                  :check {:rule :git/force-push}}]
                             :policy/checked-by :review})))
  ;; proxx tree dialect still validates
  (is (:ok (schema/validate :policy
                            {:contract/id :route-policy
                             :contract/kind :policy
                             :policy/outcome :apply})))
  ;; regression: :policy/children recursion resolves without external
  ;; registry options (was a latent :malli.core/invalid-ref until v0.2.0)
  (is (:ok (schema/validate :policy
                            {:contract/id :parent
                             :contract/kind :policy
                             :policy/outcome :apply
                             :policy/children [{:contract/id :child
                                                :contract/kind :policy
                                                :policy/outcome :block}]}))))

(deftest validate-tolerant-agent-dialect
  ;; sol agent dialect: actors as a set with wildcard, string roles, ui actions.
  (is (:ok (schema/validate :agent
                            {:contract/id "ussyverse_social_replies"
                             :contract/kind :agent
                             :contract/actors #{"*"}
                             :actor/roles [:social]
                             :agent {:role "poster"}
                             :ui/actions [{:id "reply" :label "Reply"}]}))))

(deftest validate-page-actor
  (is (:ok (schema/validate :actor {:actor/id "landing" :actor/kind :page}))))

;; ── Workflow resource ─────────────────────────────────────────────────────────

(def ^:private minimal-workflow
  {:contract/kind :workflow
   :contract/id "main-pr-gate"
   :workflow/name "eta-mu-main-pr-gate"
   :workflow/triggers [{:on/event :pull-request
                        :on/branches ["main"]
                        :on/paths [".github/workflows/*.yml"]}]
   :workflow/jobs
   [{:job/id "lint"
     :job/runner :ubuntu-latest
     :job/permissions {:contents :read}
     :job/timeout-minutes 20
     :job/steps [{:step/action :actions/checkout
                  :step/with {:persist-credentials false}}
                 {:step/run "pnpm lint"}]}]})

(deftest workflow-kind-is-registered
  (is (some? (schema/schema-for :workflow)))
  (is (= :workflow (schema/infer-contract-class {:contract/kind :workflow})))
  (is (= :workflow (schema/infer-contract-class {:workflow/id "ci"}))
      "a bare :workflow/id infers the kind, like :schedule/id and :generator/id"))

(deftest validate-minimal-workflow
  (is (:ok (schema/validate :workflow minimal-workflow))))

(deftest workflow-requires-jobs
  (is (not (:ok (schema/validate :workflow (dissoc minimal-workflow :workflow/jobs))))
      "a workflow with no jobs describes no work and must not validate"))

(deftest workflow-job-requires-an-id-and-steps
  (is (not (:ok (schema/validate :workflow
                                 (assoc minimal-workflow :workflow/jobs [{:job/steps []}]))))
      "a job without :job/id cannot be referenced by :job/needs")
  (is (not (:ok (schema/validate :workflow
                                 (assoc minimal-workflow :workflow/jobs [{:job/id "a"}]))))
      "a job without :job/steps runs nothing"))

(deftest workflow-carries-a-job-dag
  (is (:ok (schema/validate
            :workflow
            (assoc minimal-workflow :workflow/jobs
                   [{:job/id "a" :job/steps [{:step/run "true"}]}
                    {:job/id "b" :job/needs ["a"] :job/steps [{:step/run "true"}]}])))
      "no other kind carries dependency ordering; :job/needs is why this schema exists"))

(deftest workflow-permissions-are-capability-shaped
  (is (:ok (schema/validate
            :workflow
            (assoc minimal-workflow :workflow/permissions {:contents :read :pull-requests :write})))
      "permissions are namespaced keywords, not a host's YAML block")
  (is (not (:ok (schema/validate
                 :workflow
                 (assoc minimal-workflow :workflow/permissions {"contents" "read"}))))
      "string keys are a host dialect and belong in :workflow/raw"))

(deftest workflow-keeps-host-expressions-opaque
  (is (:ok (schema/validate
            :workflow
            (assoc-in minimal-workflow [:workflow/jobs 0 :job/if]
                      "${{ github.event_name == 'push' }}")))
      "Katamorph does not evaluate host expressions; it carries them as strings"))

(deftest workflow-has-a-raw-escape-hatch
  (is (:ok (schema/validate
            :workflow
            (assoc minimal-workflow :workflow/raw {:github-actions {:defaults {:run {:shell "bash"}}}})))
      "a language that cannot express the last five percent gets abandoned"))

(deftest workflow-matrix-is-not-strategy-contract
  (is (:ok (schema/validate
            :workflow
            (assoc-in minimal-workflow [:workflow/jobs 0 :job/matrix] {:node [20 22]})))
      "StrategyContract already means a retry policy; a matrix must not reuse it"))

;; ── Typed ports and step dataflow through the registered kinds ────────────────
;; These exercise schema/validate rather than the portable schemas directly. A
;; constraint only reachable by naming ActionSemantics or ActionStep is not a
;; constraint any consumer of the documented API is subject to.

(def ^:private ported-action
  {:contract/kind :action
   :contract/id :translate
   :action/category :transduction
   :action/requires {:source :any}
   :action/provides {:candidate :any}
   :action/traits #{:generative :lossy}})

(deftest registered-action-accepts-typed-ports
  (is (:ok (schema/validate :action ported-action))))

(deftest registered-action-rejects-malformed-ports
  (is (not (:ok (schema/validate :action (assoc ported-action :action/requires "source"))))
      ":action/requires is a port map, not a name")
  (is (not (:ok (schema/validate :action (assoc ported-action :action/provides [:candidate]))))
      ":action/provides is a port map, not a list of port names")
  (is (not (:ok (schema/validate :action (assoc ported-action :action/traits [:lossy]))))
      "traits are a set; a vector admits duplicates and implies order"))

(deftest registered-action-requires-category-alongside-ports
  (is (not (:ok (schema/validate :action (dissoc ported-action :action/category))))
      "ports without a category describe a flow nothing can classify"))

(deftest actions-predating-ports-still-validate
  (is (:ok (schema/validate :action {:contract/kind :action
                                     :contract/id :knoxx/dispatch
                                     :action/kind :handler
                                     :action/handler "dispatch"}))
      "the port vocabulary is opt-in; knoxx heritage actions declare none"))

(deftest registered-workflow-enforces-portable-step-input-shapes
  (let [with-step-in (fn [in]
                       (assoc-in minimal-workflow
                                 [:workflow/jobs 0 :job/steps 0 :step/in] in))]
    (is (:ok (schema/validate :workflow
                              (with-step-in {:candidate [:step :translate :candidate]}))))
    (is (:ok (schema/validate :workflow
                              (with-step-in {:candidate [:translate :candidate]})))
        "an unreserved portable vector is an ordinary literal")
    (is (:ok (schema/validate :workflow (with-step-in {:candidate "translate"})))
        "a portable string is an ordinary literal")
    (is (not (:ok (schema/validate :workflow
                                   (with-step-in {:candidate [:step :translate]}))))
        "a reserved step-reference vector must satisfy its exact shape")
    (is (not (:ok (schema/validate :workflow (with-step-in {:candidate [:event]}))))
        "other malformed reserved references fail closed")))
