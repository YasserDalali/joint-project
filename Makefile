.PHONY: dev test verify e2e diagrams diagrams-png diagrams-svg

dev:
	docker compose up -d sqlserver

test:
	mvn -B test

verify:
	mvn -B verify

e2e:
	bash scripts/e2e.sh

# --- PlantUML diagrams (use case + activity-with-swimlanes) --------------
PUML_FILES := plantuml/usecase.puml plantuml/activity-risk.puml

diagrams: diagrams-png diagrams-svg

diagrams-png:
	plantuml -tpng $(PUML_FILES)

diagrams-svg:
	plantuml -tsvg $(PUML_FILES)
