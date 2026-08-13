(ns katamorph.schema
  "Unified resource boundary schema registry.

   Merges schemas from:
   - proxx.schema (provider/policy/strategy)
   - knoxx.backend resource definitions (agent/actor/role/capability/action/trigger/schedule/generator)
   - eta-mu.contract-runtime-v2.core (policy-gate/fulfillment match maps)

   EDN files describe resources. Contracts are the schema/policy boundaries
   those resources must satisfy. The :contract/kind key remains the migration
   discriminator. Maps use {:closed false} to tolerate dialect-specific fields."
  (:require [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [katamorph.schema.action :as action]
            [katamorph.schema.step :as step]))

;; ── Primitives ────────────────────────────────────────────────────────────────

(def ContractId
  [:or string? keyword?])

(def ToolId :string)

(def ISODuration :string)

(def EvalOp
  [:enum :all :some :none :not :assert])

(def PolicyOutcome
  [:enum :apply :try :next :reduce :block :warn :note :allow])

(def Severity
  [:enum :block :warn :note])

;; ── Eval nodes (proxx heritage) ───────────────────────────────────────────────

(def EvalNode
  [:map
   [:eval/op EvalOp]
   [:eval/target {:optional true} :keyword]
   [:eval/forms [:vector :any]]])

;; ── Policy match (eta-mu v2 heritage) ─────────────────────────────────────────

(def PolicyMatch
  [:map
   [:tool/name   {:optional true} :string]
   [:tool/params {:optional true} [:map-of :keyword :any]]])

;; ── Fulfillment match (eta-mu v2 heritage) ────────────────────────────────────

(def FulfillmentMatch
  [:map
   [:tool/name   {:optional true} :string]
   [:tool/params {:optional true} [:map-of :keyword :any]]
   [:tool/output {:optional true} :any]
   [:tool/error? {:optional true} :boolean]])

;; ── Agent contract (knoxx heritage) ───────────────────────────────────────────

(def AgentSpec
  [:map {:closed false}
   [:role     {:optional true} [:or keyword? string?]]
   [:roles    {:optional true} [:sequential [:or keyword? string?]]]
   [:model    {:optional true} [:maybe string?]]
   [:thinking {:optional true} [:or keyword? string?]]])

(def ActorCapSpec
  [:map {:closed false}
   [:capabilities {:optional true} [:sequential keyword?]]])

(def ContextPolicy
  [:map {:closed false}
   [:max-messages    {:optional true} int?]
   [:max-chars       {:optional true} int?]
   [:preserve-system {:optional true} boolean?]])

(def RuntimeSourceRef
  [:or keyword? string?
   [:map {:closed false}
    [:source/ref {:optional true} [:or keyword? string?]]
    [:source/id  {:optional true} [:or keyword? string?]]
    [:filters {:optional true} [:map {:closed false}]]
    [:hydration {:optional true} [:map {:closed false}]]
    [:source/hydration {:optional true} [:map {:closed false}]]]])

(def UiAction
  "Declarative UI action a surface can render for a contract (knoxx/sol heritage)."
  [:map {:closed false}
   [:id string?]
   [:label string?]
   [:kind {:optional true} [:or keyword? string?]]
   [:surface {:optional true} [:or keyword? string?]]
   [:surfaces {:optional true} [:sequential [:or keyword? string?]]]
   [:icon {:optional true} string?]
   [:intent {:optional true} [:or keyword? string?]]
   [:agent/contract {:optional true} string?]
   [:agent/actor {:optional true} string?]
   [:tool/id {:optional true} string?]
   [:media/from {:optional true} [:or keyword? string?]]
   [:requires {:optional true} [:sequential [:or keyword? string?]]]
   [:mode {:optional true} [:or keyword? string?]]
   [:confirm? {:optional true} boolean?]
   [:enabled? {:optional true} boolean?]])

(def SubAgentConfig
  "Configuration for how a sub-agent relates to its parent."
  [:map {:closed false}
   [:mode         {:optional true} [:enum :fire-and-forget :await :collect]]
   [:timeout-ms   {:optional true} int?]
   [:inherit-role {:optional true} boolean?]
   [:restrict-capabilities {:optional true} [:vector keyword?]]
   [:shared-context {:optional true} [:map {:closed false}]]
   [:result-key  {:optional true} string?]])

(def AgentContract
  "Agent contract. Doubles as the lenient fallback schema for unrecognized
   contract maps (sol/knoxx heritage), so :contract/kind is any keyword and
   actor bindings tolerate sets, sequences, and the \"*\" wildcard."
  [:map {:closed false}
   [:contract/id      string?]
   [:contract/kind    {:optional true} keyword?]
   [:contract/actor   {:optional true} string?]
   [:contract/actors  {:optional true} [:or [:set string?] [:sequential string?]]]
   [:actor/id         {:optional true} string?]
   [:actor/roles      {:optional true} [:sequential keyword?]]
   [:actor/capabilities {:optional true} [:sequential keyword?]]
   [:enabled          {:optional true} :boolean]
   [:trigger-kind     {:optional true} keyword?]
   [:agent            {:optional true} AgentSpec]
   [:actor            {:optional true} ActorCapSpec]
   [:prompts          {:optional true}
    [:map {:closed false}
     [:system {:optional true} :any]
     [:task   {:optional true} :any]]]
   [:memory           {:optional true} [:map {:closed false}]]
   [:sources          {:optional true} [:sequential RuntimeSourceRef]]
   [:context          {:optional true} ContextPolicy]
   [:context-policy   {:optional true} ContextPolicy]
   [:ui/actions       {:optional true} [:vector UiAction]]
   [:data             {:optional true} [:map {:closed false}]]])

(def SubAgentContract
  "A sub-agent contract: a child agent spawned by a parent.
   Two dialects validate: the linked form (:parent-agent + :sub-agent/config)
   and the flat sol/knoxx form (:sub-agent/* fields, no parent link)."
  [:map {:closed false}
   [:contract/id      string?]
   [:contract/kind    [:= :sub-agent]]
   [:contract/actors  {:optional true} [:vector string?]]
   [:parent-agent     {:optional true} string?]
   [:sub-agent/config {:optional true} SubAgentConfig]
   [:sub-agent/parent-capabilities {:optional true} [:enum :inherit :restrict :none]]
   [:sub-agent/capabilities {:optional true} [:vector any?]]
   [:sub-agent/role   {:optional true} string?]
   [:sub-agent/model  {:optional true} [:maybe string?]]
   [:sub-agent/thinking {:optional true} [:or keyword? string?]]
   [:sub-agent/timeout-ms {:optional true} int?]
   [:sub-agent/mode   {:optional true} [:enum :fire-and-forget :await :collect]]
   [:enabled          {:optional true} :boolean]
   [:agent            {:optional true} AgentSpec]
   [:actor            {:optional true} ActorCapSpec]
   [:prompts          {:optional true}
    [:map {:closed false}
     [:system {:optional true} :any]
     [:task   {:optional true} :any]]]
   [:memory           {:optional true} [:map {:closed false}]]
   [:sources          {:optional true} [:sequential RuntimeSourceRef]]
   [:context          {:optional true} ContextPolicy]
   [:context-policy   {:optional true} ContextPolicy]
   [:data             {:optional true} [:map {:closed false}]]])

;; ── Actor contract (knoxx heritage) ───────────────────────────────────────────

(def ActorContract
  [:map {:closed false}
   [:actor/id            string?]
   [:actor/kind          [:enum :agent :user :page]]
   [:actor/email         {:optional true} string?]
   [:actor/username      {:optional true} string?]
   [:actor/accounts      {:optional true}
    [:map {:closed false}
     [:discord {:optional true} [:map {:closed false}
                                [:username {:optional true} string?]
                                [:user-id {:optional true} string?]
                                [:userid {:optional true} string?]]]
     [:bluesky {:optional true} [:map {:closed false}
                                [:handle {:optional true} string?]
                                [:did {:optional true} string?]]]
     [:twitch {:optional true} [:map {:closed false}
                               [:username {:optional true} string?]
                               [:user-id {:optional true} string?]
                               [:userid {:optional true} string?]]]]]
   [:actor/org           {:optional true} string?]
   [:actor/label         {:optional true} string?]
   [:actor/contract      {:optional true} string?]
   [:actor/default-agent {:optional true} string?]
   [:actor/roles         {:optional true} [:sequential keyword?]]
   [:actor/capabilities  {:optional true} [:sequential keyword?]]
   [:actor/sources       {:optional true} [:sequential RuntimeSourceRef]]
   [:sources             {:optional true} [:sequential RuntimeSourceRef]]
   [:ui/actions          {:optional true} [:vector UiAction]]])

;; ── Role contract (knoxx heritage) ────────────────────────────────────────────

(def RoleContract
  [:map {:closed false}
   [:role/id           keyword?]
   [:role/name         {:optional true} string?]
   [:role/description  {:optional true} string?]
   [:role/capabilities {:optional true} [:sequential keyword?]]
   [:role/permissions  {:optional true} [:sequential string?]]
   [:role/sources      {:optional true} [:sequential RuntimeSourceRef]]
   [:sources           {:optional true} [:sequential RuntimeSourceRef]]
   [:prompts           {:optional true}
    [:map {:closed false}
     [:system {:optional true} :any]
     [:task   {:optional true} :any]]]
   [:role/system-prompt {:optional true} :any]])

;; ── Capability contract (knoxx heritage) ──────────────────────────────────────

(def UserSurface
  [:map {:closed false}
   [:surface/id          keyword?]
   [:surface/label       string?]
   [:surface/kind        {:optional true} keyword?]
   [:surface/routes      {:optional true} [:vector string?]]
   [:surface/endpoints   {:optional true} [:vector string?]]
   [:surface/description {:optional true} string?]])

(def CapabilityContract
  [:map {:closed false}
   [:cap/id           keyword?]
   [:cap/tools        {:optional true} [:sequential any?]]
   [:cap/user-surfaces {:optional true} [:vector UserSurface]]])

;; ── Policy contract (proxx heritage — tree-shaped) ────────────────────────────

(def PolicyContract
  "Policy contract. Two dialects validate: the proxx tree shape (conditions,
   filters, :policy/outcome, children) and the knoxx/sol flat shape
   (:policy/invariants + :policy/required check maps, no outcome).
   Self-contained: :policy/children recurses through a local malli registry,
   so the schema validates without external registry options."
  [:schema {:registry
            {::policy
             [:map {:closed false}
              [:contract/id      ContractId]
              [:contract/kind    [:= :policy]]
              [:contract/doc     {:optional true} :string]
              [:contract/scope   {:optional true} :string]
              [:contract/uses    {:optional true} [:vector ContractId]]
              [:policy/condition {:optional true} EvalNode]
              [:policy/filters   {:optional true} [:vector EvalNode]]
              [:policy/outcome   {:optional true} PolicyOutcome]
              [:policy/checked-by {:optional true} keyword?]
              [:policy/strategy  {:optional true} :symbol]
              [:policy/children  {:optional true} [:vector [:ref ::policy]]]
              [:policy/sort      {:optional true} EvalNode]
              [:policy/project   {:optional true} [:vector :map]]
              [:policy/invariants {:optional true} [:vector :map]]
              [:policy/required  {:optional true} [:vector :map]]
              [:enabled          {:optional true} :boolean]]}}
   ::policy])

;; ── Policy gate contract (eta-mu v2 heritage — flat tool-call gating) ─────────

(def PolicyGateContract
  [:map {:closed false}
   [:contract/id    :string]
   [:contract/kind  [:= :policy-gate]]
   [:contract/doc   {:optional true} :string]
   [:policy/match   PolicyMatch]
   [:policy/action  Severity]
   [:policy/reason  {:optional true} :string]
   [:policy/ttl-ms  {:optional true} int?]
   [:enabled        {:optional true} :boolean]])

;; ── Fulfillment contract (eta-mu v2 heritage) ─────────────────────────────────

(def FulfillmentContract
  [:map {:closed false}
   [:contract/id         :string]
   [:contract/kind       [:= :fulfillment]]
   [:contract/doc        {:optional true} :string]
   [:fulfillment/match   FulfillmentMatch]
   [:fulfillment/mode    [:enum :notify :audit]]
   [:fulfillment/message :string]
   [:fulfillment/level   {:optional true} [:enum :info :warn :error]]
   [:enabled             {:optional true} :boolean]])

;; ── Strategy contract (proxx heritage) ────────────────────────────────────────

(def StrategyContract
  [:map {:closed false}
   [:contract/id     :keyword]
   [:contract/kind   [:= :strategy]]
   [:policy/strategy :symbol]
   [:policy/outcome  [:= :try]]])

;; ── Action contract (knoxx heritage) ──────────────────────────────────────────

;; The typed port keys come from katamorph.schema.action so the registered kind
;; and the portable ActionSemantics stay one definition. Category is optional
;; here and made mandatory by PortedActionLaw only once ports are declared —
;; knoxx actions predating the vocabulary keep validating unchanged.

(def ActionContract
  [:and
   (into [:map {:closed false}
          [:contract/kind   [:= :action]]
          [:contract/id     ContractId]
          [:action/id       {:optional true} ContractId]
          [:action/kind     {:optional true} keyword?]
          [:action/handler  {:optional true} :string]
          [:action/fn       {:optional true} :any]
          [:action/with     {:optional true} [:map {:closed false}]]
          [:action/scope    {:optional true} :map]
          [:enabled         {:optional true} :boolean]
          [:data            {:optional true}
           [:map {:closed false}
            [:context {:optional true} :map]
            [:output  {:optional true} :map]]]]
         action/PortKeys)
   action/PortedActionLaw])

;; ── Store resource (resource architecture) ────────────────────────────────────

(def StoreContract
  "Keyed persistence resource: a store id plus a Malli schema for documents."
  [:map {:closed false}
   [:contract/kind {:optional true} [:= :store]]
   [:contract/id   {:optional true} ContractId]
   [:store/id      [:or keyword? string?]]
   [:store/schema  {:optional true} :any]
   [:enabled       {:optional true} :boolean]])

;; ── Namespace resource file (resource architecture) ───────────────────────────

(def NamespaceFile
  "Namespace resource file: groups composite resources under one :namespace.
   Each entry may declare :trigger/*, :action/*, and :store/* keys at once;
   interpreters read only their own keys."
  [:map {:closed false}
   [:namespace [:or keyword? string?]]
   [:resources [:sequential [:map {:closed false}]]]])

;; ── Trigger contract (actor-first) ────────────────────────────────────────────

(def TriggerContract
  [:map {:closed false}
   [:contract/kind     [:= :trigger]]
   [:contract/id       ContractId]
   [:trigger/kind      [:enum :event]]
   [:trigger/events    [:sequential [:or string? keyword?]]]
   [:trigger/action    {:optional true} ContractId]
   [:trigger/agent     {:optional true} ContractId]
   [:trigger/actor     {:optional true} ContractId]
   [:trigger/emitter   {:optional true} ContractId]
   [:trigger/listener  {:optional true} ContractId]
   [:trigger/condition {:optional true} :any]
   [:trigger/with      {:optional true} [:map {:closed false}]]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

(def GeneratorContract
  [:map {:closed false}
   [:contract/kind     [:= :generator]]
   [:contract/id       ContractId]
   [:generator/id      {:optional true} ContractId]
   [:generator/kind    {:optional true} [:or string? keyword?]]
   [:generator/driver  {:optional true} [:or string? keyword?]]
   [:generator/actor   {:optional true} ContractId]
   [:generator/emits   {:optional true} [:sequential keyword?]]
   [:generator/policy  {:optional true} :map]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

(def ScheduleContract
  [:map {:closed false}
   [:contract/kind     [:= :schedule]]
   [:contract/id       ContractId]
   [:schedule/id       {:optional true} ContractId]
   [:schedule/rule     {:optional true} [:or string? keyword?]]
   [:schedule/cron     {:optional true} string?]
   [:schedule/at       {:optional true} string?]
   [:schedule/generator {:optional true} ContractId]
   [:schedule/event    {:optional true} [:map {:closed false}]]
   [:schedule/policy   {:optional true} :map]
   [:enabled           {:optional true} :boolean]
   [:data              {:optional true} :map]])

;; ── Workflow contract ────────────────────────────────────────────────────────
;;
;; A workflow is a resource like any other: an identity, a set of triggers, and
;; an ordered graph of jobs whose steps are actions. It describes *what runs and
;; when*; it does not name a host. Interpreting one for a CI provider, for a
;; local runner, or for a work-coordination board are all projections of the
;; same resource — which is the point of putting it here rather than in any one
;; of them.
;;
;; Deliberately absent: an expression language. Host expressions (GitHub's
;; `${{ }}`, for instance) travel as opaque strings in :step/if and friends.
;; Katamorph does not evaluate them and must not pretend to.

(def WorkflowRunner
  "Where a job runs. A keyword is a portable class a target resolves; a string is
   a host-specific label passed through untouched."
  [:or keyword? string?])

(def WorkflowStep
  [:map {:closed false}
   [:step/id      {:optional true} ContractId]
   [:step/name    {:optional true} string?]
   ;; Exactly one of :step/run or :step/action is expected. Kept as separate
   ;; optional keys rather than a variant so a target can report which it got.
   [:step/run     {:optional true} string?]
   [:step/action  {:optional true} [:or keyword? string?]]
   [:step/with    {:optional true} [:map {:closed false}]]
   ;; Dataflow, not config: each entry names an upstream step's output. Typed
   ;; from katamorph.schema.step so a bare vector cannot pass as a reference.
   [:step/in      {:optional true} step/StepInputs]
   [:step/env     {:optional true} [:map {:closed false}]]
   [:step/if      {:optional true} [:or string? EvalNode]]
   [:step/working-directory {:optional true} string?]
   [:step/continue-on-error {:optional true} :boolean]
   [:step/timeout-minutes   {:optional true} int?]])

(def WorkflowJob
  [:map {:closed false}
   [:job/id       ContractId]
   [:job/name     {:optional true} string?]
   [:job/runner   {:optional true} WorkflowRunner]
   ;; The dependency edge. Job ordering is a DAG, and no other katamorph kind
   ;; carries one — hence a first-class key rather than a reused policy field.
   [:job/needs    {:optional true} [:sequential ContractId]]
   [:job/if       {:optional true} [:or string? EvalNode]]
   [:job/matrix   {:optional true} [:map-of keyword? [:sequential :any]]]
   ;; A capability grant, expressed the way capabilities already are elsewhere:
   ;; namespaced keywords. `{:contents :read}` rather than a host's YAML block.
   [:job/permissions {:optional true} [:map-of keyword? keyword?]]
   [:job/timeout-minutes {:optional true} int?]
   [:job/env      {:optional true} [:map {:closed false}]]
   [:job/steps    [:sequential WorkflowStep]]])

(def WorkflowTrigger
  "One reason a workflow runs. `:on/event` reuses the trigger vocabulary; a
   cron trigger carries `:on/cron` the way ScheduleContract does."
  [:map {:closed false}
   [:on/event     {:optional true} [:or keyword? string?]]
   [:on/cron      {:optional true} string?]
   [:on/branches  {:optional true} [:sequential string?]]
   [:on/paths     {:optional true} [:sequential string?]]
   [:on/types     {:optional true} [:sequential [:or keyword? string?]]]
   [:on/inputs    {:optional true} [:map {:closed false}]]
   [:on/with      {:optional true} [:map {:closed false}]]])

(def WorkflowContract
  [:map {:closed false}
   [:contract/kind [:= :workflow]]
   [:contract/id   ContractId]
   [:workflow/id   {:optional true} ContractId]
   [:workflow/name {:optional true} string?]
   [:workflow/doc  {:optional true} string?]
   [:workflow/triggers   {:optional true} [:sequential WorkflowTrigger]]
   [:workflow/permissions {:optional true} [:map-of keyword? keyword?]]
   [:workflow/concurrency {:optional true} [:map {:closed false}]]
   [:workflow/env  {:optional true} [:map {:closed false}]]
   [:workflow/jobs [:sequential WorkflowJob]]
   ;; Host-specific material a target may pass through verbatim, keyed by target
   ;; id. An escape hatch is required: a workflow language that cannot express
   ;; the last five percent gets abandoned for the YAML it replaced.
   [:workflow/raw  {:optional true} [:map-of keyword? :any]]
   [:enabled       {:optional true} :boolean]
   [:data          {:optional true} :map]])

;; ── Runtime source contract (knoxx) ──────────────────────────────────────────

(def SourceEmission
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:event/shape {:optional true} [:map {:closed false}]]
    [:event/payload-schema {:optional true} :any]
    [:description {:optional true} string?]]])

(def SourceListener
  [:or keyword?
   [:map {:closed false}
    [:event/type keyword?]
    [:description {:optional true} string?]]])

(def RuntimeSourceContract
  "Source resource provider. Event sources are driver-backed listeners: they
   name the source driver implemented in code, the actor identity that owns
   credentials, and driver events they listen to. Context sources may still
   hydrate context before a turn. This is distinct from :ingest_source, which
   indexes data."
  [:map {:closed false}
   [:contract/kind    [:= :source]]
   [:contract/id      ContractId]
   [:contract/type    {:optional true} [:or string? keyword?]]
   [:source/id        [:or string? keyword?]]
   [:source/type      {:optional true} [:or string? keyword?]]
   [:source/name      {:optional true} string?]
   [:source/enabled?  {:optional true} :boolean]
   [:source/driver    {:optional true} [:or string? keyword?]]
   [:source/actor     {:optional true} [:or string? keyword?]]
   [:source/listens   {:optional true} [:sequential SourceListener]]
   [:source/emits     {:optional true} [:sequential SourceEmission]]
   [:source/protocol  {:optional true} [:map {:closed false}]]
   [:source/provider  {:optional true} [:or string? keyword?]]
   [:source/hydration {:optional true} [:map {:closed false}]]
   [:source/render    {:optional true} [:map {:closed false}]]
   [:source/filters   {:optional true} [:map {:closed false}]]
   [:source/tools     {:optional true} [:sequential [:or string? keyword?]]]])

;; ── Model family contract (merged proxx + knoxx) ──────────────────────────────

(def ModelFamilyContract
  [:map {:closed false}
   [:model-family/id             [:or string? keyword?]]
   [:model-family/provider       {:optional true} keyword?]
   [:model-family/api            {:optional true} [:or string? keyword?]]
   [:model-family/compat         {:optional true} :map]
   [:model-family/prefixes       [:sequential string?]]
   [:model-family/allowlisted    {:optional true} :boolean]
   [:model-family/reasoning      {:optional true} :boolean]
   [:model-family/default-thinking {:optional true} keyword?]
   [:model-family/thinking-levels {:optional true} [:sequential keyword?]]
   [:model-family/context-window {:optional true} int?]
   [:model-family/max-tokens     {:optional true} int?]
   [:model-family/input          {:optional true} [:sequential keyword?]]])

;; ── Model contract (merged proxx + knoxx) ─────────────────────────────────────

(def ModelContract
  [:map {:closed false}
   [:model/id              string?]
   [:model/family          {:optional true} [:or keyword? string?]]
   ;; Legacy standalone contracts used :model-family/id as a reference.
   ;; Namespace resource files reserve :K/id for registration, so new
   ;; manifests use :model/family instead.
   [:model-family/id       {:optional true} string?]
   [:model/provider        {:optional true} keyword?]
   [:model/api             {:optional true} [:or string? keyword?]]
   [:model/compat          {:optional true} :map]
   [:model/label           {:optional true} string?]
   [:model/default         {:optional true} :boolean]
   [:model/allowlisted     {:optional true} :boolean]
   [:model/reasoning       {:optional true} :boolean]
   [:model/default-thinking {:optional true} keyword?]
   [:model/thinking-levels {:optional true} [:sequential keyword?]]
   [:model/context-window  {:optional true} int?]
   [:model/max-tokens      {:optional true} int?]
   [:model/input           {:optional true} [:sequential keyword?]]])

;; ── Ingest source contract (knoxx heritage) ───────────────────────────────────

(def IngestSourceContract
  [:map {:closed false}
   [:contract/kind     [:= :ingest_source]]
   [:contract/id       ContractId]
   [:contract/type     {:optional true} [:or string? keyword?]]
   [:tenant/id         {:optional true} string?]
   [:source/id         {:optional true} [:or string? keyword?]]
   [:source/name       {:optional true} string?]
   [:source/enabled?   {:optional true} :boolean]
   [:source/driver     {:optional true} [:or string? keyword?]]
   [:source/config     {:optional true} [:map {:closed false}]]
   [:source/discovery  {:optional true} [:map {:closed false}]]
   [:source/schedule   {:optional true} [:map {:closed false}]]
   [:source/ingest     {:optional true} [:map {:closed false}]]
   [:source/sink       {:optional true} [:map {:closed false}]]
   [:source/semantic   {:optional true} [:map {:closed false}]]
   [:source/translation {:optional true} [:map {:closed false}]]
   [:source/projection {:optional true} [:map {:closed false}]]
   [:source/backpressure {:optional true} [:map {:closed false}]]])

;; ── MCP server contract (knoxx/sol heritage) ──────────────────────────────────

(def McpServerContract
  "MCP gateway server declared as data. Both :mcp-server/* and :mcp_server/*
   key spellings are tolerated (disk contracts use either)."
  [:map {:closed false}
   [:contract/kind {:optional true} [:or keyword? string?]]
   [:contract/id string?]
   [:mcp-server/id {:optional true} string?]
   [:mcp_server/id {:optional true} string?]
   [:mcp-server/transport {:optional true} [:or keyword? string?]]
   [:mcp_server/transport {:optional true} [:or keyword? string?]]
   [:mcp-server/url {:optional true} string?]
   [:mcp_server/url {:optional true} string?]
   [:enabled {:optional true} :boolean]])

;; ── Source-mode contract (knoxx/sol heritage) ─────────────────────────────────

(def SourceModeContract
  "Source-mode contracts document how event runtime source modes transform
   upstream source records into template context and runtime dispatch behavior."
  [:map {:closed false}
   [:contract/kind [:= :source-mode]]
   [:contract/id ContractId]
   [:source-mode/id {:optional true} keyword?]
   [:source/kind {:optional true} [:or keyword? string?]]
   [:source/mode {:optional true} [:or keyword? string?]]
   [:prompts {:optional true} [:map {:closed false}
                               [:system {:optional true} :any]
                               [:task {:optional true} :any]]]
   [:data {:optional true} [:map {:closed false}]]])

;; ── Runtime feature contract (knoxx/sol heritage) ─────────────────────────────

(def RuntimeFeatureContract
  "Non-agent runtime toggles (e.g. eta-mu extensions) managed as contract data
   instead of ad-hoc JSON state."
  [:map {:closed false}
   [:contract/kind [:= :runtime-feature]]
   [:contract/id string?]
   [:runtime-feature/id {:optional true} string?]
   [:runtime/owner {:optional true} [:or keyword? string?]]
   [:runtime/feature {:optional true} [:or keyword? string?]]
   [:eta-mu/extension {:optional true} [:or keyword? string?]]
   [:enabled {:optional true} :boolean]
   [:runtime/enabled {:optional true} :boolean]
   [:runtime/default-enabled {:optional true} :boolean]
   [:runtime/applies-to {:optional true} [:sequential [:map {:closed false}]]]
   [:runtime/config {:optional true} [:map {:closed false}]]])

;; ── CMS contract (knoxx/sol heritage) ─────────────────────────────────────────

(def CmsContract
  "Folder-backed visual CMS records. Domain payload (:blocks or :templates)
   stays top-level so existing CMS editor file readers remain compatible."
  [:map {:closed false}
   [:contract/id string?]
   [:contract/kind [:enum :cms-block-registry :cms-templates :cms-template-registry]]
   [:enabled {:optional true} :boolean]
   [:blocks {:optional true} [:map {:closed false}]]
   [:templates {:optional true} [:map {:closed false}]]])

;; ── Provider contract (new in v0.2.0) ─────────────────────────────────────────

(def ProviderContract
  "LLM provider gateway declared as data: identity, endpoint, API shape, auth
   mode. :auth/env names an environment VARIABLE, never a secret value —
   secrets do not belong in contract data."
  [:map {:closed false}
   [:provider/id keyword?]
   [:contract/kind {:optional true} [:= :provider]]
   [:contract/id {:optional true} ContractId]
   [:provider/label {:optional true} string?]
   [:provider/base-url {:optional true} string?]
   [:provider/api-shape {:optional true}
    [:enum :openai-chat :openai-responses :anthropic-messages]]
   [:provider/auth {:optional true}
    [:map {:closed false}
     [:auth/mode [:enum :none :bearer :api-key :basic]]
     [:auth/env {:optional true} string?]
     [:auth/header {:optional true} string?]]]
   [:provider/models-endpoint {:optional true} string?]
   [:provider/model-prefix-allowlist {:optional true} [:sequential string?]]
   [:enabled {:optional true} :boolean]])

;; ── Registry ──────────────────────────────────────────────────────────────────

(def registry
  "Complete schema registry. Keys match :contract/kind values."
  {;; Eval primitives
   :unified/eval-node          EvalNode
   :unified/policy             PolicyContract
   :unified/policy-match       PolicyMatch
   :unified/fulfillment-match  FulfillmentMatch

   ;; Agent runtime (knoxx)
   :agent        AgentContract
   :sub-agent    SubAgentContract
   :actor        ActorContract
   :role         RoleContract
   :capability   CapabilityContract

   ;; Policy engine (proxx + eta-mu)
   :policy       PolicyContract
   :policy-gate  PolicyGateContract
   :fulfillment  FulfillmentContract
   :strategy     StrategyContract

    ;; Orchestration (knoxx)
    :action       ActionContract
    :trigger      TriggerContract
   :store        StoreContract
   :namespace    NamespaceFile
   :generator    GeneratorContract
   :schedule     ScheduleContract
   :workflow     WorkflowContract
   :source       RuntimeSourceContract

   ;; Model catalog (merged)
   :model-family ModelFamilyContract
   :model        ModelContract

   ;; Provider gateway (v0.2.0)
   :provider     ProviderContract

   ;; Runtime surfaces (knoxx/sol)
   :mcp-server      McpServerContract
   :mcp_server      McpServerContract
   :source-mode     SourceModeContract
   :runtime-feature RuntimeFeatureContract
   :cms-block-registry    CmsContract
   :cms-templates         CmsContract
   :cms-template-registry CmsContract

   ;; Data ingestion (knoxx)
   :ingest_source IngestSourceContract})

;; ── Kind inference ────────────────────────────────────────────────────────────

(defn infer-contract-class
  "Infer the contract kind from a parsed EDN map.
   Returns a keyword matching a registry key, or :agent as fallback."
  [value]
  (cond
    (not (map? value))                        :agent
    (contains? value :contract/kind)          (let [k (:contract/kind value)]
                                                (cond
                                                  (keyword? k) k
                                                  (string? k)  (keyword k)
                                                  :else        :agent))
    (contains? value :actor/id)               :actor
    (contains? value :role/id)                :role
    (contains? value :cap/id)                 :capability
    (or (contains? value :mcp-server/id)
        (contains? value :mcp_server/id))     :mcp-server
    (contains? value :model/id)               :model
    (contains? value :model-family/id)        :model-family
    (contains? value :provider/id)            :provider
    (contains? value :generator/id)           :generator
    (contains? value :schedule/id)            :schedule
    (contains? value :workflow/id)            :workflow
    (contains? value :source-mode/id)         :source-mode
    (contains? value :runtime-feature/id)     :runtime-feature
    (contains? value :parent-agent)           :sub-agent
    (contains? value :contract/id)            :agent
    :else                                     :agent))

;; ── Public API ────────────────────────────────────────────────────────────────

(declare collect-humanized-errors)

(defn schema-for
  "Look up the Malli schema for a contract kind.
   Throws if the kind is unknown."
  [kind]
  (let [k (if (string? kind) (keyword kind) kind)]
    (or (get registry k)
        (throw (ex-info "Unknown contract kind"
                        {:kind kind :known (sort (keys registry))})))))

(defn validate
  "Validate a parsed contract map against its kind-specific schema.

   Returns:
   - {:ok true  :value value :errors []}
   - {:ok false :value value :errors [{:path [...] :message <text>} ...]}

   When contract-class is nil, the kind is inferred from the map."
  ([value]
   (validate nil value))
  ([contract-class value]
   (let [kind   (or contract-class (infer-contract-class value))
         schema (schema-for kind)
         ok?    (m/validate schema value)]
     (if ok?
       {:ok true :value value :errors []}
       (let [explained (m/explain schema value)
             errors    (->> (me/humanize explained)
                            (collect-humanized-errors [])
                            (mapv (fn [err] (update err :path #(mapv str %)))))]
         {:ok false :value value :errors errors})))))

(defn- collect-humanized-errors
  [prefix value]
  (cond
    (nil? value)       []
    (string? value)    [{:path prefix :message value}]
    (vector? value)    (mapcat #(collect-humanized-errors prefix %) value)
    (sequential? value) (mapcat #(collect-humanized-errors prefix %) value)
    (map? value)       (mapcat (fn [[k v]]
                                 (collect-humanized-errors (conj prefix (name k)) v))
                               value)
    :else              [{:path prefix :message (pr-str value)}]))

(defn assert!
  "Validate and throw on failure. Use at ingest boundaries."
  [contract-class value]
  (let [{:keys [ok errors]} (validate contract-class value)]
    (if ok
      value
      (throw (ex-info "Contract validation failed"
                      {:contract-class contract-class
                       :errors errors
                       :input value})))))

(defn coerce
  "Attempt to coerce a value through its schema's default-value-transformer.
   Returns the coerced value or nil on failure."
  [contract-class value]
  (let [schema (schema-for contract-class)]
    (try
      (let [coerced (m/coerce schema value (mt/default-value-transformer))]
        (when (m/validate schema coerced) coerced))
      (catch :default _ nil))))
