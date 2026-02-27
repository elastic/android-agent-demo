MAKEFLAGS += --no-print-directory
.SHELLFLAGS = -euc
SHELL = /bin/bash

repo_root := $(shell cd $(dir $(lastword $(MAKEFILE_LIST))) && git rev-parse --show-toplevel)

elastic-start-local:  ## Start ES + Kibana + EDOT Collector locally
	curl -fsSL https://elastic.co/start-local | sh -s -- --edot

.PHONY: start-backend
start-backend: elastic-start-local  ## Build and start the backend service in Docker
	$(repo_root)/gradlew -p "$(repo_root)" :backend:bootJar
	docker build -t weather-backend "$(repo_root)/backend"
	docker run -d --name weather-backend \
		--network elastic-start-local_default \
		-p 8080:8080 \
		-e OTEL_SERVICE_NAME=weather-backend \
		-e OTEL_EXPORTER_OTLP_ENDPOINT=http://edot-collector:4318 \
		weather-backend
	./await_port.sh 8080 120

.PHONY: start-env
start-env: elastic-start-local start-backend  ## Start the full environment

.PHONY: run-tests
run-tests:  ## Run E2E tests
	. elastic-start-local/.env; \
	./e2e_test.sh "$$ES_LOCAL_URL" "$$ES_LOCAL_API_KEY"
