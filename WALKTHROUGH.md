# FinRisk — Project Walkthrough (Jury Defense Edition)

This document walks through **everything** in the FinRisk project, from
"what is investing" all the way down to "what does this Java line do".

Read it top to bottom. Each section first defines the idea, then shows where
it lives in the code.

> Stack in one line: **Java 21 + Spring Boot 3 + raw JDBC + Microsoft SQL
> Server**, all packaged in **Docker**.

---

## TABLE OF CONTENTS

1. [The financial story (no finance background needed)](#1-the-financial-story-no-finance-background-needed)
2. [What FinRisk does (the product)](#2-what-finrisk-does-the-product)
3. [Technology stack — what each piece is and why](#3-technology-stack--what-each-piece-is-and-why)
4. [Layered architecture — Controller → Service → DAO → DB](#4-layered-architecture--controller--service--dao--db)
5. [The database, table by table](#5-the-database-table-by-table)
6. [The Java code, folder by folder](#6-the-java-code-folder-by-folder)
7. [OOP concepts and where they appear](#7-oop-concepts-and-where-they-appear)
8. [Design patterns and where they live](#8-design-patterns-and-where-they-live)
9. [JDBC explained](#9-jdbc-explained)
10. [End-to-end: a buy transaction](#10-end-to-end-a-buy-transaction)
11. [The risk score, explained step by step](#11-the-risk-score-explained-step-by-step)
12. [The REST API](#12-the-rest-api)
13. [Docker and how it all runs](#13-docker-and-how-it-all-runs)
14. [Testing strategy (TDD)](#14-testing-strategy-tdd)
15. [Likely jury questions and crisp answers](#15-likely-jury-questions-and-crisp-answers)

---

## 1. THE FINANCIAL STORY (no finance background needed)

This whole project is a tiny version of an app like Robinhood, eToro, or
Trade Republic. Imagine you have some money and you want to invest it.

### 1.1 What is investing?

You take money you do not need today and you use it to **buy a piece of
something** that you hope will be worth more later. If it goes up, you win.
If it goes down, you lose.

### 1.2 What is an "asset"?

An **asset** is the "thing" you can buy. In our project there are 4 kinds:

- **Stock**: a tiny share of ownership in a company. If you buy 1 share of
  Apple (symbol `AAPL`), you own one extremely small slice of Apple. If
  Apple does well, your share is worth more.
- **ETF** ("Exchange-Traded Fund"): a basket of many stocks bundled
  together. Example: the `VOO` ETF holds 500 large US companies. Buying
  1 `VOO` is like buying a tiny bit of all 500 at once. ETFs are usually
  safer than a single stock because the risk is spread out.
- **Bond**: a loan. A government or company borrows money from you, and
  promises to pay it back later **plus a fixed interest rate**. Bonds are
  usually the safest of the four.
- **Cryptocurrency** (Crypto): a digital asset like Bitcoin (`BTC`) that
  lives on a blockchain. Prices move very fast, both up and down — so
  crypto is the **riskiest** of the four.

### 1.3 What is a "user account"?

In FinRisk, a **user** is the human (you). A **user can have one or more
accounts**. An **account** is like a separate wallet for investing — for
example, you might have one account for retirement and one for short-term
trading. Each account has:

- A **cash balance** in US dollars (USD).
- A history of trades the account made (we call them **transactions**).
- A list of **holdings** — what the account currently owns.

### 1.4 What is a "transaction"?

A **transaction** is one trade — one buy or one sell.

- **Buy**: cash leaves the account, and the account gets some quantity of
  an asset. Example: "Buy 10 shares of AAPL at $180" means $1,800 leaves
  the cash balance and 10 AAPL shares enter the account's holdings.
- **Sell**: the opposite. The account gives up some asset, and cash comes
  in. Example: "Sell 4 AAPL at $190" means 4 shares leave and $760 lands
  in cash.

### 1.5 What is a "holding"?

A holding is "how much of this asset I currently own". You don't write it
down anywhere — you **derive** it by adding up your buys and subtracting
your sells. Example:

```
Bought 10 AAPL  -> +10
Bought  5 AAPL  -> +5
Sold   4 AAPL  -> -4
==============
Holding: 11 AAPL shares
```

### 1.6 What is "portfolio value"?

The total dollar value of everything the account owns right now.

```
portfolio value = cash balance
                + sum( holding quantity x current asset price )
```

### 1.7 What is "profit and loss" (P/L)?

For each holding:

```
net invested  = total cash spent on BUYs - total cash received from SELLs
current value = quantity owned x current price
profit/loss   = current value - net invested
```

Positive P/L means you are up. Negative means you are down.

### 1.8 What is "risk"?

Risk is **how much an asset's price jumps around**. A bond barely moves
day to day → low risk. A crypto coin can move 10% in an hour → very high
risk.

We measure risk with a number called **volatility** (the Greek letter
**sigma**, σ). Volatility is the **standard deviation of daily returns** —
in plain words: "on a typical day, how far does the price move from its
average?". Bigger σ = bigger swings = more risk.

We then bucket σ into 4 levels:

| Daily σ          | Risk level     | Plain meaning                           |
| ---------------- | -------------- | --------------------------------------- |
| σ < 1%           | `LOW`          | Very steady (e.g. a government bond)    |
| 1% ≤ σ < 3%      | `MEDIUM`       | Normal stock market behavior            |
| 3% ≤ σ < 6%      | `HIGH`         | Aggressive, individual tech stocks      |
| σ ≥ 6%           | `VERY_HIGH`    | Crypto-style swings                     |

### 1.9 Why USD only?

We picked one currency to keep the project focused. All cash, prices, and
totals are in **US dollars**. There is no currency conversion logic.

### 1.10 Recap (one paragraph)

> A user opens an investment account, deposits some USD, and uses that
> cash to buy assets (stocks, ETFs, bonds, crypto). FinRisk records every
> buy and sell, then derives at any moment: how much of each asset the
> user owns, how much the whole account is worth, how much profit or loss
> the user has, and how risky their portfolio is.

---

## 2. WHAT FINRISK DOES (the product)

### 2.1 60-second pitch

FinRisk is a **REST API** (a backend, no frontend in this project) that
lets a client app:

1. Create users and investment accounts.
2. Catalogue assets the world can trade (Stock, ETF, Bond, Crypto).
3. Update an asset's price (and remember the price history).
4. Execute buy and sell trades against an account.
5. Read back the account's portfolio, profit/loss, and risk score.

### 2.2 Typical user journey

```
1. POST /api/v1/users                        -> create user "Alice"
2. POST /api/v1/accounts                     -> open account, deposit $5,000
3. POST /api/v1/assets   (assetType=STOCK)   -> register AAPL at $180
4. POST /api/v1/transactions/buy             -> buy 10 AAPL  ($1,800 spent)
5. PUT  /api/v1/assets/{id}/price            -> AAPL price moves to $190
6. POST /api/v1/transactions/sell            -> sell 4 AAPL  ($760 in)
7. GET  /api/v1/accounts/{id}/portfolio      -> "you own 6 AAPL, total $1,140 + $3,960 cash"
8. GET  /api/v1/accounts/{id}/profit-loss    -> "you made $60 on AAPL"
9. GET  /api/v1/accounts/{id}/risk           -> "your portfolio risk is HIGH"
```

This exact journey is what `scripts/e2e-happy-path.sh` runs end-to-end on
every `bash scripts/e2e.sh` run.

---

## 3. TECHNOLOGY STACK — what each piece is and why

| Tool                | What it is                                                                                  | Why we use it here                                                            |
| ------------------- | ------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **Java 21**         | A general-purpose, statically-typed, object-oriented programming language.                  | Mandatory by the school brief. Modern Java (records, switch expressions, sealed interfaces, text blocks) keeps the code short. |
| **Spring Boot 3**   | A framework that handles boilerplate for building web applications (HTTP server, JSON, dependency injection). | Lets us focus on `@RestController` / `@Service` / `@Repository` instead of writing a web server from scratch. We use **only** the web + validation parts. **No JPA / Hibernate.** |
| **Maven**           | The build tool. Reads `pom.xml`, downloads dependencies, compiles, runs tests, packages the JAR. | Standard for Java school projects.                                            |
| **JDBC**            | The standard low-level Java API for talking to a database.                                  | The brief explicitly demonstrates `Connection`, `PreparedStatement`, `ResultSet`, `try-catch-finally`. We do raw JDBC instead of using JPA. |
| **HikariCP**        | A connection pool. Keeps a small number of DB connections open and ready to reuse.          | Opening a fresh DB connection per request is slow. HikariCP gives us speed without changing how the code looks. |
| **mssql-jdbc**      | Microsoft's official JDBC driver for SQL Server.                                            | The DB we chose is Microsoft SQL Server, so we need its driver.               |
| **Microsoft SQL Server 2022** | A relational database (the "system of record"). Stores users, accounts, assets, transactions, audit logs. | Mandatory by the brief. Supports stored procedures, views, constraints — all features we use. |
| **Docker + Docker Compose** | Containerization tools. A "container" is a small, portable box that has everything an app needs to run. | Lets the SQL Server and the API run identically on any machine. One command starts both. |
| **JUnit 5 + Mockito + AssertJ + RestAssured** | Testing libraries.                                                  | JUnit runs the tests, Mockito fakes collaborators in unit tests, AssertJ has nicer assertions, RestAssured drives the API end-to-end. |
| **Surefire + Failsafe** | Maven plugins. Surefire runs `*Test.java` (unit tests). Failsafe runs `*IT.java` (integration tests). | Splits "fast tests" from "tests that need a real database".                  |

### Why no JPA / Hibernate?

The brief explicitly demonstrates raw JDBC ceremony (`Connection`,
`PreparedStatement`, `ResultSet`, `try-catch-finally`). JPA hides all of
that. So we kept JDBC visible, but centralized the boilerplate inside one
small helper class (`Db`) so each DAO method stays short.

---

## 4. LAYERED ARCHITECTURE — Controller → Service → DAO → DB

The code is split into 4 layers. Each layer only talks to the one
**directly below** it.

```
HTTP request from a client (e.g. Postman, curl, the e2e script)
       |
       v
  +--------------------+
  | Controller layer   |  <-- understands HTTP. Reads JSON, returns JSON.
  +--------------------+      Translates URL paths and validates input.
       |
       v
  +--------------------+
  | Service layer      |  <-- the business rules.
  +--------------------+      "If cash < total cost, reject the buy."
       |
       v
  +--------------------+
  | DAO layer          |  <-- the only layer that talks SQL.
  +--------------------+      Reads rows, writes rows, calls procedures.
       |
       v
  +--------------------+
  | SQL Server         |  <-- stores everything. Has tables, indexes,
  +--------------------+      views, and stored procedures.
```

### Why split into layers?

- **Each layer has one job.** A controller doesn't know SQL exists. A DAO
  doesn't know HTTP exists. This makes the code easy to test and easy to
  change.
- **You can swap a layer without touching the others.** Tomorrow we could
  replace SQL Server with PostgreSQL by changing only the DAO layer.
- **Bugs are easier to find.** "It returns the wrong number" → it's in the
  service. "It crashes on JSON" → it's in the controller. "It loses data"
  → it's in the DAO.

### Code layout in this project

```
com.finrisk
├── controller/        REST endpoints (the HTTP entrypoints)
├── service/           business logic
├── dao/               persistence interfaces (one per entity)
│   └── impl/          JDBC implementations of those interfaces
├── model/             the domain objects (User, Account, Asset, Transaction…)
├── dto/
│   ├── request/       what comes IN over HTTP
│   └── response/      what goes OUT over HTTP
├── mapper/            translates Domain <-> DTO
├── factory/           builds polymorphic objects (AssetFactory, TransactionFactory)
├── strategy/risk/     pluggable risk-calculation algorithm
├── exception/         our custom exceptions
├── config/            wiring (HikariCP setup, Jackson, Spring beans)
└── util/              shared helpers (Db, SqlSort, JdbcSqlExceptionMapper)
```

---

## 5. THE DATABASE, TABLE BY TABLE

The schema lives in
[`db/migrations/V1__schema.sql`](db/migrations/V1__schema.sql) (tables and
constraints), with indexes in `V2__indexes.sql`, views in `V3__views.sql`,
and stored procedures in `V4__procedures.sql`.

### 5.1 `users`

A row per human user.

| Column       | Type            | Notes                                  |
| ------------ | --------------- | -------------------------------------- |
| `id`         | `BIGINT IDENTITY` PK | auto-increment primary key        |
| `full_name`  | `NVARCHAR(100)` | not null                               |
| `email`      | `NVARCHAR(150)` | **UNIQUE** + not null                  |
| `created_at` | `DATETIME2`     | defaults to `SYSDATETIME()`            |

**Constraint to remember:** the unique index on `email` is what makes the
`EmailAlreadyExistsException` happen (DAO catches the SQL state and
re-throws our domain exception).

### 5.2 `accounts`

A row per investment account. A user can have many accounts.

| Column         | Type                | Notes                                                |
| -------------- | ------------------- | ---------------------------------------------------- |
| `id`           | `BIGINT IDENTITY`   | PK                                                   |
| `user_id`      | `BIGINT`            | **FK → users.id**                                    |
| `account_name` | `NVARCHAR(100)`     |                                                      |
| `cash_balance` | `DECIMAL(19,4)`     | **CHECK (cash_balance >= 0)** — cash can never go negative |
| `created_at`   | `DATETIME2`         |                                                      |

The `CHECK` constraint is a database-level safety net. Even if a bug in
Java tried to set a negative balance, SQL Server would refuse.

### 5.3 `assets` + 4 subtype tables

This is the **polymorphic** part of the schema. One parent table
(`assets`) holds the common fields, and four "details" tables hold the
fields specific to each asset type.

`assets`:

| Column              | Type             | Notes                                                  |
| ------------------- | ---------------- | ------------------------------------------------------ |
| `id`                | `BIGINT IDENTITY`| PK                                                     |
| `symbol`            | `NVARCHAR(20)`   | **UNIQUE** (e.g. `AAPL`, `BTC`, `VOO`)                 |
| `name`              | `NVARCHAR(150)`  |                                                        |
| `asset_type`        | `NVARCHAR(30)`   | **CHECK (in 'STOCK','ETF','BOND','CRYPTO')**           |
| `current_price`     | `DECIMAL(19,4)`  | **CHECK (current_price > 0)**                          |
| `risk_level`        | `NVARCHAR(30)`   | **CHECK (in 'LOW','MEDIUM','HIGH','VERY_HIGH')**       |
| `created_at`        | `DATETIME2`      |                                                        |

Then four 1-to-1 tables that share the same primary key as `assets`:

- `asset_details_stock(asset_id, sector, exchange_name)`
- `asset_details_etf(asset_id, issuer, expense_ratio)`
- `asset_details_bond(asset_id, interest_rate, maturity_date, issuer)`
- `asset_details_crypto(asset_id, blockchain)`

This mirrors the Java side perfectly: the abstract `Asset` is the parent
table, and each concrete subclass has its own details table.

### 5.4 `transactions`

Every BUY and SELL ever made.

| Column             | Type              | Notes                                       |
| ------------------ | ----------------- | ------------------------------------------- |
| `id`               | `BIGINT IDENTITY` | PK                                          |
| `account_id`       | `BIGINT`          | FK → accounts.id                            |
| `asset_id`         | `BIGINT`          | FK → assets.id                              |
| `transaction_type` | `NVARCHAR(10)`    | CHECK in `'BUY','SELL'`                     |
| `quantity`         | `INT`             | CHECK > 0                                   |
| `unit_price`       | `DECIMAL(19,4)`   | CHECK > 0                                   |
| `transaction_date` | `DATETIME2`       |                                             |

This is the **single source of truth** for "what happened". Holdings,
portfolio value, and P/L are all **derived** from this table.

### 5.5 `asset_price_history`

Every time an asset's price changes, we INSERT a row here. This is what
the volatility-based risk calculator reads.

| Column        | Type              | Notes                       |
| ------------- | ----------------- | --------------------------- |
| `id`          | `BIGINT IDENTITY` | PK                          |
| `asset_id`    | `BIGINT`          | FK                          |
| `price`       | `DECIMAL(19,4)`   | CHECK > 0                   |
| `recorded_at` | `DATETIME2`       |                             |

### 5.6 `audit_logs`

Append-only log of important business events. The stored procedures write
to it. Useful for "who did what when" investigations.

| Column        | Notes                                                    |
| ------------- | -------------------------------------------------------- |
| `entity_name` | e.g. `'TRANSACTION'`                                     |
| `entity_id`   | the row id of what changed                               |
| `action_type` | e.g. `'BUY_TRANSACTION_CREATED'`, `'SELL_TRANSACTION_CREATED'` |
| `description` | free text                                                |
| `created_at`  | when                                                     |

### 5.7 Indexes (`V2__indexes.sql`)

Indexes are like the index at the back of a book — they let SQL Server
find rows fast without scanning the entire table.

- `idx_transactions_account_id` — speeds up "give me all transactions for
  account X".
- `idx_transactions_account_date` — speeds up "give me transactions for
  account X ordered by date".
- `idx_assets_symbol` — speeds up "find AAPL".
- `idx_assets_type` — speeds up "give me all stocks".
- `idx_price_history_asset_date` — speeds up "give me the last 30 prices
  for asset X".

### 5.8 Views (`V3__views.sql`)

A view is a **saved SELECT**. It looks like a table but it's actually a
query that runs every time you read it.

- **`vw_portfolio_holdings`** — for each `(account_id, asset_id)` pair,
  computes `total_quantity = sum(BUY) - sum(SELL)` and `current_value =
  total_quantity * current_price`. The portfolio endpoint reads this.
- **`vw_portfolio_summary`** — sums `current_value` per account.
- **`vw_portfolio_profit_loss`** — joins holdings with transactions to
  compute `net_invested` and `profit_loss = current_value - net_invested`
  per holding.

Why views? They put the heavy aggregation **inside the database** where
it's fastest. Java just reads the result.

### 5.9 Stored procedures (`V4__procedures.sql`)

A **stored procedure** is a function that lives **inside** the database.
You call it like a method, but it runs in SQL.

We use two: **`sp_buy_asset`** and **`sp_sell_asset`**. Both do exactly
the same shape of work:

1. Look up the account row (with a database **lock** so nobody else can
   modify it at the same time).
2. Validate (does the asset exist? is there enough cash / enough quantity?).
3. **Open a SQL transaction** (`BEGIN TRANSACTION`).
4. UPDATE `accounts.cash_balance`.
5. INSERT a row into `transactions`.
6. INSERT a row into `audit_logs`.
7. **Commit** (`COMMIT`) — or **rollback** if anything fails.

Why a stored procedure?

- **Atomicity**. Either all 3 inserts/updates happen or none of them
  happen. No "half a trade".
- **Performance**. One round-trip from Java instead of 3.
- **Validation lives next to the data**. Even if a different client
  application connected directly, the rules would still be enforced.

The procedure code is in
[`db/migrations/V4__procedures.sql`](db/migrations/V4__procedures.sql),
and Java calls it via `CallableStatement` (in `Db.call(...)`).

---

## 6. THE JAVA CODE, FOLDER BY FOLDER

### 6.1 `model/` — the domain

These are the "things" the business cares about.

```5:5:src/main/java/com/finrisk/model/User.java
public record User(Long id, String fullName, String email, LocalDateTime createdAt) {}
```

A **record** in Java is a one-line class for holding data. It
**automatically** gives you:

- private final fields (encapsulation),
- a constructor,
- accessor methods (`user.id()`, `user.fullName()`),
- `equals`, `hashCode`, `toString`.

So that one line replaces what used to be ~50 lines of getters/setters.

The polymorphic hierarchy uses a **sealed interface** + records:

```6:23:src/main/java/com/finrisk/model/Asset.java
public sealed interface Asset permits Stock, ETF, Bond, CryptoAsset {

    Long id();

    String symbol();

    String name();

    BigDecimal currentPrice();

    RiskLevel riskLevel();

    LocalDateTime createdAt();

    AssetType type();

    RiskLevel calculateRiskLevel();
}
```

`sealed` means: only the four classes listed in `permits` are allowed to
implement this interface. The compiler enforces it. This is the modern
Java way of saying "this is a closed family of types".

Each concrete asset is a record that implements `Asset`:

```6:26:src/main/java/com/finrisk/model/Stock.java
public record Stock(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        String sector,
        String exchange)
        implements Asset {

    @Override
    public AssetType type() {
        return AssetType.STOCK;
    }

    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.HIGH;
    }
}
```

`@Override` says "I am replacing a method from my parent type". This is
**polymorphism**: every asset has a `calculateRiskLevel()` method, but
each subclass returns a different value (`HIGH` for Stock, `MEDIUM` for
ETF, `LOW` for Bond, `VERY_HIGH` for Crypto).

### 6.2 `dto/` — Data Transfer Objects

DTOs are the "wire format". They are what JSON requests and responses get
converted into. They are deliberately **separate** from `model/` so that
changing the database doesn't break the API contract (and vice versa).

Example:

```java
public record UserCreateRequest(
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Email @Size(max = 150) String email) {}
```

The `@NotBlank` / `@Email` / `@Size` annotations are **validation
constraints**. Spring runs them automatically when `@Valid` is on the
controller method.

### 6.3 `dao/` — Data Access Objects

DAO = Data Access Object. **The only place in the code that knows SQL.**

Each entity has:

- An **interface** (e.g. [`UserDao`](src/main/java/com/finrisk/dao/UserDao.java))
  describing what operations exist (`findById`, `save`, `findByEmail`, …).
- A **JDBC implementation** in `dao/impl/` (e.g.
  [`UserDaoJdbc`](src/main/java/com/finrisk/dao/impl/UserDaoJdbc.java))
  that actually runs the SQL.

All interfaces extend a generic parent:

```java
public interface GenericDao<T, ID> {
    T findById(ID id);
    List<T> findAll();
    T save(T entity);
    void update(T entity);
    void delete(ID id);
}
```

`<T, ID>` is **genericity**. It means "this interface works with any
entity type `T` and any id type `ID`". `UserDao` extends
`GenericDao<User, Long>` — so its `findById` returns a `User`.

A typical DAO method now looks like this:

```66:68:src/main/java/com/finrisk/dao/impl/UserDaoJdbc.java
    @Override
    public User findById(Long id) {
        return Db.findOne(FIND_BY_ID, UserDaoJdbc::map, id).orElse(null);
    }
```

That's it — three lines. The JDBC ceremony lives in `Db.findOne`.

### 6.4 `service/` — business rules

Services are where the "what does the app actually do" logic lives. They
**orchestrate** DAOs but never write SQL themselves.

Example: the buy flow.

```35:41:src/main/java/com/finrisk/service/TransactionService.java
    public TransactionResponse buy(TradeRequest req) {
        requireAccount(req.accountId());
        requireAsset(req.assetId());
        transactionDao.executeBuyProcedure(
                req.accountId(), req.assetId(), req.quantity(), req.unitPrice());
        return loadResponse(req, TransactionType.BUY);
    }
```

Notice: the service does the **preconditions** (account exists, asset
exists), then asks the DAO to call the stored procedure. The "is there
enough cash?" check happens **inside the stored procedure** (with a row
lock) so two parallel buys can't both succeed using the same dollar.

### 6.5 `controller/` — HTTP entrypoints

Controllers translate HTTP into method calls and back.

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) { // constructor injection
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest req) {
        return userService.createUser(req);
    }
}
```

- `@RestController` = "this class returns JSON, not HTML".
- `@RequestMapping` = the URL prefix.
- `@PostMapping` = handles `POST` requests.
- `@RequestBody` = "parse the JSON body into this object".
- `@Valid` = "run the validation annotations first".
- `@ResponseStatus(CREATED)` = "respond with HTTP 201 instead of 200".

### 6.6 `mapper/` — translates Domain ↔ DTO

A mapper is a tiny static class that converts a domain object to a DTO
and back. Example:

```java
public final class UserMapper {
    private UserMapper() {}
    public static UserResponse toResponse(User u) {
        return new UserResponse(u.id(), u.fullName(), u.email(), u.createdAt());
    }
}
```

Why? Because we **don't want** the `User` domain object to be exposed
directly as JSON — that would couple the API contract to the database
schema. Mappers are the bridge.

### 6.7 `factory/` — Factory Method

Factories build polymorphic objects when the type is decided at runtime.

```22:61:src/main/java/com/finrisk/factory/AssetFactory.java
    public static Asset create(AssetCreateRequest req) {
        return switch (req) {
            case StockCreateRequest s -> new Stock(
                    null,
                    s.symbol().trim(),
                    s.name().trim(),
                    s.currentPrice(),
                    RiskLevel.HIGH,
                    null,
                    s.sector(),
                    s.exchange());
            case EtfCreateRequest e -> new ETF(
                    null,
                    e.symbol().trim(),
                    e.name().trim(),
                    e.currentPrice(),
                    RiskLevel.MEDIUM,
                    null,
                    e.issuer(),
                    e.expenseRatio());
            case BondCreateRequest b -> new Bond(
                    null,
                    b.symbol().trim(),
                    b.name().trim(),
                    b.currentPrice(),
                    RiskLevel.LOW,
                    null,
                    b.interestRate(),
                    b.maturityDate(),
                    b.issuer());
            case CryptoCreateRequest c -> new CryptoAsset(
                    null,
                    c.symbol().trim(),
                    c.name().trim(),
                    c.currentPrice(),
                    RiskLevel.VERY_HIGH,
                    null,
                    c.blockchain());
        };
    }
```

A `switch` expression on a sealed interface is **exhaustive**: the
compiler knows there are exactly 4 subtypes and will refuse to compile if
we forget one. No `default` branch needed.

### 6.8 `strategy/risk/` — Strategy Pattern

The risk computation is hidden behind an interface so we can swap formulas
without touching `RiskService`.

```java
public interface RiskCalculationStrategy {
    double sigmaFromPrices(List<BigDecimal> chronologicalAscending);
    RiskLevel levelForSigma(double dailySigma);
    RiskLevel fallbackLevel(Asset asset);
}
```

The default implementation uses **volatility** (see section 11). Tomorrow
we could plug in a `FixedMappingRiskStrategy` and the rest of the code
wouldn't change.

### 6.9 `exception/` — custom exceptions

Each business error has its own exception class:

- `DaoException` — anything from JDBC.
- `AccountNotFoundException`, `AssetNotFoundException`,
  `UserNotFoundException` — 404s.
- `InsufficientBalanceException`, `InsufficientQuantityException` — 409s.
- `InvalidTransactionException`, `EmailAlreadyExistsException`,
  `SymbolAlreadyExistsException` — 400/409s.

These are caught by `GlobalExceptionHandler` and converted into the
shared `Error` JSON shape with the documented `code` strings.

### 6.10 `config/` — wiring

- [`DatabaseConnection`](src/main/java/com/finrisk/config/DatabaseConnection.java)
  — the **Singleton** holding our HikariCP pool. Reads DB host/user/password
  from environment variables.
- `JacksonConfig` — JSON formatting (dates as ISO strings, etc.).
- `StrategyConfig` — Spring `@Bean` declaring which strategy implementation
  to inject into `RiskService`.

### 6.11 `util/` — shared helpers

- [`Db`](src/main/java/com/finrisk/util/Db.java) — the JDBC helper. Single
  place where `Connection`, `PreparedStatement`, `ResultSet` and
  `try-with-resources` live.
- `SqlSort` — translates `?sort=createdAt,desc` query params into a safe
  `ORDER BY` clause (with a column whitelist to prevent SQL injection
  through sort fields).
- `JdbcSqlExceptionMapper` — when SQL Server raises `INSUFFICIENT_BALANCE`
  from a stored procedure, this turns it into our
  `InsufficientBalanceException`.

---

## 7. OOP CONCEPTS AND WHERE THEY APPEAR

| Concept             | What it means                                                                          | Where in our code                                                              |
| ------------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **Encapsulation**   | Fields are private; outside code can only read/write them through methods.             | Every `record` (e.g. `User`) has private final fields and accessor methods. The compiler generates them. |
| **Abstraction**     | Hide details behind a "what" interface, not a "how".                                   | `Asset` is a sealed interface — callers use `asset.calculateRiskLevel()` without knowing it's a Stock vs Bond. |
| **Inheritance**     | A class reuses/extends another type.                                                   | `Stock`, `ETF`, `Bond`, `CryptoAsset` all `implements Asset`. Same for `BuyTransaction`/`SellTransaction` implementing `Transaction`. |
| **Polymorphism**    | Same method name, different behavior depending on the real object.                     | `calculateRiskLevel()` returns `HIGH` on Stock, `LOW` on Bond, `VERY_HIGH` on CryptoAsset, `MEDIUM` on ETF. |
| **`@Override`**     | Java annotation marking that we are replacing a parent method.                         | Every concrete asset's `calculateRiskLevel()` and `type()`. Compiler warns us if the signature drifts from the parent. |
| **Interface**       | A contract: "any class that implements me must have these methods".                    | `UserDao`, `AccountDao`, `AssetDao`, `TransactionDao`, `RiskCalculationStrategy`, `GenericDao<T, ID>`. |
| **Genericity**      | Code that works for any type without copy-pasting.                                     | `GenericDao<T, ID>`, `Page<T>`, `Db.findOne(..., RowMapper<T>, ...)`, `Optional<User>`, `List<Asset>`. |
| **Composition**     | An object holds another object instead of inheriting.                                  | `UserController` holds a `UserService`; `UserService` holds a `UserDao`. Wired through constructors. |
| **Immutability**    | An object can't be modified after creation.                                            | All `record` types. To "change" a User you build a new one with `new User(...)`. |

---

## 8. DESIGN PATTERNS AND WHERE THEY LIVE

A "design pattern" is a named, well-known shape of code that solves a
recurring problem. We use 7 of them, on purpose, each one earning its
place.

### 8.1 DAO (Data Access Object)

**Problem.** "Don't scatter SQL all over the codebase."
**Solution.** One interface per entity that knows how to read/write that
entity. The rest of the code uses the interface.
**Where.** Every file under `dao/` and `dao/impl/`.

### 8.2 Generic DAO (Generic Repository)

**Problem.** "Every DAO has the same `findById`/`save`/`update`/`delete`
methods. Don't copy-paste."
**Solution.** A parent interface `GenericDao<T, ID>` with the common
methods. Each entity's DAO extends it and adds entity-specific methods.
**Where.**
[`GenericDao`](src/main/java/com/finrisk/dao/GenericDao.java).

### 8.3 Singleton

**Problem.** "We want exactly **one** database connection pool, shared by
the whole application."
**Solution.** A class with a private constructor and a static accessor
that lazily creates the single instance.
**Where.**
[`DatabaseConnection`](src/main/java/com/finrisk/config/DatabaseConnection.java)
— uses **double-checked locking** so it is thread-safe and only creates
the pool once.

### 8.4 Template Method

**Problem.** "All Asset types follow the same shape (id, symbol, name,
…), but each one computes its risk level differently."
**Solution.** Define the shape on a parent type; let each subtype fill
in the steps.
**Where.** `Asset` (sealed interface) declares `calculateRiskLevel()` and
`type()`; each record (`Stock`, `ETF`, `Bond`, `CryptoAsset`) overrides
them with its own constant. Same idea for `Transaction` /
`BuyTransaction` / `SellTransaction`.

### 8.5 Factory Method

**Problem.** "When the API receives an `AssetCreateRequest`, we need to
build the **right** subtype. We don't want `if/else` chains scattered
around."
**Solution.** A single method that takes the request and returns the
right concrete object.
**Where.** `AssetFactory.create(...)` and `TransactionFactory`. Both use
modern `switch` expressions on sealed interfaces, so the compiler
guarantees exhaustiveness.

### 8.6 Strategy

**Problem.** "We want to be able to swap the risk-calculation algorithm
without touching the rest of the app."
**Solution.** Define an interface for the algorithm (`strategy`), provide
one or more implementations, inject the chosen one into `RiskService`.
**Where.** `RiskCalculationStrategy` interface +
`VolatilityRiskStrategy` implementation. Wired in `StrategyConfig` as a
Spring `@Bean`.

### 8.7 DTO + Mapper

**Problem.** "Don't expose the internal domain model directly as JSON."
**Solution.** Define separate DTOs for HTTP I/O. Use mappers to translate.
**Where.** `dto/request/`, `dto/response/`, `mapper/`.

---

## 9. JDBC EXPLAINED

### 9.1 The 3 core types

- **`Connection`** — an open line to the database.
- **`PreparedStatement`** — a SQL query with `?` placeholders. You bind
  values into the placeholders, then execute.
- **`ResultSet`** — the rows that came back.

### 9.2 SQL injection — and why we never have it

The naive way to query is to glue strings together:

```java
// DO NOT DO THIS
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
```

If `email` is `' OR 1=1 --`, the query becomes
`SELECT * FROM users WHERE email = '' OR 1=1 --'` and returns every user.
This is **SQL injection**.

We use `PreparedStatement` with `?` placeholders:

```java
String sql = "SELECT * FROM users WHERE email = ?";
ps.setString(1, email);
```

The driver sends the SQL and the value **separately**. The value is never
parsed as SQL. Injection is impossible. **Every single query in our
codebase uses placeholders.**

### 9.3 `try-with-resources`

JDBC objects must be closed (otherwise we leak DB connections). Java's
`try (...)` block closes them automatically:

```java
try (Connection c = DatabaseConnection.getConnection();
     PreparedStatement ps = c.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // use rs
} // c, ps, rs all closed here, in reverse order, even on exception
```

This replaces the old-school `try / catch / finally / closeQuietly(...)`
ceremony.

### 9.4 ResultSet → object mapping

Every DAO has a tiny `map(ResultSet rs)` method that turns one row into a
Java object:

```52:59:src/main/java/com/finrisk/dao/impl/UserDaoJdbc.java
    private static User map(ResultSet rs) throws SQLException {
        var ts = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                ts == null ? null : ts.toLocalDateTime());
    }
```

### 9.5 The `Db` helper

The JDBC ceremony was repeating in every DAO method, so we factored it
into `util/Db.java`. Each DAO method becomes one or two lines, and all the
`Connection`/`PreparedStatement`/`ResultSet` ceremony lives in one place
(see `Db.findOne`, `Db.findMany`, `Db.update`, `Db.exec`,
`Db.insertReturning`, `Db.call`, `Db.findPage`, `Db.inTx`).

### 9.6 SQL transactions in Java

For multi-step operations we use `Db.inTx`, which:

1. opens a connection,
2. sets `autoCommit = false`,
3. runs the work,
4. commits if no exception, **rolls back** if any exception,
5. always closes the connection.

For BUY/SELL, we don't need it — the **stored procedure** wraps the work
in `BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK` itself, which is even
safer (the transaction lives where the data lives).

---

## 10. END-TO-END: A BUY TRANSACTION

Let's trace `POST /api/v1/transactions/buy` step by step. Suppose the
request is:

```json
{ "accountId": 1, "assetId": 1, "quantity": 10, "unitPrice": 180.0 }
```

```
1. Network
   The client sends an HTTP POST to http://localhost:8080/api/v1/transactions/buy
   with that JSON body.

2. Spring Boot's web server (Tomcat under the hood) receives the request.

3. Routing
   Spring sees @RequestMapping("/api/v1/transactions") + @PostMapping("/buy")
   on TransactionController and calls its buy() method.

4. JSON -> DTO
   Jackson converts the JSON body into a TradeRequest record (immutable, validated).
   @Valid runs the constraints: quantity >= 1, unitPrice > 0.0001, etc.
   On failure -> 400 Bad Request via GlobalExceptionHandler.

5. Controller -> Service
   TransactionController.buy(req) calls TransactionService.buy(req).

6. Service preconditions
   - accountDao.findById(1)  -> if null, throw AccountNotFoundException -> 404.
   - assetDao.findById(1)    -> if null, throw AssetNotFoundException   -> 404.

7. Service -> DAO -> Stored procedure
   transactionDao.executeBuyProcedure(1, 1, 10, 180.0) calls Db.call("{call sp_buy_asset(?, ?, ?, ?)}", ...)
   which under the hood opens a CallableStatement.

8. Inside SQL Server (sp_buy_asset)
   - Lock the accounts row (UPDLOCK, HOLDLOCK).
   - Verify account exists.
   - Verify asset exists.
   - Verify cash_balance >= quantity * unit_price.
     If any check fails -> RAISERROR with code (e.g. 'INSUFFICIENT_BALANCE').
     Java's JdbcSqlExceptionMapper sees the message and re-throws as
     InsufficientBalanceException -> GlobalExceptionHandler -> 409 Conflict.
   - BEGIN TRANSACTION
       UPDATE accounts SET cash_balance = cash_balance - 1800
       INSERT INTO transactions (...) VALUES ('BUY', 10, 180)
       INSERT INTO audit_logs (...) VALUES ('BUY_TRANSACTION_CREATED')
   - COMMIT.

9. Service builds the response
   transactionDao.findLatest(...) reads back the just-inserted row.
   TransactionMapper.toResponse(...) converts it to TransactionResponse.

10. JSON serialization
    Jackson turns the record into JSON.

11. HTTP response
    HTTP/1.1 201 Created
    Content-Type: application/json
    { "id": 17, "accountId": 1, "assetId": 1, "transactionType": "BUY",
      "quantity": 10, "unitPrice": 180.0, "totalAmount": 1800.0,
      "currency": "USD", "transactionDate": "2026-05-09T17:25:00Z" }
```

This is the single most important flow in the project. Everything else is
a variation of it.

---

## 11. THE RISK SCORE, EXPLAINED STEP BY STEP

The risk endpoint is `GET /api/v1/accounts/{id}/risk`.

### 11.1 What we're trying to compute

A single number from 0 to 100 (and a `RiskLevel` label) that says: "how
risky is this whole portfolio?". Risky here means: how much do its assets
**move around in price**.

### 11.2 The strategy interface

[`RiskCalculationStrategy`](src/main/java/com/finrisk/strategy/risk/RiskCalculationStrategy.java)
defines three pure functions:

- `sigmaFromPrices(List<BigDecimal>)` — given a chronological list of
  prices, return the volatility σ.
- `levelForSigma(double σ)` — bucket σ into LOW/MEDIUM/HIGH/VERY_HIGH.
- `fallbackLevel(Asset)` — when there isn't enough history, fall back to
  the asset's default (Stock=HIGH, ETF=MEDIUM, Bond=LOW, Crypto=VERY_HIGH).

### 11.3 The volatility implementation

```19:42:src/main/java/com/finrisk/strategy/risk/VolatilityRiskStrategy.java
    public double sigmaFromPrices(List<BigDecimal> chronologicalAscending) {
        if (chronologicalAscending == null || chronologicalAscending.size() < 2) {
            return Double.NaN;
        }
        int n = chronologicalAscending.size() - 1;
        double sum = 0;
        double[] r = new double[n];
        for (int i = 1; i < chronologicalAscending.size(); i++) {
            BigDecimal p0 = chronologicalAscending.get(i - 1);
            BigDecimal p1 = chronologicalAscending.get(i);
            double ri =
                    Math.log(p1.divide(p0, MathContext.DECIMAL128).doubleValue());
            r[i - 1] = ri;
            sum += ri;
        }
        double mean = sum / n;
        double varSum = 0;
        for (double v : r) {
            double d = v - mean;
            varSum += d * d;
        }
        double variance = n <= 1 ? 0 : varSum / (n - 1);
        return Math.sqrt(variance);
    }
```

In plain words:

1. Take consecutive price pairs and compute the **log return** `r_i =
   ln(price_today / price_yesterday)`. Log returns are the standard way
   to measure price moves because they're additive and symmetric.
2. Compute the **average** of those returns.
3. Compute the **variance**: average squared distance from the mean.
4. **σ = sqrt(variance)** — the standard deviation.

### 11.4 The bucketing

```45:59:src/main/java/com/finrisk/strategy/risk/VolatilityRiskStrategy.java
    public RiskLevel levelForSigma(double dailySigma) {
        if (Double.isNaN(dailySigma)) {
            return RiskLevel.MEDIUM;
        }
        if (dailySigma < 0.01) {
            return RiskLevel.LOW;
        }
        if (dailySigma < 0.03) {
            return RiskLevel.MEDIUM;
        }
        if (dailySigma < 0.06) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.VERY_HIGH;
    }
```

So a stock with σ = 2% per day is `MEDIUM`. A coin with σ = 8% per day is
`VERY_HIGH`.

### 11.5 Portfolio-level score

`RiskService` (which uses the strategy) does this for each holding:

1. Read the last 30 prices from `asset_price_history`.
2. Ask the strategy for σ.
3. If too few samples, use the asset's `calculateRiskLevel()` fallback
   (e.g. `MEDIUM` for ETF).
4. **Weight** each holding by its current dollar value:

   ```
   portfolio_sigma = sum( holding_value * holding_sigma ) / sum( holding_value )
   ```

5. Map the weighted σ back through `levelForSigma(...)` to get the final
   `RiskLevel`.
6. Return both the numeric score and the level (and a per-holding
   breakdown for transparency).

### 11.6 Why use the Strategy pattern here?

Because risk formulas change. Tomorrow we may want `VaR` (Value-at-Risk),
or beta-vs-S&P500, or simply "use the asset's hardcoded label". With the
Strategy in place, we just write a new class and swap the `@Bean` in
`StrategyConfig` — `RiskService` doesn't change at all.

---

## 12. THE REST API

### 12.1 What is REST?

REST is a style for designing web APIs:

- The URL identifies a **resource** (`/users/1`, `/accounts/3/portfolio`).
- The HTTP **verb** says what to do: `GET` (read), `POST` (create),
  `PUT` (replace), `DELETE` (remove).
- Responses use standard HTTP **status codes**: 200 OK, 201 Created,
  400 Bad Request, 404 Not Found, 409 Conflict, 500 Internal Server Error.
- Data is exchanged as JSON.

### 12.2 Versioning

Every endpoint lives under `/api/v1/...`. If we ever ship a breaking
change, we'll add `/api/v2/...` so old clients keep working.

### 12.3 Pagination

Every list endpoint accepts:

- `?page=0` (zero-indexed)
- `?size=20` (max 100)
- `?sort=createdAt,desc` (repeatable for multi-key sort)

…and returns the same envelope:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false,
  "content": [ ... ]
}
```

This is the `Page<T>` record.

### 12.4 Errors

Every error returns the same shape:

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Cash balance is too low to complete this trade",
  "details": { ... optional ... }
}
```

`code` is a **stable string** the client can switch on. It will not
change over time. The full list is enumerated in `openapi.yaml`.

`GlobalExceptionHandler` is the single class that maps a thrown
exception to a status + code.

### 12.5 The full endpoint list

(Source of truth is [`openapi.yaml`](openapi.yaml).)

```
USERS
POST   /api/v1/users
GET    /api/v1/users
GET    /api/v1/users/{id}
GET    /api/v1/users/{userId}/accounts

ACCOUNTS
POST   /api/v1/accounts
GET    /api/v1/accounts/{id}
POST   /api/v1/accounts/{id}/deposit
POST   /api/v1/accounts/{id}/withdraw

ASSETS
POST   /api/v1/assets
GET    /api/v1/assets
GET    /api/v1/assets/{id}
PUT    /api/v1/assets/{id}/price
GET    /api/v1/assets/{id}/price-history

TRANSACTIONS
POST   /api/v1/transactions/buy
POST   /api/v1/transactions/sell
GET    /api/v1/accounts/{accountId}/transactions

PORTFOLIO
GET    /api/v1/accounts/{accountId}/portfolio
GET    /api/v1/accounts/{accountId}/profit-loss
GET    /api/v1/accounts/{accountId}/risk
```

---

## 13. DOCKER AND HOW IT ALL RUNS

### 13.1 What is Docker?

Docker packages an app together with everything it needs (runtime,
libraries, config) into a **container**. A container runs the same way
on any machine that has Docker.

### 13.2 What is `docker-compose`?

`docker-compose.yml` describes **multiple** containers and how they talk
to each other. We have two:

- **`sqlserver`** — Microsoft's official SQL Server 2022 image. Listens
  on port `1433`. Has a healthcheck that runs `SELECT 1` until the DB is
  up.
- **`finrisk-api`** — our app. Built from `Dockerfile` (multi-stage:
  `maven:3.9-eclipse-temurin-21` to compile, then `eclipse-temurin:21-jre`
  to run). Depends on `sqlserver` being healthy. Listens on container
  port `8080`, mapped to host port `18080` (to avoid clashing with other
  things on the host).

### 13.3 How to run everything

```bash
docker compose up -d sqlserver
bash db/scripts/apply-migrations.sh FinRiskDB
bash db/scripts/apply-migrations.sh FinRiskDB_Test
docker compose up -d --build finrisk-api
curl http://localhost:18080/api/v1/users   # works!
```

Or just one command:

```bash
bash scripts/e2e.sh
```

…which does the whole dance plus runs all tests plus runs the happy-path
integration script.

---

## 14. TESTING STRATEGY (TDD)

We followed **Test-Driven Development**: write the test first, see it
fail, write code, see it pass, refactor. Diagram of the loop:

```
Write test  ->  Run: red  ->  Write minimum code  ->  Run: green  ->  Refactor
```

### 14.1 Two test layers

- **Unit tests** (`src/test/java/.../*Test.java`, run by `mvn test`):
  fast. Mock collaborators with Mockito. Cover Services, Controllers
  (`@WebMvcTest`), Mappers, Factories, the Volatility strategy.
- **Integration tests** (`*IT.java`, run by `mvn verify`): slower. Use a
  **real** SQL Server (`FinRiskDB_Test` database). Cover JDBC DAOs and
  full HTTP-to-DB roundtrips via RestAssured.

### 14.2 The end-to-end script

[`scripts/e2e.sh`](scripts/e2e.sh) does, in order:

1. `docker compose up -d sqlserver`
2. Wait for SQL Server to be healthy.
3. Apply migrations to `FinRiskDB` and `FinRiskDB_Test`.
4. `mvn test` (unit, must be green).
5. `mvn verify` (integration, must be green).
6. Seed data.
7. Build + run `finrisk-api`.
8. Run [`scripts/e2e-happy-path.sh`](scripts/e2e-happy-path.sh): create
   user → create account → deposit → create stock → update price → buy 10
   → check portfolio → sell 4 → check P&L → check risk.
9. Tear down.

A green `e2e.sh` is the final acceptance gate.

---

## 15. LIKELY JURY QUESTIONS AND CRISP ANSWERS

**Q: Why no JPA / Hibernate?**
A: The brief explicitly demonstrates raw JDBC ceremony (Connection,
PreparedStatement, ResultSet, try-with-resources). JPA would hide all of
that. We kept JDBC visible but factored its boilerplate into one helper
class, so each DAO method stays short and the requirement is still
demonstrably present.

**Q: Why Spring Boot if you're not using JPA?**
A: For the web layer (`@RestController`, JSON, validation, dependency
injection) only. We disabled JPA. Spring Boot saved us from writing a
custom HTTP server.

**Q: Where exactly is the Singleton pattern?**
A: `config/DatabaseConnection.java`. Private constructor, static
`getDataSource()` with double-checked locking. Exactly one HikariCP pool
exists for the whole app.

**Q: Show me Polymorphism in your code.**
A: `Asset.calculateRiskLevel()` returns `HIGH` for `Stock`, `MEDIUM` for
`ETF`, `LOW` for `Bond`, `VERY_HIGH` for `CryptoAsset`. The same method
name behaves differently depending on the runtime type. Look at any
`Asset` implementing record's `@Override public RiskLevel
calculateRiskLevel()`.

**Q: What stops SQL injection?**
A: Every query uses `?` placeholders bound via `PreparedStatement`. The
driver sends SQL and values separately, so user input is never parsed as
SQL. Sort fields go through a column whitelist (`SqlSort`).

**Q: What if the buy fails halfway through?**
A: It cannot. The stored procedure `sp_buy_asset` wraps the cash update,
the `transactions` insert, and the `audit_logs` insert in a single SQL
transaction (`BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK`). Either all
three happen or none of them happen.

**Q: Two parallel buys arrive at the same time on the same account. What
happens?**
A: The stored procedure takes a row lock (`UPDLOCK, HOLDLOCK`) on the
account row before checking the cash balance. The second buy waits for
the first to commit, then re-evaluates the (now lower) balance. They
cannot both succeed using the same dollar.

**Q: Why a stored procedure instead of doing it in Java?**
A: Atomicity (single SQL transaction), one network round-trip instead of
three, and the safety rule lives next to the data — even a different
client app couldn't accidentally write a half-trade.

**Q: How is the risk level computed?**
A: For each holding we read the last 30 prices from
`asset_price_history`, compute log returns, then their standard deviation
σ. We bucket σ into `LOW` (<1%) / `MEDIUM` (<3%) / `HIGH` (<6%) /
`VERY_HIGH`. The portfolio score is the value-weighted average of
per-asset σ. If a holding has fewer than the minimum samples, we fall
back to the asset's default risk level.

**Q: Why the Strategy pattern for risk?**
A: So we can swap algorithms (volatility today, VaR tomorrow) without
touching `RiskService` or any other code. We just change which `@Bean`
implementation is wired in `StrategyConfig`.

**Q: Why the Factory pattern?**
A: When the API receives a polymorphic `AssetCreateRequest`, we need to
build the correct subtype (`Stock` vs `ETF` vs `Bond` vs `CryptoAsset`).
The factory hides that decision behind one method call. Modern `switch`
on a sealed interface makes the compiler enforce that we handle every
case.

**Q: Why DTOs separate from domain models?**
A: So changing the database schema doesn't break the API contract, and
vice versa. Mappers translate between them.

**Q: Why genericity in `GenericDao<T, ID>`?**
A: So every DAO doesn't repeat the same `findById` / `findAll` / `save`
/ `update` / `delete` signatures. Each DAO just extends
`GenericDao<MyEntity, MyIdType>` and inherits the contract.

**Q: How is the project tested?**
A: Two layers. **Unit tests** with Mockito, run by Surefire on every
`mvn test`. **Integration tests** against a real SQL Server
(`FinRiskDB_Test`), run by Failsafe on `mvn verify`. Plus an end-to-end
shell script that exercises the running container with curl/jq.

**Q: How would you add a 5th asset type — e.g. "REAL_ESTATE"?**
A: Five touch points: (1) add `REAL_ESTATE` to the `AssetType` enum and
the `assets.asset_type` CHECK constraint. (2) Add `RealEstate` record
implementing `Asset`. (3) Add it to the `permits` list of the `Asset`
sealed interface. (4) Add a branch to `AssetFactory.create(...)` and
`withTimestamps(...)` — the compiler will complain until you do, because
the switch is exhaustive. (5) Optional: add `asset_details_real_estate`
table for type-specific fields. The Strategy, the controllers, the
service layer, and the API spec scale automatically.

**Q: Why USD only?**
A: Scope. Multi-currency would mean adding an `FX_RATES` table, currency
columns on every monetary field, conversion logic in the service layer.
We chose to do one currency well rather than three currencies poorly.

**Q: What happens if SQL Server goes down?**
A: HikariCP's connection acquisition will time out → DAO throws
`DaoException` → `GlobalExceptionHandler` returns
`500 Internal Server Error` with `code: "INTERNAL_ERROR"`. The
`docker-compose` healthcheck on the `sqlserver` service plus the
`depends_on` keeps the API from starting until the DB is healthy.

**Q: Where is the `audit_logs` table used?**
A: The stored procedures `sp_buy_asset` and `sp_sell_asset` insert one
row per trade. It's append-only and used for traceability — a real
financial app would forward this to a SIEM/monitoring system.

**Q: Why are the models records?**
A: Records give us encapsulation (private final fields + accessors),
constructor, `equals`/`hashCode`/`toString` for free. They are immutable
by design, which removes a whole class of "I mutated something I
shouldn't have" bugs. They also play well with Jackson and the validation
framework.
