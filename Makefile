.PHONY: dev test verify e2e

dev:
	docker compose up -d sqlserver

test:
	mvn -B test

verify:
	mvn -B verify

e2e:
	bash scripts/e2e.sh
