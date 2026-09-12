# Namespace warning and npm lint verification

The run JSON preserves original timestamps, argv, exit codes and sandbox paths. Each same-named file under `logs/` with extension `.txt` is a byte-identical durable copy of its original log. Absolute paths in the JSON describe the historical launcher and are not new-checkout requirements.

The two `main-warning` records establish the unchanged source fix: 218 CLJS tests/647 assertions, 91 JVM tests/348 assertions, zero failures/errors, and zero lint/compiler warnings. The newer `review` records prove the npm-compatible alias: original `pnpm lint` exits 127 with pnpm absent from PATH; the new direct command completes with zero errors/warnings under the same PATH. Full verify then reruns lint, 218 CLJS tests, executable examples and the release library build successfully.

Standalone debug runners still print their existing optional source-map-support notice. It is preserved in the logs and is distinct from compiler warnings. No production source changed in the npm-compatibility successor; the prior JVM proof is identified as prior evidence.
