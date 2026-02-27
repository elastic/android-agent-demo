#!/usr/bin/env bash

set -euo pipefail

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
  response=$(curl "${ES_LOCAL_URL}/$index/_search" -sS \
   -H "Authorization: ApiKey ${ES_LOCAL_API_KEY}" \
   -H "Content-Type: application/json" \
   -d "$query"
  )

  response=$(require "$response")

  local hits
  hits=$(echo "$response" | jq -r '.hits.total.value')

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
    result=$(es_search "$index" "$query")
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

  assert_equals "weather-backend" "$service_name"
}

# Main execution
ES_LOCAL_URL=$1
ES_LOCAL_API_KEY=$2
current_dir=$(pwd)
repo_root="${current_dir%/.github*}"

# Connectivity diagnostics
echo "=== Connectivity diagnostics ==="
echo "Checking EDOT Collector port 4318 from host..."
nc -z localhost 4318 && echo "  Host -> localhost:4318: OK" || echo "  Host -> localhost:4318: FAILED"
echo "Checking backend port 8080 from host..."
nc -z localhost 8080 && echo "  Host -> localhost:8080: OK" || echo "  Host -> localhost:8080: FAILED"
echo "Docker containers:"
docker ps --format '  {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null || true
echo "=== End diagnostics ==="

launch_app "$repo_root"

# Trigger a backend request to generate backend telemetry
echo "Triggering backend request..."
curl -sS --retry 5 --retry-connrefused --retry-delay 2 "http://localhost:8080/v1/forecast?city=Berlin" > /dev/null

echo "Waiting for telemetry data to be indexed..."

app_span_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}},{"term":{"name":{"value":"Creating app"}}}]}}}'
app_log_query='{"query":{"bool":{"filter":[{"term":{"service.name":{"value":"weather-demo-app"}}},{"match":{"body.text":"During app creation"}}]}}}'
backend_span_query='{"query":{"term":{"service.name":{"value":"weather-backend"}}}}'

app_span=$(es_wait_for_item "traces-*" "$app_span_query" "Android app spans")
app_log=$(es_wait_for_item "logs-*" "$app_log_query" "Android app logs")
backend_span=$(es_wait_for_item "traces-*" "$backend_span_query" "Backend spans")

# Storing ES responses
es_build_dir="$repo_root/build/e2e"
mkdir -p "$es_build_dir"
echo "$app_span" > "$es_build_dir/app_span.json"
echo "$app_log" > "$es_build_dir/app_log.json"
echo "$backend_span" > "$es_build_dir/backend_span.json"

# Validate data was found
failed=0
if [ -z "$app_span" ]; then
  echo "FAIL: No Android app spans found in ES within timeout"
  failed=1
fi
if [ -z "$app_log" ]; then
  echo "FAIL: No Android app logs found in ES within timeout"
  failed=1
fi
if [ -z "$backend_span" ]; then
  echo "FAIL: No backend spans found in ES within timeout"
  failed=1
fi
if [ "$failed" -eq 1 ]; then
  exit 1
fi

# Validate Android app telemetry
echo "Validating Android app span..."
validate_app_span "$app_span"
echo "Validating Android app log..."
validate_app_log "$app_log"
# Validate backend telemetry
echo "Validating backend span..."
validate_backend_span "$backend_span"

echo "E2E tests succeeded"
