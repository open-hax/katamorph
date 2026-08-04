# Roadmap — katamorph's slice

> Hub: **[eta-mu/ROADMAP.md](https://github.com/open-hax/eta-mu/blob/main/ROADMAP.md)** — read that for the seam, the ownership
> table, and the sequencing rule. This file is only katamorph's slice.
> Last surveyed: 2026-08-04.

## What katamorph is, on this roadmap

**The canon.** Resource identity, shared contract vocabulary, validation, laws,
and small pure interpreters. One contract language, many interpreters.

It does **not** own file discovery, deployment, host SDK objects, model calls, or
storage connections. Consumers provide those effects.

## Why this repo is separate

Deliberate change control. Katamorph's shapes are load-bearing for every
interpreter, so changing one should be hard and reviewed rather than convenient.
That is enforced, not merely intended: **`eta-mu:contract-redefinition-guard`**
(done) fails a consumer's gate when it redefines a katamorph-owned schema name.

Becoming a git submodule of eta-mu later is cosmetic. The guard is the substance.

## What affects katamorph

- **`eta-mu:katamorph-canonical-cutover`** (P0, breakdown) — the spine. The
  finding that matters here: *the same contract schema set exists in 4+ places,
  none deferring to katamorph.* Extraction succeeded; adoption did not.
- **`eta-mu:katamorph-provider-contract`** (done) — `ProviderContract` added and
  tagged. Note the original gap: *provider was a field on models, not a contract
  kind.*
- **`eta-mu:capability-schema-reconciliation`** (**ready**) — reconcile muse's
  capability shape with katamorph's `CapabilityContract`. *"A capability is the
  primitive."* Anything downstream that groups capabilities is blocked on this,
  including knoxx's MCP permission groups.

## Known consumers and their state

| Consumer | State |
|---|---|
| sol | ✅ validates via `katamorph.schema`; local duplicate deleted |
| muse | dep declared in `deps.edn`, **required nowhere**; own `dsl/schema.cljc` |
| knoxx | **references katamorph nowhere**; consumes `open-hax.contract-runtime` |
| eta-mu | `contract_runtime_v2` is a fifth runtime |

## What katamorph should not do

- Grow a host, an I/O path, or a model call.
- Absorb a consumer's convenience helper because that consumer is the only user.
  If only one interpreter needs it, it is not canon yet.
