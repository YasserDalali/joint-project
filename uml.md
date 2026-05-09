# FinRisk API — UML & Architecture Diagrams

This document captures the design of the **FinRisk** API before any code is
written. It is meant to be reviewed alongside [`openapi.yaml`](openapi.yaml).

> Stack assumption: **Java + JDBC + SQL Server**, layered as
> `controller -> service -> dao -> SQL Server`.

---

## 1. Domain overview

The system models a user's investment portfolio. Each user owns one or more
investment accounts. Accounts hold cash and execute buy/sell transactions
against assets (stocks, ETFs, bonds, crypto). The API derives holdings,
portfolio value, profit/loss, and a risk score from the transaction history.

```mermaid
flowchart TD
    User --> Account
    Account --> Transaction
    Transaction --> BuyTransaction
    Transaction --> SellTransaction
    Account --> Holding
    Holding --> Asset
    Asset --> Stock
    Asset --> ETF
    Asset --> Bond
    Asset --> CryptoAsset
```

---

## 2. Package architecture

```mermaid
flowchart LR
    subgraph controller [com.finrisk.controller]
        UserController
        AccountController
        AssetController
        TransactionController
        PortfolioController
    end

    subgraph service [com.finrisk.service]
        UserService
        AccountService
        TransactionService
        PortfolioService
        RiskService
    end

    subgraph dao [com.finrisk.dao]
        GenericDao
        UserDao
        AccountDao
        AssetDao
        TransactionDao
        subgraph daoImpl [com.finrisk.dao.impl]
            UserDaoJdbc
            AccountDaoJdbc
            AssetDaoJdbc
            TransactionDaoJdbc
        end
    end

    subgraph model [com.finrisk.model]
        UserModel[User]
        AccountModel[Account]
        AssetModel[Asset abstract]
        Stock
        ETF
        Bond
        CryptoAsset
        TransactionModel[Transaction abstract]
        BuyTransaction
        SellTransaction
    end

    subgraph dto [com.finrisk.dto]
        RequestDto[request/*]
        ResponseDto[response/*]
    end

    subgraph mapper [com.finrisk.mapper]
        UserMapper
        AccountMapper
        AssetMapper
        TransactionMapper
        PortfolioMapper
    end

    subgraph factory [com.finrisk.factory]
        AssetFactory
        TransactionFactory
    end

    subgraph strategy [com.finrisk.strategy.risk]
        RiskCalculationStrategy
        VolatilityRiskStrategy
    end

    subgraph exception [com.finrisk.exception]
        DaoException
        AccountNotFoundException
        InsufficientQuantityException
        InvalidTransactionException
    end

    subgraph config [com.finrisk.config]
        DatabaseConnection
    end

    controller --> service
    controller --> dto
    controller --> mapper
    service --> dao
    service --> factory
    service --> strategy
    daoImpl --> config
    daoImpl --> model
    service --> model
    service --> exception
    dao --> exception
    mapper --> model
    mapper --> dto
    factory --> model
```

Rules:

- Controllers contain **no SQL** and **no business rules**.
- DAOs contain **no business rules** — only persistence + mapping.
- Services own all financial rules (validating buys/sells, computing portfolio
  value, computing risk).

---

## 3. Class diagram — domain model (inheritance)

```mermaid
classDiagram
    class Asset {
        <<abstract>>
        -Long id
        -String symbol
        -String name
        -BigDecimal currentPrice
        -RiskLevel riskLevel
        +getId() Long
        +getSymbol() String
        +getName() String
        +getCurrentPrice() BigDecimal
        +calculateRiskLevel()* RiskLevel
        +getAssetType()* AssetType
    }

    class Stock {
        -String sector
        -String exchange
        +calculateRiskLevel() RiskLevel
        +getAssetType() AssetType
    }

    class ETF {
        -String issuer
        -BigDecimal expenseRatio
        +calculateRiskLevel() RiskLevel
        +getAssetType() AssetType
    }

    class Bond {
        -BigDecimal interestRate
        -LocalDate maturityDate
        -String issuer
        +calculateRiskLevel() RiskLevel
        +getAssetType() AssetType
    }

    class CryptoAsset {
        -String blockchain
        +calculateRiskLevel() RiskLevel
        +getAssetType() AssetType
    }

    Asset <|-- Stock
    Asset <|-- ETF
    Asset <|-- Bond
    Asset <|-- CryptoAsset

    class Transaction {
        <<abstract>>
        -Long id
        -Long accountId
        -Long assetId
        -int quantity
        -BigDecimal unitPrice
        -LocalDateTime transactionDate
        +getTotalAmount() BigDecimal
        +getTransactionType()* TransactionType
        +applyTo(Account, Asset)*
    }

    class BuyTransaction {
        +getTransactionType() TransactionType
        +applyTo(Account, Asset)
    }

    class SellTransaction {
        +getTransactionType() TransactionType
        +applyTo(Account, Asset)
    }

    Transaction <|-- BuyTransaction
    Transaction <|-- SellTransaction

    class User {
        -Long id
        -String fullName
        -String email
        -LocalDateTime createdAt
    }

    class Account {
        -Long id
        -Long userId
        -String accountName
        -BigDecimal cashBalance
        -LocalDateTime createdAt
        +deposit(BigDecimal)
        +withdraw(BigDecimal)
    }

    class RiskLevel {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        VERY_HIGH
    }

    class AssetType {
        <<enumeration>>
        STOCK
        ETF
        BOND
        CRYPTO
    }

    class TransactionType {
        <<enumeration>>
        BUY
        SELL
    }

    User "1" --> "*" Account : owns
    Account "1" --> "*" Transaction : executes
    Transaction "*" --> "1" Asset : references
```

This satisfies: **encapsulation**, **abstraction**, **inheritance**,
**polymorphism** (`calculateRiskLevel`, `applyTo`), and **`@Override`** in
child classes.

---

## 4. Class diagram — DAO layer (genericity + interfaces)

```mermaid
classDiagram
    class GenericDao~T, ID~ {
        <<interface>>
        +findById(ID id) T
        +findAll() List~T~
        +save(T entity) void
        +update(T entity) void
        +delete(ID id) void
    }

    class UserDao {
        <<interface>>
        +findByEmail(String email) User
    }

    class AccountDao {
        <<interface>>
        +findByUserId(Long userId) List~Account~
        +updateCashBalance(Long id, BigDecimal newBalance) void
    }

    class AssetDao {
        <<interface>>
        +findBySymbol(String symbol) Asset
        +findByType(AssetType type) List~Asset~
    }

    class TransactionDao {
        <<interface>>
        +findByAccountId(Long accountId) List~Transaction~
        +getOwnedQuantity(Long accountId, Long assetId) int
        +saveBuy(BuyTransaction tx) void
        +saveSell(SellTransaction tx) void
    }

    GenericDao <|-- UserDao
    GenericDao <|-- AccountDao
    GenericDao <|-- AssetDao
    GenericDao <|-- TransactionDao

    class UserDaoJdbc
    class AccountDaoJdbc
    class AssetDaoJdbc
    class TransactionDaoJdbc

    UserDao <|.. UserDaoJdbc
    AccountDao <|.. AccountDaoJdbc
    AssetDao <|.. AssetDaoJdbc
    TransactionDao <|.. TransactionDaoJdbc

    class DatabaseConnection {
        +getConnection() Connection
    }

    UserDaoJdbc ..> DatabaseConnection
    AccountDaoJdbc ..> DatabaseConnection
    AssetDaoJdbc ..> DatabaseConnection
    TransactionDaoJdbc ..> DatabaseConnection
```

This satisfies: **DAO architecture**, **interfaces**, **genericity**,
**reusability**.

---

## 5. Class diagram — service layer

```mermaid
classDiagram
    class UserService {
        -UserDao userDao
        +createUser(User u) User
        +getUser(Long id) User
    }

    class AccountService {
        -AccountDao accountDao
        +createAccount(Account a) Account
        +getAccount(Long id) Account
        +listForUser(Long userId) List~Account~
    }

    class TransactionService {
        -AccountDao accountDao
        -AssetDao assetDao
        -TransactionDao transactionDao
        +buy(Long accountId, Long assetId, int qty, BigDecimal price) Transaction
        +sell(Long accountId, Long assetId, int qty, BigDecimal price) Transaction
    }

    class PortfolioService {
        -TransactionDao transactionDao
        -AssetDao assetDao
        +getHoldings(Long accountId) List~Holding~
        +getTotalValue(Long accountId) BigDecimal
        +getProfitLoss(Long accountId) ProfitLoss
    }

    class RiskService {
        -AssetDao assetDao
        -TransactionDao transactionDao
        -AssetPriceHistoryDao priceHistoryDao
        -RiskCalculationStrategy riskStrategy
        +computeRiskScore(Long accountId) RiskScore
    }

    TransactionService ..> AccountDao
    TransactionService ..> AssetDao
    TransactionService ..> TransactionDao
    PortfolioService ..> TransactionDao
    PortfolioService ..> AssetDao
    RiskService ..> AssetDao
    RiskService ..> TransactionDao
    RiskService ..> AssetPriceHistoryDao
    RiskService ..> RiskCalculationStrategy
```

---

## 6. Custom exceptions

```mermaid
classDiagram
    class Exception {
        <<JDK>>
    }

    class DaoException {
        +DaoException(String, Throwable)
    }

    class AccountNotFoundException {
        +AccountNotFoundException(String)
    }

    class InsufficientBalanceException {
        +InsufficientBalanceException(String)
    }

    class InsufficientQuantityException {
        +InsufficientQuantityException(String)
    }

    class InvalidTransactionException {
        +InvalidTransactionException(String)
    }

    Exception <|-- DaoException
    Exception <|-- AccountNotFoundException
    Exception <|-- InsufficientBalanceException
    Exception <|-- InsufficientQuantityException
    Exception <|-- InvalidTransactionException
```

---

## 6.5. Design patterns used

A small, deliberate set of patterns — chosen so each one earns its place
without overcomplicating the project.

| Pattern             | Where it lives                                 | Why                                                                                                         |
| ------------------- | ---------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| **DAO**             | `dao/*` package                                | Required by the brief; isolates persistence from business logic.                                            |
| **Generic DAO**     | `GenericDao<T, ID>`                            | Removes duplication across UserDao / AccountDao / AssetDao / TransactionDao.                                |
| **Singleton**       | `DatabaseConnection`                           | Single shared `DataSource` / connection pool, lazily initialized and thread-safe.                           |
| **Template Method** | `Asset` (`calculateRiskLevel`, `getAssetType`) | Parent class fixes the algorithm shape; subclasses fill the steps.                                          |
| **Factory Method**  | `AssetFactory`, `TransactionFactory`           | Translates a typed request (e.g. `assetType=BOND`) into the right concrete subclass without `if/else` soup. |
| **Strategy**        | `RiskCalculationStrategy`                      | Lets us swap volatility-based risk for a different formula (e.g. fixed-mapping) without touching services.  |
| **DTO + Mapper**    | `dto/*`, `mapper/*`                            | API contracts (request/response) are decoupled from domain models; mappers do the translation.              |

### Factory Method — Asset & Transaction creation

```mermaid
classDiagram
    class AssetFactory {
        +create(AssetCreateRequest req) Asset
    }

    class Asset {
        <<abstract>>
    }

    AssetFactory ..> Asset : creates
    AssetFactory ..> Stock : creates STOCK
    AssetFactory ..> ETF : creates ETF
    AssetFactory ..> Bond : creates BOND
    AssetFactory ..> CryptoAsset : creates CRYPTO

    class TransactionFactory {
        +create(TradeRequest req, TransactionType type) Transaction
    }

    class Transaction {
        <<abstract>>
    }

    TransactionFactory ..> Transaction : creates
    TransactionFactory ..> BuyTransaction : creates BUY
    TransactionFactory ..> SellTransaction : creates SELL
```

Pseudocode:

```java
public final class AssetFactory {
    public static Asset create(AssetCreateRequest req) {
        return switch (req.getAssetType()) {
            case STOCK  -> new Stock(req.getSymbol(), req.getName(), req.getCurrentPrice(),
                                     req.getStockDetails().getSector(),
                                     req.getStockDetails().getExchange());
            case ETF    -> new ETF(req.getSymbol(), req.getName(), req.getCurrentPrice(),
                                   req.getEtfDetails().getIssuer(),
                                   req.getEtfDetails().getExpenseRatio());
            case BOND   -> new Bond(req.getSymbol(), req.getName(), req.getCurrentPrice(),
                                    req.getBondDetails().getInterestRate(),
                                    req.getBondDetails().getMaturityDate(),
                                    req.getBondDetails().getIssuer());
            case CRYPTO -> new CryptoAsset(req.getSymbol(), req.getName(), req.getCurrentPrice(),
                                           req.getCryptoDetails().getBlockchain());
        };
    }
}
```

### Strategy — risk calculation (volatility-based)

```mermaid
classDiagram
    class RiskCalculationStrategy {
        <<interface>>
        +scoreFor(Asset asset, List~AssetPricePoint~ history) double
        +levelFor(double score) RiskLevel
    }

    class VolatilityRiskStrategy {
        -int minSamples
        +scoreFor(Asset, List~AssetPricePoint~) double
        +levelFor(double) RiskLevel
    }

    class FixedMappingRiskStrategy {
        +scoreFor(Asset, List~AssetPricePoint~) double
        +levelFor(double) RiskLevel
    }

    RiskCalculationStrategy <|.. VolatilityRiskStrategy
    RiskCalculationStrategy <|.. FixedMappingRiskStrategy

    class RiskService {
        -RiskCalculationStrategy strategy
        +computeRiskScore(Long accountId) RiskScore
    }

    RiskService o--> RiskCalculationStrategy
```

Algorithm in `VolatilityRiskStrategy`:

1. Pull the last N price points from `asset_price_history` for the asset
   (default N = 30).
2. Compute log returns `r_i = ln(p_i / p_(i-1))`.
3. `volatility = stddev(r_i)`.
4. Map volatility -> `RiskLevel`:

   | Daily volatility (sigma) | RiskLevel   |
   | ------------------------ | ----------- |
   | sigma < 0.01             | `LOW`       |
   | 0.01 <= sigma < 0.03     | `MEDIUM`    |
   | 0.03 <= sigma < 0.06     | `HIGH`      |
   | sigma >= 0.06            | `VERY_HIGH` |

5. If history has fewer than `minSamples` (default 5), fall back to the
   asset subclass's `calculateRiskLevel()` (Template Method default).

Portfolio-level score in `RiskService`:

```text
score = sum_over_holdings( holding.currentValue * holding.assetVolatility )
        / sum_over_holdings( holding.currentValue )
level = strategy.levelFor(score)
```

This keeps `RiskService` ignorant of *how* risk is computed — only that a
strategy exists.

### Singleton — `DatabaseConnection`

```java
public final class DatabaseConnection {
    private static volatile DataSource INSTANCE;

    private DatabaseConnection() {}

    public static DataSource getDataSource() {
        if (INSTANCE == null) {
            synchronized (DatabaseConnection.class) {
                if (INSTANCE == null) {
                    INSTANCE = buildHikariDataSource();
                }
            }
        }
        return INSTANCE;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
```

### DTO + Mapper

```mermaid
flowchart LR
    Client -->|JSON| Controller
    Controller -->|"DTO (request)"| Mapper1[Mapper]
    Mapper1 -->|Domain| Service
    Service -->|Domain| Mapper2[Mapper]
    Mapper2 -->|"DTO (response)"| Controller
    Controller -->|JSON| Client
    Service --> DAO --> SQL[(SQL Server)]
```

- `dto/request/*` — what comes in over HTTP (e.g. `TradeRequest`,
  `AssetCreateRequest`).
- `dto/response/*` — what goes out (e.g. `TransactionResponse`,
  `PortfolioResponse`).
- `mapper/*` — pure functions translating both directions.
- Domain models in `model/*` stay free of Jackson annotations; this keeps
  the persistence/business core independent of the HTTP layer.

---

## 7. ER diagram — SQL Server schema

```mermaid
erDiagram
    users ||--o{ accounts : owns
    accounts ||--o{ transactions : executes
    assets ||--o{ transactions : referenced_by
    assets ||--o| asset_details_stock : "1:1 (if STOCK)"
    assets ||--o| asset_details_bond : "1:1 (if BOND)"
    assets ||--o| asset_details_crypto : "1:1 (if CRYPTO)"
    assets ||--o{ asset_price_history : has_history

    users {
        BIGINT id PK
        NVARCHAR full_name
        NVARCHAR email UK
        DATETIME2 created_at
    }

    accounts {
        BIGINT id PK
        BIGINT user_id FK
        NVARCHAR account_name
        DECIMAL cash_balance "CHECK >= 0"
        DATETIME2 created_at
    }

    assets {
        BIGINT id PK
        NVARCHAR symbol UK
        NVARCHAR name
        NVARCHAR asset_type "STOCK|ETF|BOND|CRYPTO"
        DECIMAL current_price "CHECK > 0"
        NVARCHAR risk_level "LOW|MEDIUM|HIGH|VERY_HIGH"
        DATETIME2 created_at
    }

    asset_details_stock {
        BIGINT asset_id PK_FK
        NVARCHAR sector
        NVARCHAR exchange_name
    }

    asset_details_bond {
        BIGINT asset_id PK_FK
        DECIMAL interest_rate
        DATE maturity_date
        NVARCHAR issuer
    }

    asset_details_crypto {
        BIGINT asset_id PK_FK
        NVARCHAR blockchain
    }

    transactions {
        BIGINT id PK
        BIGINT account_id FK
        BIGINT asset_id FK
        NVARCHAR transaction_type "BUY|SELL"
        INT quantity "CHECK > 0"
        DECIMAL unit_price "CHECK > 0"
        DATETIME2 transaction_date
    }

    asset_price_history {
        BIGINT id PK
        BIGINT asset_id FK
        DECIMAL price "CHECK > 0"
        DATETIME2 recorded_at
    }

    audit_logs {
        BIGINT id PK
        NVARCHAR entity_name
        BIGINT entity_id
        NVARCHAR action_type
        NVARCHAR description
        DATETIME2 created_at
    }
```

---

## 8. Sequence diagram — `POST /api/transactions/buy`

```mermaid
sequenceDiagram
    participant Client
    participant TC as TransactionController
    participant TS as TransactionService
    participant AD as AccountDao
    participant AsD as AssetDao
    participant TD as TransactionDao
    participant DB as SQL Server

    Client->>TC: POST /api/transactions/buy {accountId, assetId, qty, price}
    TC->>TS: buy(accountId, assetId, qty, price)
    TS->>AD: findById(accountId)
    AD->>DB: SELECT * FROM accounts WHERE id = ?
    DB-->>AD: account row
    AD-->>TS: Account
    TS->>AsD: findById(assetId)
    AsD->>DB: SELECT * FROM assets WHERE id = ?
    DB-->>AsD: asset row
    AsD-->>TS: Asset

    alt cash_balance < qty * price
        TS-->>TC: throw InsufficientBalanceException
        TC-->>Client: 409 Conflict
    else sufficient funds
        TS->>DB: BEGIN TRANSACTION
        TS->>AD: updateCashBalance(accountId, balance - total)
        TS->>TD: saveBuy(BuyTransaction)
        TS->>DB: INSERT audit_logs (...)
        TS->>DB: COMMIT
        TS-->>TC: Transaction
        TC-->>Client: 201 Created
    end
```

---

## 9. Sequence diagram — `GET /api/accounts/{id}/portfolio`

```mermaid
sequenceDiagram
    participant Client
    participant PC as PortfolioController
    participant PS as PortfolioService
    participant TD as TransactionDao
    participant AsD as AssetDao
    participant DB as SQL Server

    Client->>PC: GET /api/accounts/1/portfolio
    PC->>PS: getHoldings(1)
    PS->>DB: SELECT * FROM vw_portfolio_holdings WHERE account_id = 1
    Note over PS,DB: View aggregates BUY/SELL into net quantity
    DB-->>PS: holding rows
    PS->>AsD: load asset details for each holding
    AsD->>DB: SELECT FROM assets + asset_details_*
    DB-->>AsD: Asset (Stock/ETF/Bond/CryptoAsset)
    AsD-->>PS: List~Asset~
    PS-->>PC: Portfolio { holdings, totalValue }
    PC-->>Client: 200 OK
```

---

## 10. State diagram — Transaction lifecycle

```mermaid
stateDiagram-v2
    [*] --> Requested
    Requested --> Validating : controller forwards to service
    Validating --> Rejected : InsufficientBalance / InsufficientQuantity / InvalidTransaction
    Validating --> Persisting : checks pass
    Persisting --> Failed : SQL error / DaoException
    Persisting --> Recorded : COMMIT
    Recorded --> [*]
    Rejected --> [*]
    Failed --> [*]
```

---

## 11. Requirement traceability

| Requirement                | Where it shows up in this design                                                  |
| -------------------------- | --------------------------------------------------------------------------------- |
| Encapsulation              | All model classes in section 3 — private fields, getters/setters                  |
| Abstraction                | `Asset` and `Transaction` are abstract                                            |
| Inheritance                | `Stock`, `ETF`, `Bond`, `CryptoAsset` extend `Asset`; Buy/Sell extend Transaction |
| Polymorphism + `@Override` | `calculateRiskLevel()`, `applyTo()` overridden per subtype                        |
| Interfaces                 | `GenericDao`, `UserDao`, `AccountDao`, `AssetDao`, `TransactionDao`, `RiskCalculationStrategy` |
| Genericity                 | `GenericDao<T, ID>`                                                               |
| Layered packages           | Section 2: controller / service / dao / model / dto / mapper / factory / strategy / exception / config |
| DAO architecture           | Section 4: interfaces + JDBC implementations                                      |
| JDBC                       | `DatabaseConnection`, `Connection`, `PreparedStatement`, `ResultSet`              |
| SQL injection prevention   | All DAO queries use `?` placeholders                                              |
| ResultSet mapping          | `mapResultSetToUser`, `mapResultSetToAsset`, `mapResultSetToTransaction`          |
| Custom exceptions          | Section 6 hierarchy                                                               |
| SQL skills                 | ER diagram (constraints), views, stored procs, indexes (see openapi + DDL)        |
| Transactional integrity    | Section 8 sequence diagram (BEGIN/COMMIT around buy)                              |
| Dockerization              | `sqlserver` + `finrisk-api` Compose services                                      |
| Design patterns            | Section 6.5: DAO, Generic DAO, Singleton, Template Method, Factory Method, Strategy, DTO + Mapper |
| Volatility-based risk      | `VolatilityRiskStrategy` (section 6.5) + `asset_price_history` table              |
| API versioning             | All paths under `/api/v1` in `openapi.yaml`                                       |
| Pagination                 | `Page<T>` envelope + `page` / `size` / `sort` query params on all list endpoints  |

---

## 12. Confirmed design decisions

| Topic              | Decision                                                                                                                                  |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Authentication     | **None.** API is open. Authn/authz is explicitly out of scope.                                                                            |
| Currency           | **USD only.** All `cashBalance`, `unitPrice`, and `currentPrice` values are USD. No FX conversion logic.                                  |
| Risk score formula | **Volatility-based.** `RiskService` derives the risk level of each holding from the standard deviation of returns in `asset_price_history`. The portfolio risk is the value-weighted average of per-asset volatility. |
| ETF default risk   | `ETF.calculateRiskLevel()` returns **`MEDIUM`** when there is insufficient price history.                                                 |
| Pagination         | **Enabled on all list endpoints** (`page`, `size`, optional `sort`). Responses use a generic `Page<T>` envelope.                          |
| API versioning     | All endpoints live under **`/api/v1`**.                                                                                                   |
| API contracts      | **DTOs are separate from domain models.** Mappers translate `Domain <-> DTO`. Domain models never leak directly into JSON.                |
