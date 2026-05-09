# FinRisk API

Spring Boot 3 + JDBC/SQL Server implementation of the OpenAPI contract in `openapi.yaml`, aligned with `uml.md`.

## Prerequisites

- Docker + Docker Compose
- JDK 21 (`JAVA_HOME` recommended)
- Maven 3.9+
- `jq` (for `scripts/e2e-happy-path.sh`)

## Configure credentials

Copy `.env.example` to `.env` and set `SA_PASSWORD` (and optional JDBC overrides).

The bundled SQL Server container authenticates the app/tests using the `sa` login (`DB_USER=sa`, `DB_PASSWORD` defaults to `SA_PASSWORD` in `.env.example`).

## Database (Phase 1 smoke)

```bash
Run the API locally
```

```bash
export $(grep -v '^#' .env | xargs)   # or `set -a && source .env && set +a`
mvn spring-boot:run
```

Health (local `mvn spring-boot:run`): `GET http://localhost:8080/actuator/health`

Docker Compose maps the container to host port **18080** → `GET http://localhost:18080/actuator/health`

## Tests

```bash
export SA_PASSWORD=... # needed for FinRiskDB_Test integration tests
mvn test        # Surefire (*Test.java)
mvn verify      # Surefire + Failsafe (*IT.java)
```

## End-to-end runner

Runs migrations, `mvn test`, `mvn verify`, seeds data, builds/runs the Docker image, then executes `scripts/e2e-happy-path.sh`.

```bash
export SA_PASSWORD=...
bash scripts/e2e.sh
```

Convenience targets:

```bash
make dev     # docker compose up sqlserver
make test    # mvn test
make verify  # mvn verify
make e2e     # scripts/e2e.sh
```

