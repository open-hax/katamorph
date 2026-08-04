# Π Handoff — 2026-08-04

**Tag:** `Π/2026-08-04/214331-305a5e4`
**Branch:** `main`
**Head (before):** `305a5e4`

## What Changed

- **README.md** — rewritten for clarity, portable contract language framing
- **deps.edn** — reorganized with `:dev` alias, ClojureScript + shadow-cljs dev deps
- **package.json** — version bump 0.1.0 → 0.2.0, new `examples` and `verify` scripts
- **shadow-cljs.edn** — switched to `:deps {:aliases [:dev]}`, added `:examples` build
- **src/cljs/katamorph/schema.cljs** — `ModelFamilyContract` loosens `:model-family/id` to `[:or string? keyword?]`, `ModelContract` adds `:model/family`
- **test/** — new `examples_test.cljs`, new `model-family-reference` and `validate-model-catalog-contracts` tests
- **examples/** — `hello-world.edn`, `host-runtime.edn`, `cljs/runner.cljs`
- **.gitignore** — new (`.clj-kondo/.cache/`, `.cpcache/`, `.shadow-cljs/`, `dist/`, `node_modules/`)
- **ROADMAP.md** — new

## Verification

| Check | Result |
|-------|--------|
| clj-kondo lint | 0 errors, 0 warnings |
| Test suite | 115 tests, 280 assertions, 0 failures |
| Examples run | hello-world + host-runtime validated |
| ESM lib build | clean |

## Concurrent Dirt

None detected. All owned paths staged.

## Secrets Scan

Clean — no secrets found in diff.
