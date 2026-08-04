# Continuous integration

This document covers the CI tooling for changing the demo. To just run the demo, see the
[README](../README.md).

## Workflow

The `ci` workflow ([workflows/ci.yml](workflows/ci.yml)) runs a change detector, two conditional
jobs, and a gatekeeper:

- **changes** — detects whether the pull request or push contains anything other than Markdown.
- **checks** — runs `./checks.sh` (Spotless formatting, compilation, and behavior-focused local
  unit tests).
- **e2e** — runs [scripts/e2e-test/e2e_test.sh](scripts/e2e-test/e2e_test.sh): starts
  Elasticsearch, Kibana, and the Elastic Agent via `start-local --edot`, starts the pre-built
  backend Docker image, builds the app in Release, installs it on an Android Emulator, exercises
  the telemetry and crash scenarios, and queries Elasticsearch to verify startup spans and logs, a
  distributed trace shared by the Android app and the backend (same `trace.id`), and a persisted
  crash report exported after relaunch. The job uploads `build/e2e/` diagnostics (logcat, ES
  responses). See [scripts/e2e-test/README.md](scripts/e2e-test/README.md) for local execution.
- **ci** — aggregates the results as the branch-protection gate. For Markdown-only changes, it
  succeeds after `changes` while the two code-related jobs are skipped.

Markdown-only changes (`**/*.md`) skip the code-related jobs, but the lightweight workflow still
runs so the required `ci` check reports success.

## Run the checks locally

```sh
./checks.sh
```

The script runs `./gradlew check`, which includes Spotless formatting enforcement, compilation, and
behavior-focused local unit tests. Fix formatting findings with:

```sh
./gradlew spotlessApply
```

## Build variants

- **Debug** is for interactive use in Android Studio.
- **Release** is what CI builds and installs in the end-to-end test, so the tested app matches a
  real production build: minified with R8, signed with the debug key. The E2E test uses
  `./gradlew :app:assembleRelease` then `adb install`.

## Dependencies

Renovate manages dependency versions in `gradle/libs.versions.toml` and the SHA-pinned GitHub
Actions. After a dependency version changes, run `./checks.sh` to confirm the update builds
cleanly.
