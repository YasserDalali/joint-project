# FinRisk API

Spring Boot 3 + JDBC/SQL Server implementation of the OpenAPI contract in `openapi.yaml`, aligned with `uml.md`.

## Prerequisites

- Docker + Docker Compose
- JDK 21 (`JAVA_HOME` recommended)
- Maven 3.9+
- Node.js 20+ (for the `finrisk-web` UI only)
- `jq` (for `scripts/e2e-happy-path.sh`)

## Configure credentials

```bash
cp .env.example .env
```

Edit `.env` and set `SA_PASSWORD` (and optional JDBC overrides). The SQL Server container and the app both use the `sa` login; `DB_PASSWORD` should match `SA_PASSWORD` unless you change the server configuration.

Load variables into your shell when running commands locally:

```bash
set -a && source .env && set +a
```

(On Windows PowerShell, set the same variables in your session or use a `.env` loader you prefer.)

## Recommended: React app + API in Docker

The `finrisk-web` dev server proxies `/api` to **`http://localhost:18080`** (the `finrisk-api` container). Do **not** set `VITE_API_BASE_URL` for local dev unless the API sends CORS for your UI origin (this project does not enable global CORS).

1. **Load env and start the stack** (SQL Server + API on host **18080**)

   ```bash
   set -a && source .env && set +a
   make up
   ```

   Equivalent: `docker compose up -d --build`. Use `make dev` only if you intend to run the JVM on the host with `mvn spring-boot:run` and **not** use the `finrisk-api` container.

2. **Migrations and seed** (first time, or after wiping the SQL Server volume)

   ```bash
   set -a && source .env && set +a
   bash db/scripts/apply-migrations.sh FinRiskDB
   bash db/scripts/seed.sh FinRiskDB
   ```

3. **Smoke-test the API** (HTTP 200 and JSON)

   ```bash
   curl -sf http://localhost:18080/actuator/health
   curl -sf "http://localhost:18080/api/v1/users?page=0&size=2" | head -c 200; echo
   curl -sf "http://localhost:18080/api/v1/assets?page=0&size=2" | head -c 200; echo
   ```

4. **Run the web UI**

   ```bash
   cd finrisk-web
   npm install
   npm run dev
   ```

   Open the URL Vite prints (usually `http://localhost:5173`). The browser calls `/api/v1/...` on the same origin; Vite forwards those requests to **18080**.

### Vite: `http proxy error` / `ECONNREFUSED 127.0.0.1:18080`

Nothing is listening on **18080** — usually the **`finrisk-api` container is stopped** (for example you only ran `make dev`, which starts SQL Server but not the API). Run **`make up`** (or `docker compose up -d finrisk-api`) with `.env` loaded, wait a few seconds, then retry in the browser or restart `npm run dev`.

### If `mvn spring-boot:run` fails with “port 8080 already in use”

Something else (often the **Docker** API container) is already bound to **8080** on the loopback, or another Java process is. Prefer **`docker compose up`** for the stack, or stop the other process, or run Spring with `SERVER_PORT=8081` only if you also change the Vite proxy target to match.

## Optional: API on the host (port 8080)

Use this when you want to debug the JVM **without** the `finrisk-api` container (stop that container first so **8080** is free). Then point Vite at it by changing `finrisk-web/vite.config.ts` proxy `target` to `http://localhost:8080`, or run the UI with a reverse proxy you configure yourself.

```bash
set -a && source .env && set +a
mvn spring-boot:run
```

### Health checks

- API in Docker: `curl -s http://localhost:18080/actuator/health`
- API on host: `curl -s http://localhost:8080/actuator/health`

## Makefile targets

```bash
make dev     # docker compose up -d sqlserver only
make up      # docker compose up -d --build (SQL Server + API on :18080)
make test    # mvn -B test
make verify  # mvn -B verify
make e2e     # bash scripts/e2e.sh
```

## Tests

```bash
export SA_PASSWORD=...   # or source .env
mvn test        # Surefire (*Test.java)
mvn verify      # Surefire + Failsafe (*IT.java)
```

## End-to-end runner

Runs migrations, `mvn test`, `mvn verify`, seeds data, builds/runs the Docker image, then executes `scripts/e2e-happy-path.sh`.

```bash
export SA_PASSWORD=...
bash scripts/e2e.sh
```

## Postman Flows (E2E happy path)

Postman **Flows** are canvas workflows built from blocks (see [Build flows overview](https://learning.postman.com/docs/postman-flows/build-flows/overview/)). They are **not** the same as importing a Collection JSON.

1. Import **`postman/FinRisk-E2E-Happy-Path.postman_collection.json`** and set collection variable **`baseUrl`** (`http://localhost:18080` for Docker).
2. Follow **`postman/flows/FINRISK-E2E-HAPPY-PATH.md`**: add **HTTP Request** blocks, pick each request from the collection, and chain **Success → Send** between blocks ([Run requests in sequence](https://learning.postman.com/docs/postman-flows/tutorials/advanced/run-requests-in-sequence/)).

To version a `.flow` file in Git, use Postman Desktop **Native Git** / **Local View** ([Manage flows with Native Git](https://learning.postman.com/docs/postman-flows/get-started/flows-native-git/)); the binary/JSON layout is authored by Postman, not checked in here.

### Seed smoke (3 requests, no setup scripts)

Import **`postman/FinRisk-Seed-Smoke.postman_collection.json`**. After **`bash db/scripts/seed.sh FinRiskDB`**, run the collection (or three **HTTP Request** blocks in a Flow):

1. `GET /api/v1/users/1` — first seed user.
2. `GET /api/v1/accounts/1/portfolio` — account **1** holds **SEED00001–SEED00003** (see `db/seed/seed.sql` tail).
3. `GET /api/v1/accounts/1/risk` — volatility **breakdown** for those three instruments (console log in Tests).

```bash
newman run postman/FinRisk-Seed-Smoke.postman_collection.json --env-var "baseUrl=http://localhost:18080"
```

### Collection Runner and Newman (E2E happy path)

- Open **FinRisk — E2E happy path** → folder **E2E happy path** → **Run folder** (requests **01 → 11**).
- Or: `newman run postman/FinRisk-E2E-Happy-Path.postman_collection.json --env-var "baseUrl=http://localhost:18080"`

## Regenerate web client types

After changing `openapi.yaml`:

```bash
cd finrisk-web
npm run generate:api
```
