# EDOT Android demo

This repository demonstrates the
[Elastic Distribution of OpenTelemetry Android](https://github.com/elastic/apm-agent-android) (EDOT
Android) in an end-to-end weather application. See the
[EDOT Android documentation](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/android)
for the full agent reference.

Choose a city, request its current weather through a local instrumented backend, and inspect the
complete distributed trace in Elastic. The demo also includes focused examples of manual spans,
logs, an intentional backend error, and an intentional Android crash.

## Table of contents

- [What you can observe](#what-you-can-observe)
- [Components](#components)
  - [Backend service](#backend-service)
  - [Android application](#android-application)
  - [Elastic Agent](#elastic-agent)
- [Prerequisites](#prerequisites)
- [Run the demo](#run-the-demo)
  - [1. Start the Elastic Stack](#1-start-the-elastic-stack)
  - [2. Start the instrumented backend](#2-start-the-instrumented-backend)
  - [3. Run the Android application](#3-run-the-android-application)
- [Inspect the data](#inspect-the-data)
- [Emulator and physical-device networking](#emulator-and-physical-device-networking)
- [License](#license)

## What you can observe

- Distributed traces from an Android user action through HTTP, the backend, and Open-Meteo.
- Custom spans and logs from the Android app.
- A backend error when New York is selected.
- A persisted crash report exported after the app is relaunched.

## Components

![components](assets/components.png)

### Backend service

A simple Spring Boot service that provides APIs for the application and helps showcasing the
[distributed tracing](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/android#distributed-tracing)
use case. It is instrumented by the EDOT Java runtime attach library, and its source is maintained
in [elastic/shared-otel-sdk-demo](https://github.com/elastic/shared-otel-sdk-demo/tree/main/backend).

### Android application

Located in the [app](app) module. The first screen has a dropdown list of city names and a button
that takes you to the second one, where you'll see the selected city's current temperature. If you
pick a non-European city on the first screen, you'll get an error from the (local) backend when you
head to the second screen. This is to demonstrate how network and backend errors are captured and
correlated. The floating action button intentionally crashes the app so you can also inspect Android
crash reporting in Kibana.

### Elastic Agent

The [Elastic Agent](https://www.elastic.co/docs/reference/fleet/elastic-agent-as-otel-collector)
provides the OTLP endpoint that receives telemetry from the Android application and backend service,
then forwards it to Elasticsearch for storage and analysis. In this demo, it is set up automatically
as part of [Step 1](#1-start-the-elastic-stack) via start-local.

## Prerequisites

- [Docker](https://www.docker.com/).
- An [Android emulator](https://developer.android.com/studio/run/emulator#get-started).
- On Microsoft Windows, use
  [Windows Subsystem for Linux (WSL)](https://learn.microsoft.com/en-us/windows/wsl/install).

> [!NOTE]
> The reason why an emulator is recommended is that the demo's endpoints point to local services
> through the emulator's host machine alias
> ([10.0.2.2](https://developer.android.com/studio/run/emulator-networking#networkaddresses)). If
> you wanted to use a real device instead, see
> [Emulator and physical-device networking](#emulator-and-physical-device-networking).

## Run the demo

### 1. Start the Elastic Stack

We use [start-local](https://github.com/elastic/start-local/) to spin up Elasticsearch, Kibana and
the Elastic Agent locally with a single command. In this setup, the Elastic Agent provides the OTLP
endpoint that receives telemetry from the Android application and backend service. Run this command
from the repository root:

```sh
curl -fsSL https://elastic.co/start-local | sh -s -- --edot
```

This creates an `elastic-start-local` folder and starts all three services. Once it finishes, the
OTLP endpoint is available at `http://localhost:4318`.

You don't need to configure the OTLP endpoint for this demo application, as it has already been
set [here](app/src/main/java/co/elastic/otel/android/demo/MyApp.kt).

You can stop and start the services later with the scripts in the `elastic-start-local` folder:

```sh
cd elastic-start-local
./stop.sh   # stop the services
./start.sh  # start them again
```

For more information on start-local, refer to
the [start-local documentation](https://github.com/elastic/start-local/).

### 2. Start the instrumented backend

We're going to use the `backend-manager` script, which will pull the pre-built
[backend](https://github.com/elastic/shared-otel-sdk-demo/tree/main/backend) Docker image from
`ghcr.io` and run it connected to the same network as the Elastic Agent.

Once the backend service is running, its endpoint will be `http://localhost:8080/v1/`.

You don't need to set it for this demo application, as it has already been
done [here](app/src/main/java/co/elastic/otel/android/demo/network/WeatherRestManager.kt). So, for
this demo application use case, once the backend service is running, you're ready to go to the
next step.

Execute the [backend-manager](backend-manager) script. You can do so by opening up
a terminal, navigating to this directory and running the following command:

```sh
./backend-manager start
```

To stop the backend:

```sh
./backend-manager stop
```

To stop the backend and remove the Docker image from your machine:

```sh
./backend-manager uninstall
```

### 3. Run the Android application

Open up this project with Android Studio
and [run the application](https://developer.android.com/studio/run) in
an Android Emulator. Once everything is running, navigate around in the app to generate
some load that we would like to observe in Elastic APM. So, select a city, click Next and repeat it
multiple times. To see the intentional error path, select New York in the Android app and tap
**Next**. The backend rejects that city on purpose, which gives you an error trace to inspect and
correlate with the Android-side request.

To demonstrate Android crash reporting, tap the floating crash button in the lower-right corner.
The app will close intentionally. Tap **Open app again**, or launch it again from Android Studio or
the emulator launcher, so the EDOT Android agent can export the buffered crash event.

## Inspect the data

After launching the app and navigating through it, open Kibana at http://localhost:5601 and log in
with username `elastic` and the password printed at the end of the start-local setup. You can also
find the password in `elastic-start-local/.env` (the `ES_LOCAL_PASSWORD` variable).

Useful service names:

- `weather-demo-app`
- `weather-demo-backend`

For a more detailed overview, take a look at how
to [Visualize telemetry](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/android/getting-started#visualize-telemetry)
in the docs.

## Emulator and physical-device networking

The Android Emulator reaches services on the host machine through `10.0.2.2`, so the checked-in
configuration works without changes. A physical device cannot use that address. To use one:

1. Change the OTLP export URL in
   [`MyApp.kt`](app/src/main/java/co/elastic/otel/android/demo/MyApp.kt) and the backend URL in
   [`WeatherRestManager.kt`](app/src/main/java/co/elastic/otel/android/demo/network/WeatherRestManager.kt)
   to the host machine's LAN address.
2. Ensure ports `4318` (Elastic Agent OTLP) and `8080` (backend) are reachable through the host
   firewall.
3. Keep the device and host on the same network.

The app permits clear-text traffic because all demo endpoints are local HTTP services. Do not carry
`android:usesCleartextTraffic="true"` into a production application.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
