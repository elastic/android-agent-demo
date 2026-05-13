# CI E2E tests

These scripts are intended for CI and maintainer troubleshooting. The user-facing demo setup is
documented in the repository root `README.md`.

## What the test validates

The CI workflow uses `make -C .github/scripts/e2e-test start-env` to start Elasticsearch, the EDOT
Collector, and the backend service. It then uses `make -C .github/scripts/e2e-test run-tests`
inside an Android emulator runner.

The test path validates that:

* The Android release APK builds successfully, including the minified R8 path.
* The APK installs and launches on the emulator.
* A backend request to `GET /v1/forecast?city=Berlin` produces backend telemetry.
* The test triggers the app's intentional crash action, relaunches the app, and validates that
  crash telemetry is exported.
* Elasticsearch receives telemetry created during the current test run for:
  * The Android service `weather-demo-app`.
  * The startup span `Creating app`.
  * The startup log `During app creation`.
  * The crash event `device.crash`, including `exception.type` and `exception.stacktrace`.
  * The backend service `weather-backend`.

## Failure artifacts

The test stores diagnostics under `build/e2e/`:

* `logcat.txt`
* `app_span.json`
* `app_log.json`
* `backend_span.json`
* `crash_event.json`

On failure, the script also prints running Docker containers, recent `weather-backend` logs, and
the last logcat lines to the CI log.
