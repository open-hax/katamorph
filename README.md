# Katamorph

> **Roadmap:** [`ROADMAP.md`](ROADMAP.md) — this repo's slice. The hub, with the
> seam, ownership table and sequencing rule, is [eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md).

Katamorph is a portable contract language and interpreter toolkit for
data-described runtimes.

Put identities, capabilities, policies, triggers, actions, sources, stores,
providers, and agent configuration in EDN. Katamorph gives those declarations
a common namespace grammar, validates their boundaries with Malli, and provides
small interpreters and laws that applications can compose.

The intended direction is simple: describe as much behavior as practical in
contracts, and keep deployment-specific effects behind injected functions and
protocols. An in-process agent, a deployed Sol service, several coding-agent
hosts, and a Kanban workflow should be able to share the language without
sharing one monolithic runtime.

Katamorph does not currently read an application directory, run an agent, call
an LLM, or own a database connection. Consumers provide those effects.

## Try it

Prerequisites are Node.js, pnpm, the Clojure CLI, and clj-kondo.

```bash
pnpm install
pnpm examples
pnpm verify
```

`pnpm examples` validates both checked-in EDN manifests and executes the
hello-world action:

```text
Validated Katamorph examples:
 - examples/hello-world.edn :hello 2 [:action :trigger]
 - examples/host-runtime.edn :open-hax 5 [:provider :model-family :model :mcp-server :agent]
Executed hello-world: {:message "Hello, Katamorph!", :handled-by :demo/greet}
```

The first example is deliberately small:

```clojure
{:namespace :hello
 :resources
 [{:action/id :greet
   :action/kind :demo/greet
   :action/with {:greeting "Hello"}}

  {:trigger/id :greeting-requested
   :trigger/events [:hello/requested]
   :trigger/action :hello/greet
   :trigger/with {:name "Katamorph"}}]}
```

Katamorph expands `:greet` to the stable identity `:hello/greet`, validates the
action and trigger projections, resolves the trigger's action reference, and
dispatches `:demo/greet` through an application-supplied handler. See
[`examples/cljs/katamorph/examples/runner.cljs`](examples/cljs/katamorph/examples/runner.cljs)
for the complete executable adapter.

[`examples/host-runtime.edn`](examples/host-runtime.edn) shows the larger
upstream shape: a provider, model family, model, MCP server, and agent declared
together without committing them to OpenCode, Claude, Codex, or Sol syntax.
Muse is the first intended host interpreter: it assembles these resources and
projects them into a selected runtime's equivalent vocabulary.

## The model

A namespace resource file has one `:namespace` and a sequence of resource
maps. Four grammar rules keep the maps composable:

- Registration: `:K/id` registers a resource of kind `K`. Namespace plus local
  id becomes its stable qualified identity.
- Composite entries: one map may register several kinds. Each interpreter
  reads only the keys in its own `:K/*` facet.
- Anonymous facets: `:K/*` keys without `:K/id` describe behavior owned by
  another registered resource. They are interpreted in place, not registered.
- References: relationships live under the owning kind's namespace, such as
  `:trigger/action` or `:role/capabilities`.

This creates a narrow execution boundary:

```text
EDN namespace file
  -> manifest projections with qualified identities
  -> kind-specific schema validation
  -> pure interpreter or law
  -> consumer-injected effects
```

Contracts say what must be true. Resources give those contracts identities and
relationships. Interpreters turn selected declarations into decisions.
Applications supply time, files, networks, databases, model calls, and other
effects.

Calling Katamorph a linter is useful but incomplete. It is the admissibility
and shared-vocabulary layer: it validates declarations, assigns stable
identities, and supplies laws or small pure interpreters where evaluation is
portable. It does not choose a host. Muse performs that Keryx role by
assembling lawful declarations and translating them to host artifacts. A
translation must either preserve the declaration's meaning or report an
incompatibility; silently dropping unsupported semantics is a bug.

## Use it from ClojureScript

Katamorph v0.2.0 is consumed as an immutable Git dependency. The package is
private on npm; `package.json` exists for standalone build tooling, not as the
canonical distribution channel.

```clojure
{:deps
 {io.github.open-hax/katamorph
  {:git/tag "v0.2.0"
   :git/sha "305a5e49d834aca27566f739e8510f6b409fda78"}}}
```

With Shadow CLJS, use the `deps.edn` classpath:

```clojure
{:deps true
 :builds
 {:app
  {:target :node-script
   :main my.app/main
   :output-to "dist/app.cjs"}}}
```

Then require only the seams the application owns:

```clojure
(:require [katamorph.action.interpreter :as action]
          [katamorph.manifest :as manifest]
          [katamorph.schema :as schema])
```

Do not add a sibling `../katamorph/src/cljs` source path. A pinned Git
dependency gives every consumer the same contract definitions.

## Public seams

`katamorph.manifest` is the front door for already-parsed EDN. Its core
functions are `namespace-file?`, `namespace-file-definitions`, `qualified-id`,
`qualified-id-str`, `entry-kinds`, `facet-kinds`, and `anonymous-facets`.
Katamorph intentionally leaves file discovery and reading to the consumer.

`katamorph.schema` contains the kind registry and the `schema-for`,
`infer-contract-class`, `validate`, `assert!`, and `coerce` APIs. The registry
currently covers agents, sub-agents, actors, roles, capabilities, actions,
triggers, stores, policies, fulfillment, schedules, generators, runtime and
ingest sources, models, providers, MCP servers, and selected runtime surfaces.

`katamorph.action.interpreter/execute!` resolves inline action facets,
registered handlers, or referenced EDN action resources. Its dependencies are
injected under `:contract-runtime/deps`; the hello-world runner is the smallest
working example of that boundary.

`katamorph.store.protocol` defines the promise-returning `IStore` boundary.
`katamorph.store.memory` is the included process-local implementation, and
`katamorph.store.registry` resolves declared stores. No MongoDB adapter ships
in this repository; a database implementation belongs behind `IStore`.

`katamorph.policy.*` evaluates the policy, fulfillment, and tool-gate
contracts. `katamorph.{condition,driver,filter}.registry` supplies small
extension registries. `katamorph.agent.*` contains runtime-neutral turn
helpers for reasoning, streamed text, tool lifecycle, context, and guards.

The ESM build currently exports only the manifest grammar helpers from
`dist/lib`. The ClojureScript namespaces are the complete public surface.

## What is solid, and what is not

| Area | Current state |
| --- | --- |
| Manifest grammar and qualified identities | Implemented and tested |
| Kind-specific Malli validation | Implemented and tested |
| Action-resource resolution | Implemented and exercised by hello-world |
| Store protocol and memory store | Implemented and law-tested |
| Policy evaluation, fulfillment, and gates | Implemented and tested |
| Agent turn helpers | Implemented; core helpers are tested |
| Driver, condition, filter, and resource registries | Implemented extension seams |
| ESM consumption | Manifest helpers only |
| Workflow language | Planned, not implemented |

There is a `:workflow` resource-registry placeholder, but there is not yet a
workflow schema, transition language, or interpreter. The goal is a reusable
workflow DSL whose state-machine semantics can implement the Kanban workflow;
it is not to bake the current Kanban finite-state machine directly into
Katamorph.

## Growing Katamorph

Prefer a contract when behavior can be expressed as portable data and checked
without performing I/O. A useful addition normally includes:

1. a kind-specific schema and stable identity;
2. manifest registration when the kind is discoverable;
3. a small interpreter, protocol, or law when data alone is insufficient;
4. an executable example and regression tests; and
5. an adapter in the consuming runtime for effects.

For a consuming runtime, move reusable schemas, policies, resource grammar,
and pure interpretation here first. Keep HTTP handlers, storage connections,
model-provider calls, UI wiring, and host translation in runtime or Muse
adapters. Stabilize those upstream seams before attempting a downstream
application migration.

## Commands

```bash
pnpm lint:kondo  # static analysis
pnpm test        # 115 tests, including the checked-in examples
pnpm examples    # validate both manifests and execute hello-world
pnpm build       # optimized ESM manifest-helper library
pnpm verify      # lint + test + examples + build
```
