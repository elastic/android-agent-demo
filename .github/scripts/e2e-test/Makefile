MAKEFLAGS += --no-print-directory
.SHELLFLAGS = -euc
SHELL = /bin/bash

repo_root := $(shell cd $(dir $(lastword $(MAKEFILE_LIST))) && git rev-parse --show-toplevel)

elastic-start-local:  ## Start ES + Kibana + EDOT Collector locally
	curl -fsSL https://elastic.co/start-local | sh -s -- --edot

.PHONY: collector-host-network
collector-host-network: elastic-start-local  ## Recreate EDOT Collector with host networking
	collector_image=$$(docker inspect edot-collector --format '{{.Config.Image}}'); \
	docker cp edot-collector:/etc/otelcol-contrib/config.yaml edot-collector-config.yaml; \
	sed 's/elasticsearch:9200/localhost:9200/g' edot-collector-config.yaml > edot-collector-config-host.yaml; \
	docker stop edot-collector; \
	docker rm edot-collector; \
	docker run -d --name edot-collector \
	  --network=host \
	  -v "$$PWD/edot-collector-config-host.yaml:/etc/otelcol-contrib/config.yaml" \
	  "$$collector_image" \
	  --config=/etc/otelcol-contrib/config.yaml

.PHONY: start-backend
start-backend: collector-host-network  ## Build and start the backend service in Docker
	$(repo_root)/gradlew -p $(repo_root) :backend:bootJar
	docker rm -f weather-backend 2>/dev/null || true
	docker build -t weather-backend $(repo_root)/backend
	docker run -d --name weather-backend \
	  --network=host \
	  -e OTEL_SERVICE_NAME=weather-backend \
	  -e OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
	  weather-backend
	./await_port.sh 8080 120

.PHONY: start-env
start-env: collector-host-network start-backend  ## Start the full environment

.PHONY: run-tests
run-tests:  ## Run E2E tests
	. elastic-start-local/.env; \
	./e2e_test.sh "$$ES_LOCAL_URL" "$$ES_LOCAL_API_KEY"
