# EDOT Collector launcher

> [!NOTE]
> This guide explains how to run a standalone EDOT Collector for testing purposes. If you're planning to use it to [try the demo app](..) out, take a look at the demo app's [README](..#how-to-run) instead.

The [EDOT Collector](https://www.elastic.co/docs/reference/opentelemetry/edot-collector/) receives telemetry data from the [EDOT Android agent](https://github.com/elastic/apm-agent-android), processes it, and forwards it to Elasticsearch.

---

## How to run

This launcher allows running an EDOT Collector for testing purposes. Refer to
the [EDOT Collector](https://www.elastic.co/docs/reference/opentelemetry/edot-collector/) docs for
more information.

> [!IMPORTANT]
> Do not use the EDOT Collector created by this launcher in production.

### Prerequisites

* An Elasticsearch + Kibana setup with version `9.2.0` or higher. If you don't have one yet, you
  can
  quickly create it with [start-local](https://github.com/elastic/start-local/).
* An Elasticsearch API Key. Take a look at how to create
  one [here](https://www.elastic.co/docs/deploy-manage/api-keys/elasticsearch-api-keys#create-api-key).

### Step 1: Setting your Elasticsearch properties

You must set your Elasticsearch endpoint URL
and [API Key](https://www.elastic.co/docs/deploy-manage/api-keys/elasticsearch-api-keys#create-api-key)
into the [elasticsearch.properties](../elasticsearch.properties) file located in the root directory of this repo.

```properties
endpoint=YOUR_ELASTICSEARCH_ENDPOINT
api_key=YOUR_ELASTICSEARCH_API_KEY
```

Replace `YOUR_ELASTICSEARCH_ENDPOINT` and `YOUR_ELASTICSEARCH_API_KEY` with the respective values.

### Step 2: Launching the EDOT Collector

We're going to use the `edot-collector-launcher` script, located in the root directory of this repo, which will:

* Download the latest EDOT Collector build.
* Create
  a [configuration file](https://www.elastic.co/docs/reference/opentelemetry/edot-collector/config/default-config-standalone#gateway-mode)
  using the values from
  the [elasticsearch.properties](../elasticsearch.properties) file.
* Launch the EDOT Collector service and leave it running until manually stopped.

#### For Windows

Execute the [edot-collector-launcher.ps1](../edot-collector-launcher.ps1) script with PowerShell. You
can learn how to do so by taking a look
at [this guide](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_scripts#how-to-run-a-script).

#### For Linux and MacOS

Execute the [edot-collector-launcher](../edot-collector-launcher) script.

#### Example

```shell
# Make sure you're in the root dir of this repo
cd android-agent-demo
# Run the launcher
./edot-collector-launcher
```

---

## Sending data to the EDOT Collector

Once the EDOT Collector is running, its endpoint will be `http://localhost:4318`.

To use it with [EDOT Android](https://github.com/elastic/apm-agent-android), set it with the `setExportUrl(String)` config param mentioned in the [agent setup](https://www.elastic.co/docs/reference/opentelemetry/edot-sdks/android/getting-started#agent-setup). For this EDOT Collector there's no need to set an authentication method with `setExportAuthentication()`, as it's meant for testing purposes only.

> [!IMPORTANT]
> If you're using an Android emulator to reach the EDOT Collector within the same host machine, you must replace `localhost` by `10.0.2.2` in the endpoint, i.e. `http://10.0.2.2:4318`. Refer to [Android emulator networking](https://developer.android.com/studio/run/emulator-networking#networkaddresses) for more information.

Refer to
the [EDOT Collector](https://www.elastic.co/docs/reference/opentelemetry/edot-collector/) docs for
more information on how to set up an EDOT Collector with authentication for production environments.