.PHONY: dev up test verify e2e diagrams diagrams-png diagrams-svg sonarqube-up sonar

# SQL Server only (e.g. host JVM with mvn spring-boot:run)
dev:
	docker compose up -d sqlserver

# Full stack: SQL Server + API :18080 + static web UI :8081 (nginx proxies /api → API)
up:
	docker compose up -d --build

test:
	mvn -B test

verify:
	mvn -B verify

e2e:
	bash scripts/e2e.sh

# SonarQube Community on http://localhost:9000 — create a token (My Account → Security), then:
#   export SONAR_LOGIN='squ_...'
#   make sonar
sonarqube-up:
	docker compose up -d sonarqube
	@echo "Wait until SonarQube is up: docker compose logs -f sonarqube (look for 'SonarQube is operational')"
	@echo "Then open http://localhost:9000 — default login is admin/admin on first boot"

sonar:
	@test -n "$$SONAR_LOGIN" || (echo "Set SONAR_LOGIN to a SonarQube user token (User → My Account → Security)"; exit 1)
	mvn -B -DskipTests sonar:sonar -Dsonar.login=$$SONAR_LOGIN

# --- PlantUML diagrams (use case + activity-with-swimlanes) --------------
PUML_FILES := plantuml/usecase.puml plantuml/activity-risk.puml

diagrams: diagrams-png diagrams-svg

diagrams-png:
	plantuml -tpng $(PUML_FILES)

diagrams-svg:
	plantuml -tsvg $(PUML_FILES)
