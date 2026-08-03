#!/usr/bin/env bash

set -euo pipefail

logcat_pid=""
logcat_file=""
crash_logcat_file=""

require() {
  local value="$1"
  if [ -z "$value" ]; then
    exit 1
  fi
  echo "$value"
}

es_search() {
  local index="$1"
  local query="$2"
  local response
  if ! response=$(curl "${ES_LOCAL_URL}/$index/_search" -sS \
    -H "Authorization: ApiKey ${ES_LOCAL_API_KEY}" \
    -H "Content-Type: application/json" \
    -d "$query"
  ); then
    echo "Failed to query Elasticsearch index pattern '${index}'" >&2
    return 1
  fi

  if [ -z "$response" ]; then
    echo "Elasticsearch returned an empty response for '${index}'" >&2
    return 1
  fi

  local hits
  hits=$(echo "$response" | jq -r '.hits.total.value // empty')
  if [ -z "$hits" ]; then
    echo "Elasticsearch response did not include a hit count for '${index}'" >&2
    return 1
  fi

  if [ "$hits" -lt 1 ]; then
    echo ""
    return
  fi

  echo "$response" | jq -r '.hits.hits[0]'
}

es_wait_for_item() {
  local index="$1"
  local query="$2"
  local label="$3"
  local timeout=120
  local interval=5
  local elapsed=0

  while [ $elapsed -lt $timeout ]; do
    local result
    if ! result=$(es_search "$index" "$query"); then
      return 1
    fi
    if [ -n "$result" ]; then
      echo "$result"
      return 0
    fi
    echo "  ${elapsed}s/${timeout}s — waiting for ${label}..." >&2
    sleep "$interval"
    elapsed=$((elapsed + interval))
  done

  echo ""
}

launch_app() {
  local app_dir="$1"
  "$app_dir/gradlew" -p "$app_dir" :app:assembleRelease
  adb install -r "$app_dir"/app/build/outputs/apk/release/app-release.apk
  adb shell am start -n co.elastic.otel.android.demo/.ui.MainActivity
}

assert_equals() {
  local expected="$1"
  local actual="$2"
  if [ "$expected" != "$actual" ]; then
    echo "Expected value: '$expected' not matching actual value: '$actual'"
    exit 1
  fi
}

assert_not_empty() {
  local value="$1"
  local message="$2"
  if [ -z "$value" ]; then
    echo "$message"
    exit 1
  fi
}

cleanup_logcat() {
  if [ -n "${logcat_pid:-}" ]; then
    kill "$logcat_pid" 2>/dev/null || true
    wait "$logcat_pid" 2>/dev/null || true
    logcat_pid=""
  fi
}

tap_node() {
  local attribute="$1"
  local value="$2"

  adb shell uiautomator dump /sdcard/window.xml >/dev/null

  local bounds
  bounds=$(python3 - "$attribute" "$value" <<'PY'
import subprocess
import sys
import xml.etree.ElementTree as ET

attribute, value = sys.argv[1], sys.argv[2]
xml = subprocess.check_output(["adb", "exec-out", "cat", "/sdcard/window.xml"]).decode(
    "utf-8", "ignore"
)
root = ET.fromstring(xml)
for node in root.iter("node"):
    if node.attrib.get(attribute) == value:
        print(node.attrib["bounds"])
        sys.exit(0)
sys.exit(1)
PY
)

  local coords
  coords=$(echo "$bounds" | sed -E 's/\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]/\1 \2 \3 \4/')
  read -r left top right bottom <<< "$coords"
  adb shell input tap "$(((left + right) / 2))" "$(((top + bottom) / 2))"
}

tap_crash_button() {
  tap_node "content-desc" "Crash the app to demonstrate crash reporting"
}

tap_next_button() {
  tap_node "resource-id" "co.elastic.otel.android.demo:id/button_first"
}

trigger_crash_and_relaunch_app() {
  echo "Triggering intentional app crash..."
  cleanup_logcat
  adb logcat -c 2>/dev/null || true

  tap_crash_button

  # The crash window gets its own file so the logcat streamed since app launch stays available
  # for diagnostics
  crash_logcat_file="${logcat_file%.txt}_crash.txt"
  for i in $(seq 1 10); do
    sleep 2
    adb logcat -d > "$crash_logcat_file" 2>&1
    if grep -q "AndroidRuntime.*FATAL EXCEPTION" "$crash_logcat_file"; then
      echo "Crash detected in logcat"
      break
    fi
    echo "  waiting for crash in logcat... ($i/10)" >&2
  done

  if ! grep -q "AndroidRuntime.*FATAL EXCEPTION" "$crash_logcat_file"; then
    echo "FAIL: No intentional crash found in logcat"
    exit 1
  fi

  echo "Re-launching app to export buffered crash data..."
  sleep 3
  adb shell am force-stop co.elastic.otel.android.demo 2>/dev/null || true
  sleep 2
  adb shell am start -n co.elastic.otel.android.demo/.ui.MainActivity
}

print_failure_diagnostics() {
  echo "=== Failure diagnostics ===" >&2
  docker ps --format '  {{.Names}}\t{{.Status}}\t{{.Ports}}' >&2 2>/dev/null || true
  echo "--- Last 200 android-weather-backend log lines ---" >&2
  docker logs --tail 200 android-weather-backend >&2 2>/dev/null || true
  if [ -n "${logcat_file:-}" ] && [ -f "$logcat_file" ]; then
    echo "--- Last 200 logcat lines ---" >&2
    tail -n 200 "$logcat_file" >&2 || true
  fi
  if [ -n "${crash_logcat_file:-}" ] && [ -f "$crash_logcat_file" ]; then
    echo "--- Last 200 crash-window logcat lines ---" >&2
    tail -n 200 "$crash_logcat_file" >&2 || true
  fi
  echo "=== End failure diagnostics ===" >&2
}

on_exit() {
  local exit_code=$?
  cleanup_logcat
  if [ "$exit_code" -ne 0 ]; then
    print_failure_diagnostics
  fi
  exit "$exit_code"
}

trap on_exit EXIT

validate_app_span() {
  local span
  span=$(require "$1")
  local expected_span_name="Creating app"
  local expected_agent_name="android/java"
  local span_name
  span_name=$(echo "$span" | jq -r '._source.name')
  local agent_name
  agent_name=$(echo "$span" | jq -r '._source.resource.attributes."agent.name"')

  assert_equals "$expected_span_name" "$span_name"
  assert_equals "$expected_agent_name" "$agent_name"
}

validate_app_log() {
  local log
  log=$(require "$1")
  local expected_body_text="During app creation"
  local body_text
  body_text=$(echo "$log" | jq -r '._source.body.text')

  assert_equals "$expected_body_text" "$body_text"
}

validate_backend_span() {
  local span
  span=$(require "$1")
  local service_name
  service_name=$(echo "$span" | jq -r '._source.resource.attributes."service.name"')

  assert_equals "weather-demo-backend" "$service_name"
}

validate_trace_correlation() {
  local app_http_span="$1"
  local backend_span="$2"
  local app_trace_id
  app_trace_id=$(echo "$app_http_span" | jq -r '._source.trace_id // ._source."trace.id" // empty')
  local backend_trace_id
  backend_trace_id=$(echo "$backend_span" | jq -r '._source.trace_id // ._source."trace.id" // empty')

  assert_not_empty "$app_trace_id" "App HTTP span has no trace id"
  assert_not_empty "$backend_trace_id" "Backend span has no trace id"
  assert_equals "$app_trace_id" "$backend_trace_id"
}

validate_crash_event() {
  local crash_event
  crash_event=$(require "$1")
  local event_name
  event_name=$(echo "$crash_event" | jq -r '._source.attributes."otel.event.name" // ._source.event_name // empty')
  local exception_type
  exception_type=$(echo "$crash_event" | jq -r '._source.attributes."exception.type" // empty')
  local stacktrace
  stacktrace=$(echo "$crash_event" | jq -r '._source.attributes."exception.stacktrace" // empty')

  assert_equals "device.crash" "$event_name"
  assert_equals "java.lang.RuntimeException" "$exception_type"
  assert_not_empty "$stacktrace" "Crash event has no exception.stacktrace field"
}

# Main execution
if [ "$#" -ne 2 ] || [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
  echo "Usage: $0 <ES_LOCAL_URL> <ES_LOCAL_API_KEY>" >&2
  exit 1
fi

ES_LOCAL_URL=$1
ES_LOCAL_API_KEY=$2
current_dir=$(pwd)
repo_root="${current_dir%/.github*}"

# Connectivity diagnostics
echo "=== Connectivity diagnostics ==="
nc -z localhost 4318 && echo "  localhost:4318 (collector): OK" || echo "  localhost:4318 (collector): FAILED"
nc -z localhost 8080 && echo "  localhost:8080 (backend): OK" || echo "  localhost:8080 (backend): FAILED"
docker ps --format '  {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null || true
echo "=== End diagnostics ==="

launch_app "$repo_root"

# Capture logcat in the background for diagnostics
logcat_file="$repo_root/build/e2e/logcat.txt"
mkdir -p "$(dirname "$logcat_file")"
adb logcat -d > "$logcat_file" 2>&1 || true
adb logcat -c 2>/dev/null || true
adb logcat > "$logcat_file" 2>&1 &
logcat_pid=$!

# Give the app a moment to initialize and export telemetry
sleep 10

# Navigate to the forecast screen like a real user, which makes the app request the
# forecast from the backend and produces a distributed trace across both services
echo "Navigating to the forecast screen..."
tap_next_button
sleep 5

echo "Waiting for telemetry data to be indexed..."

app_span_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}},{"term":{"name":{"value":"Creating app"}}}]}}}'
app_log_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}},{"match":{"body.text":"During app creation"}}]}}}'
backend_span_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-backend"}}},{"term":{"name":{"value":"GET /v1/forecast"}}}]}}}'
crash_event_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}}],"should":[{"term":{"event_name":{"value":"device.crash"}}},{"term":{"attributes.otel.event.name":{"value":"device.crash"}}}],"minimum_should_match":1}}}'

app_span=$(es_wait_for_item "traces-*" "$app_span_query" "Android app startup span")
app_log=$(es_wait_for_item "logs-*" "$app_log_query" "Android app startup log")
backend_span=$(es_wait_for_item "traces-*" "$backend_span_query" "Backend forecast span")

# The forecast request is auto-instrumented by the EDOT OkHttp instrumentation, so the app-side
# span of the distributed trace is looked up by the backend span's trace id
app_http_span=""
if [ -n "$backend_span" ]; then
  backend_trace_id=$(echo "$backend_span" | jq -r '._source.trace_id // ._source."trace.id" // empty')
  app_http_span_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}}],"should":[{"term":{"trace_id":{"value":"'"$backend_trace_id"'"}}},{"term":{"trace.id":{"value":"'"$backend_trace_id"'"}}}],"minimum_should_match":1}}}'
  app_http_span=$(es_wait_for_item "traces-*" "$app_http_span_query" "Android span in the backend's trace")
fi

trigger_crash_and_relaunch_app
crash_event=$(es_wait_for_item "logs-*" "$crash_event_query" "Android app crash event")

# Storing ES responses
es_build_dir="$repo_root/build/e2e"
mkdir -p "$es_build_dir"
echo "$app_span" > "$es_build_dir/app_span.json"
echo "$app_log" > "$es_build_dir/app_log.json"
echo "$app_http_span" > "$es_build_dir/app_http_span.json"
echo "$backend_span" > "$es_build_dir/backend_span.json"
echo "$crash_event" > "$es_build_dir/crash_event.json"

# Validate data was found
failed=0
if [ -z "$app_span" ]; then
  echo "FAIL: No Android app startup span found in ES within timeout"
  failed=1
fi
if [ -z "$app_log" ]; then
  echo "FAIL: No Android app startup log found in ES within timeout"
  failed=1
fi
if [ -z "$backend_span" ]; then
  echo "FAIL: No backend span found in ES within timeout"
  failed=1
fi
if [ -z "$app_http_span" ]; then
  echo "FAIL: No Android app span found in the backend's trace within timeout"
  failed=1
fi
if [ -z "$crash_event" ]; then
  echo "FAIL: No Android app crash event found in ES within timeout"
  failed=1
fi
if [ "$failed" -eq 1 ]; then
  exit 1
fi

# Validate Android app telemetry
echo "Validating Android app startup span..."
validate_app_span "$app_span"
echo "Validating Android app startup log..."
validate_app_log "$app_log"
# Validate backend telemetry
echo "Validating backend span..."
validate_backend_span "$backend_span"
# Validate distributed trace correlation (the app's auto-instrumented HTTP span and the backend
# span share the same trace.id)
echo "Validating trace correlation..."
validate_trace_correlation "$app_http_span" "$backend_span"
echo "Validating Android app crash event..."
validate_crash_event "$crash_event"

echo "E2E tests succeeded"
