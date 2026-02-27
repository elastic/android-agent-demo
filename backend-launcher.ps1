$BaseDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

& "$BaseDir\gradlew.bat" "-p" "$BaseDir" ":backend:bootJar"

docker rm -f weather-backend 2>$null
docker build -t weather-backend "$BaseDir\backend"
docker run --name weather-backend `
  --network elastic-start-local_default `
  -p 8080:8080 `
  -e OTEL_SERVICE_NAME=weather-backend `
  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://edot-collector:4318 `
  weather-backend
