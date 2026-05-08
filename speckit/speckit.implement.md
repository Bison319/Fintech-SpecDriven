# SpecKit Implementation Guide — Fintech Transaction & Wallet Platform

> How to implement each service, code structure, design patterns, and database schemas.

---

## 1. Design Patterns

### 1.1 Hexagonal Architecture (Ports & Adapters)

Each service follows hexagonal architecture to decouple business logic from infrastructure:

```
           ┌──────────────────────────────────────────┐
           │            APPLICATION LAYER              │
           │  (Controllers, DTOs — Inbound Adapters)   │
           └──────────────────┬───────────────────────┘
                              │
           ┌──────────────────▼───────────────────────┐
           │              DOMAIN LAYER                 │
           │  (Entities, Services, Repository Ports)   │
           │  ★ No framework dependencies ★            │
           └──────────────────┬───────────────────────┘
                              │
           ┌──────────────────▼───────────────────────┐
           │          INFRASTRUCTURE LAYER             │
           │  (JPA Repos, Feign Clients, Config —      │
           │   Outbound Adapters)                      │
           └──────────────────────────────────────────┘
```

### 1.2 Domain-Driven Design Patterns Used

| Pattern                    | Where Used                                    |
| -------------------------- | --------------------------------------------- |
| **Aggregate Root**         | `Wallet` (owns balance state)                 |
| **Value Object**           | `Money(amount, currency)`                     |
| **Domain Event**           | `TransactionCompleted`, `FraudAlertRaised`    |
| **Repository Pattern**     | All data access via interfaces                |
| **Service Layer**          | Business logic in domain services             |
| **Anti-Corruption Layer**  | Feign clients wrap external service contracts |

### 1.3 Infrastructure Patterns

| Pattern                | Implementation                                |
| ---------------------- | --------------------------------------------- |
| **API Gateway**        | Spring Cloud Gateway — single entry point     |
| **Circuit Breaker**    | Resilience4j on all Feign clients             |
| **Retry**              | Resilience4j retry with exponential backoff   |
| **Idempotent Consumer**| Idempotency key table with unique constraints |
| **Optimistic Locking** | `@Version` on Wallet entity                   |
| **Event Publishing**   | RabbitMQ via Spring AMQP                      |
| **Structured Logging** | Logback with JSON encoder + MDC               |

---

## 2. Database Schemas

### 2.1 User Service Database (`users_db`)

```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
```

### 2.2 Wallet Service Database (`wallets_db`)

```sql
CREATE TABLE wallets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    balance     DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency    VARCHAR(3) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, currency)
);

CREATE INDEX idx_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_status ON wallets (status);

CREATE TABLE wallet_audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    action          VARCHAR(20) NOT NULL,
    amount          DECIMAL(19,4) NOT NULL,
    balance_before  DECIMAL(19,4) NOT NULL,
    balance_after   DECIMAL(19,4) NOT NULL,
    description     VARCHAR(500),
    reference_id    UUID,
    correlation_id  UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallet_audit_wallet_id ON wallet_audit_log (wallet_id);
CREATE INDEX idx_wallet_audit_created_at ON wallet_audit_log (created_at);
```

### 2.3 Transaction Service Database (`transactions_db`)

```sql
CREATE TABLE transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_wallet_id  UUID NOT NULL,
    target_wallet_id  UUID,
    type              VARCHAR(20) NOT NULL,
    amount            DECIMAL(19,4) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key   UUID NOT NULL,
    fraud_check_result VARCHAR(20),
    description       VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP,
    UNIQUE (idempotency_key)
);

CREATE INDEX idx_txn_source_wallet ON transactions (source_wallet_id);
CREATE INDEX idx_txn_target_wallet ON transactions (target_wallet_id);
CREATE INDEX idx_txn_status ON transactions (status);
CREATE INDEX idx_txn_created_at ON transactions (created_at);
CREATE INDEX idx_txn_idempotency ON transactions (idempotency_key);

CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body   JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
    UNIQUE (idempotency_key, operation)
);

CREATE INDEX idx_idempotency_key ON idempotency_keys (idempotency_key, operation);
CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
```

### 2.4 Fraud Service Database (`fraud_db`)

```sql
CREATE TABLE fraud_checks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    wallet_id       UUID NOT NULL,
    transaction_amount DECIMAL(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    risk_score      INTEGER NOT NULL DEFAULT 0,
    decision        VARCHAR(20) NOT NULL,
    reasons         TEXT[],
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_user_id ON fraud_checks (user_id);
CREATE INDEX idx_fraud_wallet_id ON fraud_checks (wallet_id);
CREATE INDEX idx_fraud_created_at ON fraud_checks (created_at);
CREATE INDEX idx_fraud_decision ON fraud_checks (decision);
```

---

## 3. Service Implementation Details

### 3.1 User Service

**Entry point:** `UserServiceApplication.java`

**Flow:**
```
Request → UserController → UserService → UserRepository → PostgreSQL
```

**Key implementation notes:**
- Email uniqueness enforced at DB level (UNIQUE constraint) + application level.
- Soft delete: `deactivateUser()` sets status to `INACTIVE`, does not delete row.
- Phone validation uses E.164 regex pattern.

### 3.2 Wallet Service

**Entry point:** `WalletServiceApplication.java`

**Flow (create):**
```
Request → WalletController → WalletService → UserServiceClient (verify user)
                                            → WalletRepository → PostgreSQL
```

**Flow (credit/debit):**
```
Request → WalletController → WalletService → WalletRepository.findById()
                                            → Validate balance (debit only)
                                            → Update balance + version
                                            → Insert audit log
                                            → Return updated wallet
```

**Key implementation notes:**
- Optimistic locking via `@Version` annotation on Wallet entity.
- On `OptimisticLockingFailureException`, retry up to 3 times with 100ms delay.
- One wallet per (userId, currency) — enforced by unique constraint.
- All balance operations use `BigDecimal` with `DECIMAL(19,4)` precision.

### 3.3 Transaction Service

**Entry point:** `TransactionServiceApplication.java`

**Flow:**
```
Request → TransactionController
            │
            ├─1→ Check Idempotency Key (local DB)
            │     ├─ Exists: return cached response
            │     └─ New: continue
            │
            ├─2→ FraudServiceClient.evaluate() [Circuit Breaker]
            │     ├─ REJECTED: save failed txn, return 422
            │     └─ APPROVED/REVIEW: continue
            │
            ├─3→ WalletServiceClient.debit() [Circuit Breaker + Retry]
            │     ├─ Success: continue
            │     └─ Failure: save failed txn, return error
            │
            ├─4→ Save transaction as COMPLETED
            │
            ├─5→ Store idempotency key + response
            │
            └─6→ Publish TransactionCompleted event to RabbitMQ
```

**Key implementation notes:**
- Idempotency check is the FIRST step — before any side effects.
- Fraud check fallback: if Fraud Service is down, APPROVE with `riskScore=0` and log warning.
- Circuit breaker on Wallet Service: if open, return 503 Service Unavailable.
- Transaction status machine: `PENDING → COMPLETED | FAILED | REVERSED`.

### 3.4 Fraud Service

**Entry point:** `FraudServiceApplication.java`

**Flow (sync check):**
```
Request → FraudController → FraudDetectionService → Apply Rules
                                                   → Calculate Risk Score
                                                   → Make Decision
                                                   → Store FraudCheck
                                                   → Return Result
```

**Fraud Rules:**

| Rule                      | Condition                        | Score Impact |
| ------------------------- | -------------------------------- | ------------ |
| High Amount               | Amount > $10,000                 | +40          |
| Very High Amount          | Amount > $50,000                 | +30 (total 70) |
| Velocity Check            | > 10 transactions in last minute | +50          |
| High Velocity             | > 5 transactions in last minute  | +25          |
| New Account               | Account < 24 hours old           | +15          |

**Decision Matrix:**

| Risk Score | Decision    |
| ---------- | ----------- |
| 0 - 30     | APPROVED    |
| 31 - 70    | REVIEW      |
| 71 - 100   | REJECTED    |

---

## 4. API Gateway Implementation

**Technology:** Spring Cloud Gateway

**Route Configuration:**
```yaml
routes:
  - id: user-service
    uri: http://user-service:8081
    predicates:
      - Path=/api/v1/users/**
  - id: wallet-service
    uri: http://wallet-service:8082
    predicates:
      - Path=/api/v1/wallets/**
  - id: transaction-service
    uri: http://transaction-service:8083
    predicates:
      - Path=/api/v1/transactions/**
  - id: fraud-service
    uri: http://fraud-service:8084
    predicates:
      - Path=/api/v1/fraud/**
```

**Filters:**
1. **CorrelationIdFilter** — generates/propagates `X-Correlation-Id` header.
2. **RequestLoggingFilter** — logs all inbound requests with timing.
3. **RateLimitFilter** — (future) token bucket per client IP.

---

## 5. Event Schema

### TransactionCompleted Event

```json
{
  "eventType": "TransactionCompleted",
  "eventId": "uuid",
  "timestamp": "2026-04-22T10:00:00Z",
  "payload": {
    "transactionId": "uuid",
    "sourceWalletId": "uuid",
    "targetWalletId": "uuid",
    "userId": "uuid",
    "type": "DEBIT",
    "amount": 150.00,
    "currency": "USD",
    "status": "COMPLETED"
  }
}
```

---

## 6. Cross-Cutting Concerns

### 6.1 Correlation ID Propagation

```
Client → Gateway (generate ID) → Service A (pass via header) → Service B (pass via header)
                                      ↓                              ↓
                                  MDC.put("correlationId")      MDC.put("correlationId")
                                      ↓                              ↓
                                  Log with ID                   Log with ID
```

### 6.2 Error Handling Strategy

Each service has a `GlobalExceptionHandler` that catches:
- `MethodArgumentNotValidException` → 400 with field-level errors
- `EntityNotFoundException` → 404
- `DuplicateKeyException` → 409
- `OptimisticLockingFailureException` → 409
- `InsufficientBalanceException` → 422
- `FraudRejectedException` → 422
- `Exception` → 500 (generic fallback, log full stack trace)

---

*This implementation guide should be read alongside the specification (speckit.specify.md) and plan (speckit.plan.md).*
