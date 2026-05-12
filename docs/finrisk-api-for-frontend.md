# FinRisk API — frontend / Stitch AI reference

Machine-readable spec: [`openapi.yaml`](../openapi.yaml) at the repository root (attach to tools that accept OpenAPI).

## Product summary

FinRisk is an **investment portfolio REST API**. A **user** (human; **no authentication** in scope) owns **investment accounts** denominated in **USD**. Each account holds **cash** and **positions** from **buy/sell** trades on **assets** (stocks, ETFs, bonds, crypto). The API exposes **portfolio value**, **profit/loss**, and a **volatility-based risk score**.

## Conventions

| Topic | Detail |
|--------|--------|
| Base path | All routes under `/api/v1` |
| Auth | None (open API) |
| Currency | USD only; no FX conversion |
| Versioning | `/api/v1` prefix on every endpoint |

### Pagination

List endpoints accept:

- `page` — zero-based page index (default `0`)
- `size` — page size, 1–100 (default `20`)
- `sort` — repeatable; format `field,direction` with `direction` ∈ `asc`, `desc` (e.g. `sort=createdAt,desc`)

Responses use a **page envelope**: `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.

### Errors

JSON body:

```json
{
  "code": "string",
  "message": "string",
  "details": {}
}
```

Stable `code` values include: `VALIDATION_ERROR`, `ACCOUNT_NOT_FOUND`, `ASSET_NOT_FOUND`, `USER_NOT_FOUND`, `INSUFFICIENT_BALANCE`, `INSUFFICIENT_QUANTITY`, `INVALID_TRANSACTION`, `EMAIL_ALREADY_EXISTS`, `SYMBOL_ALREADY_EXISTS`, `INTERNAL_ERROR`.

### Enums

- **AssetType:** `STOCK`, `ETF`, `BOND`, `CRYPTO`
- **TransactionType:** `BUY`, `SELL`
- **RiskLevel:** `LOW`, `MEDIUM`, `HIGH`, `VERY_HIGH`
- **Currency:** always `USD` in responses

---

## Endpoints

### Users

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/users` | Create user. Body: `fullName`, `email`. **201** user; **400** validation; **409** email already in use. |
| `GET` | `/api/v1/users` | List users (paged). Optional query: `email` — case-insensitive **prefix** filter on email. |
| `GET` | `/api/v1/users/{id}` | Get user by id. **404** if not found. |

### Accounts

| Method | Path | Summary |
|--------|------|---------|
| `GET` | `/api/v1/users/{userId}/accounts` | List accounts for a user (paged). **404** if user not found. |
| `POST` | `/api/v1/accounts` | Open account. Body: `userId`, `accountName`, optional `initialDeposit` (≥ 0, default 0). **201** account; **404** owning user not found; **400** validation. |
| `GET` | `/api/v1/accounts/{id}` | Get account by id (cash balance, currency, metadata). **404** if not found. |
| `POST` | `/api/v1/accounts/{id}/deposit` | Deposit USD. Body: `amount` (≥ 0.01). **200** updated account. |
| `POST` | `/api/v1/accounts/{id}/withdraw` | Withdraw USD. Same body. **409** insufficient cash; **400** / **404** as usual. |

### Assets

| Method | Path | Summary |
|--------|------|---------|
| `GET` | `/api/v1/assets` | List assets (paged). Optional filters: `type` (`AssetType`), `symbol` (exact match, case-insensitive), `search` (substring on `name`, case-insensitive). |
| `POST` | `/api/v1/assets` | Register asset. Body is **polymorphic**, discriminated by `assetType` (see [Asset create / response shapes](#asset-create--response-shapes)). **201** asset; **409** symbol already exists; **400** validation. |
| `GET` | `/api/v1/assets/{id}` | Get asset by id (subtype fields in response). **404** if not found. |
| `PUT` | `/api/v1/assets/{id}/price` | Update current price. Body: `{ "price": number }` (minimum `0.0001`). Updates `current_price` and appends **price history**; risk for holdings recomputes on next `GET .../risk`. |
| `GET` | `/api/v1/assets/{id}/price-history` | Paged historical prices (most recent first): `price`, `currency`, `recordedAt`. |

### Transactions

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/transactions/buy` | Buy asset. Body: `accountId`, `assetId`, `quantity` (integer ≥ 1), `unitPrice` (≥ 0.0001). Debits cash; audit logged. **201** transaction; **409** insufficient cash; **404** account or asset. |
| `POST` | `/api/v1/transactions/sell` | Sell asset. Same body. Credits cash. **201** transaction; **409** insufficient quantity; **404** account or asset. |
| `GET` | `/api/v1/accounts/{accountId}/transactions` | List transactions for an account (paged). Optional: `type` (`BUY`/`SELL`), `assetId`, `from` / `to` (ISO 8601 `date-time` on `transactionDate`; `from` inclusive, `to` **exclusive**). |

**Transaction response** (representative fields): `id`, `accountId`, `assetId`, `assetSymbol` (convenience), `transactionType`, `quantity`, `unitPrice`, `totalAmount`, `currency`, `transactionDate`.

### Portfolio (derived)

| Method | Path | Summary |
|--------|------|---------|
| `GET` | `/api/v1/accounts/{accountId}/portfolio` | Current holdings and totals: `cashBalance`, `holdings[]`, `totalHoldingsValue`, `totalAccountValue` (cash + holdings). All USD. |
| `GET` | `/api/v1/accounts/{accountId}/profit-loss` | P&L per holding and `totalProfitLoss`. Per holding: `netInvested`, `currentValue`, `profitLoss`, optional `profitLossPercent`. |
| `GET` | `/api/v1/accounts/{accountId}/risk` | Risk score: `score` (0–100), aggregate `level`, `strategy` (e.g. `VOLATILITY`), `breakdown[]` per asset (`riskLevel`, `weight`, `volatility` nullable, `sampleSize`). |

#### Risk behavior (for UI copy / tooltips)

Per asset: use last ~30 price points from history → log returns → standard deviation → map to `RiskLevel` (e.g. `LOW` if volatility below 0.01, `MEDIUM` below 0.03, `HIGH` below 0.06, otherwise `VERY_HIGH`). If fewer than ~5 samples, fall back to asset **default** risk level. Portfolio score is a **value-weighted** blend of volatilities, normalized to 0–100.

---

## Asset create / response shapes

Common create fields: `symbol`, `name`, `assetType`, `currentPrice` (≥ 0.0001).

| `assetType` | Extra required fields |
|-------------|------------------------|
| `STOCK` | `sector`, `exchange` |
| `ETF` | `issuer`; optional `expenseRatio` |
| `BOND` | `interestRate`, `maturityDate`, `issuer` |
| `CRYPTO` | `blockchain` |

Responses mirror subtypes (`StockResponse`, `EtfResponse`, etc.) with discriminator `assetType`. Responses include `defaultRiskLevel` for history-poor risk fallback.

---

## Suggested UI areas

1. **Users** — create, list (email prefix), detail by id.
2. **Accounts** — list by `userId`, create with optional initial deposit, view account, deposit/withdraw.
3. **Assets** — list with filters, polymorphic create form, detail, price update, price history table/chart.
4. **Trading** — buy/sell forms; transaction ledger with type, asset, date range filters.
5. **Portfolio** — holdings + totals; P&L view; risk score + breakdown.

**IDs:** path and query IDs are integers (`int64`).

---

## Local / deployment note

Example server URL in OpenAPI: `http://localhost:8080`. A Vite dev app may proxy same-origin `/api/...` to the backend; production may set a public API base URL in the client — this is **not** part of the OpenAPI contract.
