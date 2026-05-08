# FINTECH TRANSACTION & WALLET PLATFORM - COMPLETE PROJECT DOCUMENTATION

**Project Generated:** April 22, 2026  
**Version:** 1.0.0-SNAPSHOT  
**License:** MIT

---

## TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Project Vision & Goals](#project-vision--goals)
3. [Technical Architecture](#technical-architecture)
4. [Technology Stack](#technology-stack)
5. [System Components](#system-components)
6. [Engineering Principles](#engineering-principles)
7. [Data Architecture](#data-architecture)
8. [Communication Patterns](#communication-patterns)
9. [Project Structure](#project-structure)
10. [Deployment & Infrastructure](#deployment--infrastructure)
11. [API Overview](#api-overview)
12. [Development Workflow](#development-workflow)
13. [Key Design Decisions (ADRs)](#key-design-decisions-adrs)
14. [Implementation Patterns](#implementation-patterns)
15. [Testing Strategy](#testing-strategy)
16. [Security Architecture](#security-architecture)
17. [Observability & Monitoring](#observability--monitoring)
18. [Roadmap & Status](#roadmap--status)

---

## EXECUTIVE SUMMARY

**Fintech Transaction & Wallet Platform** is a production-grade microservices platform built for financial transaction processing and wallet management. The system processes high-volume transactions with guarantees on idempotency, fault tolerance, fraud detection, and complete audit trails.

### Key Capabilities

- **User Management**: Registration, profile management, account lifecycle
- **Wallet Operations**: Create wallets, credit/debit balance with strong consistency
- **Transaction Processing**: Orchestrated payments with fraud checks, idempotency, and resilience
- **Fraud Detection**: Rule-based engine with velocity checks, amount thresholds, and risk scoring
- **Audit Logging**: Immutable audit trail for all state-changing operations
- **Resilience**: Circuit breakers, retry logic, and graceful degradation
- **Event-Driven**: Asynchronous processing via RabbitMQ for fraud analysis and auditing

### Why This Platform Matters

Financial systems demand:
- **Safety**: No duplicate transactions, never lose a transaction
- **Scalability**: Independent service scaling, zero downtime deployments
- **Observability**: Complete traceability of every transaction
- **Resilience**: Tolerate partial failures without cascading failures
- **Auditability**: Immutable records for compliance and investigation

This platform addresses all these requirements using industry best practices in microservices architecture, domain-driven design, and distributed systems.

---

## PROJECT VISION & GOALS

### Vision

Build a **bank-grade, spec-driven fintech platform** that can scale to handle millions of transactions daily while maintaining strong consistency on critical operations and eventual consistency across service boundaries.

### Goals

| Goal                     | How Achieved                                          |
| ------------------------ | ----------------------------------------------------- |
| **Correctness**          | Idempotency keys, optimistic locking, strong testing |
| **Performance**          | Async events, caching, connection pooling             |
| **Scalability**          | Microservices, database per service, stateless design |
| **Reliability**          | Circuit breakers, retries, fallbacks, health checks   |
| **Auditability**         | Immutable audit logs, correlation IDs, structured logs |
| **Maintainability**      | Clean code, separation of concerns, comprehensive docs |
| **Developer Experience** | Docker Compose for local dev, OpenAPI docs, clear APIs |

---

## TECHNICAL ARCHITECTURE

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                 │
│              (Web App, Mobile App, Third-party APIs)                │
└──────────────────────────┬──────────────────────────────────────────┘
                           │ HTTPS/TLS
┌──────────────────────────▼──────────────────────────────────────────┐
│                  API GATEWAY (:8080)                                 │
│      Spring Cloud Gateway + JWT Validation + Request Routing        │
│              Rate Limiting + Correlation ID Propagation             │
└───┬──────────┬──────────────┬───────────────┬──────────────┬────────┘
    │          │              │               │              │
    ▼          ▼              ▼               ▼              ▼
┌────────┐ ┌─────────┐ ┌────────────┐ ┌───────────┐ ┌────────────┐
│ User   │ │ Wallet  │ │Transaction │ │  Fraud    │ │  Audit     │
│Service │ │ Service │ │  Service   │ │  Service  │ │  Service   │
│ :8081  │ │  :8082  │ │   :8083    │ │   :8084   │ │  :8085     │
└───┬────┘ └──┬──────┘ └──┬──────┬──┘ └─────┬─────┘ └────────────┘
    │         │           │      │          │
    │         │    ╔══════╝      │          │
    │         │    ║ Feign       │          │
    │         │    ║ (Sync)      │          │
    ▼         ▼    ▼             ▼          │
┌────────┐ ┌─────────┐ ┌────────────┐ ┌───────────┐
│users   │ │wallets  │ │transactions│ │ fraud_db  │
│  _db   │ │  _db    │ │    _db     │ │           │
└────────┘ └─────────┘ └─────┬──────┘ └───────────┘
                              │
                    ┌─────────▼──────────┐
                    │     RabbitMQ       │
                    │  (Event Broker)    │
                    │ AMQP 0.9.1         │
                    └─────────┬──────────┘
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
        ┌──────────┐  ┌────────────┐  ┌──────────────┐
        │  Fraud   │  │   Audit    │  │ Notification │
        │ Consumer │  │  Consumer  │  │   (Future)   │
        └──────────┘  └────────────┘  └──────────────┘
```

### Architecture Style

**Microservices with API Gateway Pattern** — Each service owns its domain, database, and API surface. An API gateway provides a single entry point, handles cross-cutting concerns (authentication, correlation IDs, rate limiting), and routes requests to appropriate services.

### Key Principles

1. **Domain Ownership**: Each service controls its own database and business logic
2. **Independence**: Services can be deployed, scaled, and updated independently
3. **Resilience**: Partial failures don't cascade — circuit breakers and timeouts prevent cascading failures
4. **Eventual Consistency**: Cross-service data is eventually consistent via events
5. **Observability**: Every request has a correlation ID for tracing across services
6. **Auditability**: All state changes are logged immutably

---

## TECHNOLOGY STACK

| Layer                  | Technology                          | Version       | Purpose                              |
| ---------------------- | ----------------------------------- | ------------- | ------------------------------------ |
| **Language**           | Java                                | 17 LTS        | Type-safe, high performance          |
| **Framework**          | Spring Boot                         | 3.2.5         | Rapid microservices development      |
| **Cloud**              | Spring Cloud                        | 2023.0.1      | Distributed systems patterns         |
| **API Gateway**        | Spring Cloud Gateway                | 4.0.1         | Request routing, rate limiting       |
| **Service Discovery**  | Embedded (Docker DNS)               | Docker        | Service-to-service communication    |
| **Configuration**      | Spring Boot Config + Environment    | Native        | 12-Factor compliance                 |
| **Database**           | PostgreSQL                          | 16            | ACID compliance, JSON support        |
| **ORM**                | Spring Data JPA + Hibernate         | Latest        | Object-relational mapping            |
| **Migrations**         | Flyway                              | Latest        | Database versioning                  |
| **Messaging**          | RabbitMQ                            | 3.13          | Reliable event-driven communication  |
| **Resilience**         | Resilience4j                        | 2.2.0         | Circuit breaker, retry, timeout     |
| **Service Calls**      | OpenFeign                           | Latest        | Declarative REST client              |
| **API Documentation** | SpringDoc OpenAPI                   | 2.5.0         | Interactive API docs (Swagger UI)    |
| **Validation**         | Bean Validation (Jakarta)           | 3.0           | Input validation framework           |
| **Testing**            | JUnit 5 + Mockito + AssertJ         | Latest        | Unit & integration tests             |
| **In-Memory DB**       | H2                                  | Latest        | Testing, no external dependencies    |
| **Containerization**   | Docker                              | Latest        | Container images per service         |
| **Orchestration**      | Docker Compose                      | 3.9           | Local development & testing          |
| **Build Tool**         | Maven                               | 3.9           | Multi-module project management      |
| **Logging**            | Logback + JSON Encoder              | Latest        | Structured JSON logging              |
| **Monitoring**         | Spring Boot Actuator + Micrometer   | Latest        | Health checks, metrics               |

---

## SYSTEM COMPONENTS

### 1. API Gateway (Port 8080)

**Spring Cloud Gateway**

**Responsibilities:**
- Route incoming requests to appropriate services
- Validate JWTs and authenticate requests
- Propagate `X-Correlation-Id` header for distributed tracing
- Implement rate limiting (future)
- Add security headers
- Handle CORS

**Technologies:**
- Spring Cloud Gateway 4.0.1
- JWT validation
- Request/Response filtering

**Health Endpoint:** `http://localhost:8080/actuator/health`

---

### 2. User Service (Port 8081)

**Purpose:** User registration, profile management, and account lifecycle

**Responsibilities:**
- User registration and validation
- Profile retrieval and updates
- Account activation/deactivation
- Email uniqueness validation
- Password management (basic)

**Domain Model:**
```
User (Aggregate Root)
├── id: UUID
├── email: String (unique)
├── firstName: String
├── lastName: String
├── phoneNumber: String
├── status: ACTIVE | INACTIVE | SUSPENDED
├── createdAt: ZonedDateTime
├── updatedAt: ZonedDateTime
└── version: Long (for optimistic locking)
```

**API Endpoints:**
- `POST /api/v1/users` — Register new user
- `GET /api/v1/users/{id}` — Get user by ID
- `GET /api/v1/users` — List users (paginated)
- `PUT /api/v1/users/{id}` — Update user profile
- `PATCH /api/v1/users/{id}/status` — Activate/deactivate user

**Dependencies:**
- Spring Data JPA
- Spring Web
- Spring Validation
- PostgreSQL Driver

**Database:** `users_db` (PostgreSQL)

**Key Features:**
- Email uniqueness validation
- Pagination support
- OpenAPI documentation
- Health checks and metrics

**Health Endpoint:** `http://localhost:8081/actuator/health`

**API Docs:** `http://localhost:8081/swagger-ui.html`

---

### 3. Wallet Service (Port 8082)

**Purpose:** Wallet lifecycle management and balance operations with strong consistency

**Responsibilities:**
- Wallet creation for users
- Balance queries
- Credit operations (deposits)
- Debit operations (withdrawals)
- Balance mutations with optimistic locking
- Audit trail for all balance changes

**Domain Model:**
```
Wallet (Aggregate Root)
├── id: UUID
├── userId: UUID
├── balance: BigDecimal
├── currency: String (default: USD)
├── status: ACTIVE | CLOSED | SUSPENDED
├── createdAt: ZonedDateTime
├── updatedAt: ZonedDateTime
└── version: Long (Optimistic Locking)

WalletAuditLog (Value Object)
├── id: UUID
├── walletId: UUID
├── previousBalance: BigDecimal
├── newBalance: BigDecimal
├── operation: CREDIT | DEBIT
├── transactionId: UUID
├── correlationId: String
└── timestamp: ZonedDateTime
```

**API Endpoints:**
- `POST /api/v1/wallets` — Create wallet for user
- `GET /api/v1/wallets/{id}` — Get wallet details
- `GET /api/v1/wallets/user/{userId}` — Get user's wallets
- `POST /api/v1/wallets/{id}/credit` — Credit wallet
- `POST /api/v1/wallets/{id}/debit` — Debit wallet

**Request/Response Example:**
```json
// Debit Request
{
  "amount": 50.00,
  "currency": "USD",
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "description": "Payment for transaction TX-123"
}

// Debit Response (200 OK)
{
  "status": "success",
  "wallet": {
    "id": "wallet-123",
    "balance": 950.00,
    "version": 2
  },
  "auditLog": {
    "previousBalance": 1000.00,
    "newBalance": 950.00,
    "operation": "DEBIT",
    "timestamp": "2026-04-22T10:15:30Z"
  }
}
```

**Dependencies:**
- Spring Data JPA
- Spring Web
- Spring Cloud OpenFeign (calls User Service)
- PostgreSQL Driver

**Database:** `wallets_db` (PostgreSQL)

**Key Features:**
- **Optimistic Locking**: `@Version` annotation ensures concurrent balance updates don't create race conditions
- **Audit Trail**: Every balance change recorded immutably
- **Strong Consistency**: ACID transactions guarantee accurate balance
- **Feign Client**: Integrates with User Service to validate user exists
- **Error Handling**: 
  - `OptimisticLockingFailureException` → Retry logic
  - `InsufficientFundsException` → 400 Bad Request
  - `WalletNotFoundException` → 404

**Health Endpoint:** `http://localhost:8082/actuator/health`

**API Docs:** `http://localhost:8082/swagger-ui.html`

---

### 4. Transaction Service (Port 8083)

**Purpose:** Orchestrate financial transactions with fraud checks, idempotency, and resilience

**Responsibilities:**
- Process transactions (payments) between wallets
- Enforce idempotency using client-provided keys
- Orchestrate fraud checks
- Maintain transaction state machine
- Publish transaction events
- Handle distributed failures gracefully

**Domain Model:**
```
Transaction (Aggregate Root)
├── id: UUID
├── fromWalletId: UUID
├── toWalletId: UUID
├── amount: BigDecimal
├── currency: String
├── status: INITIATED | FRAUD_CHECK_PENDING | FRAUD_APPROVED | 
│            DEBITED | CREDITED | COMPLETED | FAILED | REVERSED
├── fraudCheckResult: FraudCheckResult
├── idempotencyKey: String (unique)
├── errorCode: String (if failed)
├── errorMessage: String
├── createdAt: ZonedDateTime
├── completedAt: ZonedDateTime
└── version: Long

IdempotencyRecord
├── id: UUID
├── idempotencyKey: String (unique)
├── operationType: String
├── requestHash: String
├── responsePayload: JSON
├── expiresAt: ZonedDateTime (24 hours)
└── createdAt: ZonedDateTime
```

**API Endpoints:**
- `POST /api/v1/transactions` — Initiate transaction
- `GET /api/v1/transactions/{id}` — Get transaction status
- `GET /api/v1/transactions` — List transactions (paginated)
- `GET /api/v1/transactions/wallet/{walletId}` — Transactions for wallet

**Request/Response Example:**
```json
// POST /api/v1/transactions
{
  "fromWalletId": "wallet-123",
  "toWalletId": "wallet-456",
  "amount": 100.00,
  "currency": "USD",
  "idempotencyKey": "tx-20260422-001",
  "description": "Payment to John Doe",
  "correlationId": "corr-12345"
}

// Successful Response (201 Created)
{
  "id": "tx-550e8400-e29b-41d4",
  "status": "COMPLETED",
  "fromWalletId": "wallet-123",
  "toWalletId": "wallet-456",
  "amount": 100.00,
  "fraudCheckResult": {
    "riskScore": 0.15,
    "approved": true,
    "rules": ["VELOCITY_OK", "AMOUNT_OK", "NEW_DESTINATION_WARNING"]
  },
  "createdAt": "2026-04-22T10:15:30Z",
  "completedAt": "2026-04-22T10:15:35Z"
}

// Idempotent Response (202 Accepted — already processing)
{
  "status": "ALREADY_PROCESSING",
  "message": "Transaction with this idempotency key is already being processed",
  "transactionId": "tx-550e8400-e29b-41d4"
}

// Duplicate Response (200 OK — return cached result)
{
  "status": "CACHED",
  "message": "Returning cached result for this idempotency key",
  "previousResult": { /* ... */ }
}
```

**Transaction State Machine:**
```
INITIATED
    ↓
FRAUD_CHECK_PENDING ←── (async to Fraud Service)
    ↓
FRAUD_APPROVED (if risk acceptable)
    ↓
DEBITED (wallet1 balance decreased)
    ↓
CREDITED (wallet2 balance increased)
    ↓
COMPLETED (success)

Alternative flows:
INITIATED → FRAUD_REJECTED → FAILED
INITIATED → ERROR (validation) → FAILED
DEBITED → ERROR (wallet2 unavailable) → REVERSED (refund wallet1)
```

**Resilience Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        intervalFunction: exponential_backoff
        
  timelimit:
    configs:
      default:
        timeoutDuration: 5s
```

**Dependencies:**
- Spring Data JPA
- Spring Web
- Spring Cloud OpenFeign (calls Wallet & Fraud Services)
- Spring AMQP (publish events)
- Resilience4j (circuit breaker, retry, timeout)
- Spring AOP (aspect-oriented programming)
- PostgreSQL Driver

**Database:** `transactions_db` (PostgreSQL)

**Key Features:**
- **Idempotency**: Client-provided keys prevent duplicate transactions
- **Distributed Orchestration**: Coordinates calls to Wallet and Fraud services
- **Resilience**: Circuit breakers on external calls, retry with exponential backoff
- **Event Publishing**: Publishes `TransactionCompleted` and `TransactionFailed` events to RabbitMQ
- **Audit Trail**: Complete transaction history with state transitions
- **Error Handling**:
  - Idempotency key collision → return cached result or 202 Accepted
  - Fraud check failure → 403 Forbidden
  - Insufficient funds → 400 Bad Request
  - Service unavailable → 503 Service Unavailable (with fallback)

**Health Endpoint:** `http://localhost:8083/actuator/health`

**API Docs:** `http://localhost:8083/swagger-ui.html`

---

### 5. Fraud Service (Port 8084)

**Purpose:** Rule-based fraud detection and risk scoring

**Responsibilities:**
- Analyze transactions for fraud risk
- Calculate risk scores based on rules
- Maintain fraud history and patterns
- Publish fraud alerts for suspicious activities
- Consume transaction events asynchronously
- Store fraud analysis results

**Fraud Rules:**

| Rule                  | Description                                 | Threshold |
| --------------------- | ------------------------------------------- | --------- |
| **Velocity Check**    | Max transactions per user in 24 hours       | 100 tx    |
| **Amount Threshold**  | Max single transaction amount               | $10,000   |
| **New Destination**   | First transaction to new wallet             | Warning   |
| **Unusual Time**      | Transaction outside typical hours (UTC)     | 00-05     |
| **Geographic Anomaly**| New country/IP (future)                     | Warning   |
| **Repeat Offender**   | User has history of fraud alerts            | Flag      |

**Domain Model:**
```
FraudAnalysis
├── id: UUID
├── transactionId: UUID
├── userId: UUID
├── riskScore: Float (0.0 - 1.0)
├── status: APPROVED | REJECTED | MANUAL_REVIEW
├── violatedRules: List<String>
├── warnings: List<String>
├── analysisTimestampMs: Long
└── timestamp: ZonedDateTime

FraudAlert
├── id: UUID
├── userId: UUID
├── alertType: HIGH_RISK | VELOCITY_EXCEEDED | REPEAT_OFFENDER
├── message: String
├── suspiciousTransactionIds: List<UUID>
├── createdAt: ZonedDateTime
└── resolvedAt: ZonedDateTime (nullable)

UserFraudHistory
├── id: UUID
├── userId: UUID
├── transactionCount: Long
├── alertCount: Long
├── lastTransactionAt: ZonedDateTime
└── lastAlertAt: ZonedDateTime
```

**API Endpoints:**
- `POST /api/v1/fraud/analyze` — Analyze transaction for fraud
- `GET /api/v1/fraud/alerts/user/{userId}` — Get user's fraud alerts
- `GET /api/v1/fraud/history/{userId}` — Get user's fraud history

**Request/Response Example:**
```json
// POST /api/v1/fraud/analyze
{
  "transactionId": "tx-123",
  "userId": "user-456",
  "fromWalletId": "wallet-123",
  "toWalletId": "wallet-456",
  "amount": 5000.00,
  "previousTransactionCount": 45,
  "isNewDestination": false,
  "correlationId": "corr-12345"
}

// Response (200 OK)
{
  "riskScore": 0.35,
  "status": "APPROVED",
  "violatedRules": [],
  "warnings": [
    "UNUSUAL_TIME",
    "NEW_DESTINATION"
  ],
  "analysisDetails": {
    "velocityCheck": "OK (45 transactions in 24h)",
    "amountThreshold": "OK ($5,000 < $10,000)",
    "newDestination": "WARNING (first transaction to this wallet)",
    "timeOfDay": "WARNING (02:15 UTC is outside typical hours)"
  },
  "recommendation": "Approve with monitoring",
  "timestamp": "2026-04-22T02:15:30Z"
}

// High Risk Response
{
  "riskScore": 0.85,
  "status": "REJECTED",
  "violatedRules": [
    "VELOCITY_EXCEEDED",
    "REPEAT_OFFENDER"
  ],
  "recommendation": "Manual review required",
  "alertId": "alert-789"
}
```

**Risk Scoring Algorithm:**
```
Base Score = 0.0

If velocity > 80% of threshold: +0.15
If amount > 80% of threshold: +0.20
If new destination: +0.10
If unusual time: +0.05
If user has fraud alerts: +0.30

Final Score = min(Base Score, 1.0)

Decision:
  score < 0.3: APPROVED
  0.3 ≤ score < 0.7: APPROVED (monitor)
  score ≥ 0.7: REJECTED (or MANUAL_REVIEW)
```

**Dependencies:**
- Spring Data JPA
- Spring Web
- Spring AMQP (consume events)
- PostgreSQL Driver

**Database:** `fraud_db` (PostgreSQL)

**Messaging:**
- **Consumes**: `transactions.completed` → Asynchronous fraud analysis
- **Publishes**: `fraud.alert.raised` → High-risk transactions

**Key Features:**
- **Rule-Based Engine**: Configurable rules with weighted scoring
- **Async Processing**: Processes events from RabbitMQ without blocking Transaction Service
- **Historical Analysis**: Tracks fraud patterns per user
- **Alert System**: Raises alerts for suspicious activities
- **Error Handling**:
  - Invalid transaction data → 400 Bad Request
  - User not found → creates new fraud history record

**Health Endpoint:** `http://localhost:8084/actuator/health`

**API Docs:** `http://localhost:8084/swagger-ui.html`

---

## ENGINEERING PRINCIPLES

### 1. Idempotency

**Every write operation** (POST, PUT, PATCH) MUST accept an `Idempotency-Key` header.

**Implementation:**
```yaml
Request Headers:
  Idempotency-Key: "550e8400-e29b-41d4-a716-446655440000"
  
Behavior:
  First request: Process normally, store result
  Duplicate request (same key): Return cached result (200 OK) or 202 Accepted
  Collision (different payload): Return 422 Unprocessable Entity
  
Storage:
  Table: idempotency_records
  Columns:
    - id: UUID
    - idempotency_key: String (unique)
    - operation_type: String
    - request_hash: String (SHA-256)
    - response_payload: JSON
    - expires_at: TIMESTAMP (24 hours from creation)
    - created_at: TIMESTAMP
```

### 2. Consistency Model

**Wallet balance operations** use **strong consistency** with optimistic locking.

```
// Spring Data JPA
@Entity
public class Wallet {
    @Id
    private UUID id;
    
    private BigDecimal balance;
    
    @Version  // ← Optimistic locking
    private Long version;
    
    // When balance changes:
    // SELECT balance, version FROM wallets WHERE id = ? FOR UPDATE
    // [check balance sufficient]
    // balance -= amount
    // version += 1
    // UPDATE wallets SET balance = ?, version = ? WHERE id = ? AND version = (old_version)
}
```

**Cross-service state** follows **eventual consistency** via domain events:
- Transaction Service publishes `TransactionCompleted` event
- Fraud Service consumes event asynchronously
- Audit Service eventually records the transaction

### 3. Observability

Every service MUST expose:
- `/actuator/health` — Service health status
- `/actuator/metrics` — Performance metrics
- `/actuator/info` — Service metadata

**Structured Logging:**
```json
{
  "timestamp": "2026-04-22T10:15:30.123Z",
  "level": "INFO",
  "logger": "com.fintech.transaction.service.TransactionService",
  "message": "Transaction processing initiated",
  "correlationId": "corr-12345",
  "sourceService": "transaction-service",
  "targetService": "wallet-service",
  "userId": "user-456",
  "transactionId": "tx-550e8400",
  "latencyMs": 245,
  "httpStatus": 201,
  "thread": "main-thread-1"
}
```

**Correlation ID Propagation:**
```
Client Request
    ↓
API Gateway adds/extracts X-Correlation-Id header
    ↓
Transaction Service receives request with X-Correlation-Id
    ↓
Transaction Service calls Wallet Service with same X-Correlation-Id
    ↓
Wallet Service propagates to Fraud Service
    ↓
All logs tagged with same correlationId for distributed tracing
```

### 4. Resilience

All external calls wrapped with **Resilience4j**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      walletServiceClient:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        
  retry:
    instances:
      walletServiceClient:
        maxAttempts: 3
        waitDuration: 500ms
        intervalFunction: exponential_backoff
        
  timelimit:
    instances:
      walletServiceClient:
        timeoutDuration: 5s
```

**Circuit Breaker States:**
```
CLOSED (normal operation)
  ↓ (failure rate exceeds threshold)
OPEN (block all calls)
  ↓ (wait duration elapsed)
HALF_OPEN (test with limited calls)
  ↓ (all test calls succeed)
CLOSED (recover)

OR

HALF_OPEN (test fails)
  ↓
OPEN (open circuit again)
```

**Retry Strategy:**
```
Attempt 1: Immediate
Attempt 2: Wait 500ms
Attempt 3: Wait 1000ms (500ms * 2)
Attempt 4: Wait 2000ms (1000ms * 2)
Max: 3 attempts (fail after 3500ms total)
```

### 5. Auditability

Every **state-changing operation** produces audit log entry:

```
AuditLog
├── timestamp: TIMESTAMP
├── userId: UUID
├── correlationId: STRING
├── action: STRING (e.g., "TRANSACTION_COMPLETED")
├── resource: STRING (e.g., "Transaction")
├── resourceId: UUID
├── previousState: JSON (null for CREATE)
├── newState: JSON
└── version: BIGINT (immutable, never deleted)
```

Example audit log entry:
```json
{
  "timestamp": "2026-04-22T10:15:35.456Z",
  "userId": "user-456",
  "correlationId": "corr-12345",
  "action": "TRANSACTION_COMPLETED",
  "resource": "Transaction",
  "resourceId": "tx-550e8400-e29b-41d4",
  "previousState": {
    "status": "DEBITED",
    "amount": 100.00,
    "fromWalletId": "wallet-123"
  },
  "newState": {
    "status": "COMPLETED",
    "amount": 100.00,
    "fromWalletId": "wallet-123",
    "toWalletId": "wallet-456",
    "completedAt": "2026-04-22T10:15:35.456Z"
  }
}
```

---

## DATA ARCHITECTURE

### Database-per-Service Pattern

Each microservice has its own PostgreSQL database (logical separation within single PostgreSQL instance for development, separate instances in production).

```
PostgreSQL Instance (Development)
├── users_db
│   ├── users (users table)
│   └── audit_logs_users
├── wallets_db
│   ├── wallets
│   ├── wallet_audit_logs
│   └── idempotency_records (wallet operations)
├── transactions_db
│   ├── transactions
│   ├── idempotency_records (transaction operations)
│   ├── transaction_audit_logs
│   └── fraud_check_results
└── fraud_db
    ├── fraud_analyses
    ├── fraud_alerts
    └── user_fraud_history
```

### Key Constraints & Indexes

**Wallet Table:**
```sql
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance DECIMAL(19,2) NOT NULL CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,  -- Optimistic locking
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_user_currency ON wallets(user_id, currency);
CREATE INDEX idx_user_id ON wallets(user_id);
```

**Idempotency Table:**
```sql
CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_payload JSONB NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_idempotency_key ON idempotency_records(idempotency_key, operation_type);
CREATE INDEX idx_expires_at ON idempotency_records(expires_at);
```

**Transaction Table:**
```sql
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    from_wallet_id UUID NOT NULL,
    to_wallet_id UUID NOT NULL,
    amount DECIMAL(19,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(30) NOT NULL,
    fraud_check_result JSONB,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    correlation_id VARCHAR(255),
    error_code VARCHAR(50),
    error_message TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_from_wallet ON transactions(from_wallet_id, created_at DESC);
CREATE INDEX idx_to_wallet ON transactions(to_wallet_id, created_at DESC);
CREATE INDEX idx_user_created ON transactions(user_id, created_at DESC);
CREATE INDEX idx_status ON transactions(status);
```

### Audit Log Table

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id UUID,
    correlation_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    previous_state JSONB,
    new_state JSONB NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Immutable: no UPDATE/DELETE allowed
-- Append-only: only INSERT allowed

CREATE INDEX idx_user_timestamp ON audit_logs(user_id, timestamp DESC);
CREATE INDEX idx_resource ON audit_logs(resource, resource_id, timestamp DESC);
CREATE INDEX idx_correlation ON audit_logs(correlation_id);
```

### Consistency & Integrity Rules

1. **Wallet Balance Invariant**: `balance >= 0` (checked with CHECK constraint)
2. **Transaction Invariant**: `amount > 0` (checked with CHECK constraint)
3. **Referential Integrity**: User must exist before creating wallet
4. **Unique Idempotency**: Same operation can only produce one result
5. **Immutable Audit**: Never UPDATE or DELETE audit logs

---

## COMMUNICATION PATTERNS

### Synchronous Communication (REST/Feign)

Used for:
- User queries
- Wallet balance checks
- Fraud analysis requests (blocking)

**Flow:**
```
1. Client → API Gateway (HTTP REST)
2. API Gateway → Transaction Service (HTTP REST)
3. Transaction Service → Wallet Service (Feign)
4. Transaction Service → Fraud Service (Feign)
5. Response propagates back through call stack
```

**Circuit Breaker Protection:**
```java
@FeignClient(
    name = "walletServiceClient",
    url = "${wallet.service.url:http://wallet-service:8082}",
    configuration = FeignClientConfiguration.class
)
@CircuitBreaker(name = "walletServiceClient")
@Retry(name = "walletServiceClient")
@Timeout(name = "walletServiceClient")
public interface WalletServiceClient {
    @PostMapping("/api/v1/wallets/{id}/debit")
    WalletResponse debitWallet(
        @PathVariable UUID id,
        @RequestBody DebitRequest request,
        @RequestHeader("X-Correlation-Id") String correlationId
    );
}
```

**Fallback Strategy:**
```java
@Fallback(WalletServiceFallback.class)
public interface WalletServiceClient { /* ... */ }

@Component
public class WalletServiceFallback implements WalletServiceClient {
    @Override
    public WalletResponse debitWallet(...) {
        // Return cached wallet state or neutral response
        // Log incident for manual investigation
        // Publish event for monitoring
        throw new ServiceUnavailableException("Wallet Service is temporarily unavailable");
    }
}
```

### Asynchronous Communication (RabbitMQ/AMQP)

Used for:
- Post-transaction fraud analysis
- Audit log persistence
- Notifications (future)

**Flow:**
```
Transaction Service
    ↓
Publishes TransactionCompleted event to RabbitMQ
    ↓
RabbitMQ exchanges and routes to queues
    ↓
Fraud Service consumes from fraud-analysis queue
    ↓
Audit Service consumes from audit-log queue
    ↓
Each service processes independently
```

**Message Format:**
```json
{
  "eventId": "evt-123",
  "eventType": "transaction.completed",
  "timestamp": "2026-04-22T10:15:35.456Z",
  "correlationId": "corr-12345",
  "data": {
    "transactionId": "tx-550e8400-e29b-41d4",
    "userId": "user-456",
    "fromWalletId": "wallet-123",
    "toWalletId": "wallet-456",
    "amount": 100.00,
    "status": "COMPLETED"
  }
}
```

**RabbitMQ Configuration:**
```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest
    
rabbitmq:
  exchanges:
    - name: transactions
      type: topic
      durable: true
  queues:
    - name: fraud-analysis
      durable: true
    - name: audit-logging
      durable: true
  bindings:
    - exchange: transactions
      queue: fraud-analysis
      routingKey: transaction.completed
    - exchange: transactions
      queue: audit-logging
      routingKey: transaction.*
```

---

## PROJECT STRUCTURE

```
fintech-spec-driven-platform/
│
├── README.md                              # Recruiter-facing overview
├── LICENSE                                # MIT License
├── pom.xml                                # Parent POM (multi-module)
│
├── speckit/                               # ← SPECIFICATION & DOCUMENTATION
│   ├── speckit.constitution.md            # Engineering principles & standards
│   ├── speckit.plan.md                    # Architecture & trade-off decisions
│   ├── speckit.specify.md                 # OpenAPI specs & API contracts
│   ├── speckit.tasks.md                   # Epic/Feature/Task breakdown
│   └── speckit.implement.md               # Implementation guide & patterns
│
├── api-gateway/                           # ← API GATEWAY SERVICE
│   ├── Dockerfile                         # Container definition
│   ├── pom.xml                            # Service POM
│   └── src/
│       ├── main/
│       │   ├── java/com/fintech/gateway/
│       │   │   ├── GatewayApplication.java
│       │   │   ├── config/
│       │   │   │   ├── GatewayConfig.java
│       │   │   │   └── RouteConfig.java
│       │   │   └── filter/
│       │   │       ├── CorrelationIdFilter.java
│       │   │       └── JwtAuthenticationFilter.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/java/...
│
├── services/                              # ← MICROSERVICES
│
│   ├── user-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/com/fintech/user/
│   │       │   ├── UserServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   └── UserController.java
│   │       │   ├── service/
│   │       │   │   └── UserService.java
│   │       │   ├── repository/
│   │       │   │   └── UserRepository.java
│   │       │   ├── entity/
│   │       │   │   └── User.java
│   │       │   ├── dto/
│   │       │   │   ├── CreateUserRequest.java
│   │       │   │   └── UserResponse.java
│   │       │   ├── exception/
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   └── config/
│   │       │       └── OpenApiConfig.java
│   │       ├── test/java/com/fintech/user/
│   │       │   ├── service/UserServiceTest.java
│   │       │   └── controller/UserControllerIntegrationTest.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/ (Flyway migrations)
│   │
│   ├── wallet-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/com/fintech/wallet/
│   │       │   ├── WalletServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   └── WalletController.java
│   │       │   ├── service/
│   │       │   │   └── WalletService.java
│   │       │   ├── repository/
│   │       │   │   ├── WalletRepository.java
│   │       │   │   └── WalletAuditLogRepository.java
│   │       │   ├── entity/
│   │       │   │   ├── Wallet.java (with @Version)
│   │       │   │   └── WalletAuditLog.java
│   │       │   ├── dto/
│   │       │   │   ├── CreditRequest.java
│   │       │   │   ├── DebitRequest.java
│   │       │   │   └── WalletResponse.java
│   │       │   ├── client/
│   │       │   │   └── UserServiceClient.java (Feign)
│   │       │   ├── exception/
│   │       │   │   ├── WalletNotFoundException.java
│   │       │   │   ├── InsufficientFundsException.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   └── config/
│   │       │       ├── OpenApiConfig.java
│   │       │       └── FeignConfig.java
│   │       ├── test/java/com/fintech/wallet/
│   │       │   ├── service/WalletServiceTest.java
│   │       │   ├── controller/WalletControllerIntegrationTest.java
│   │       │   └── OptimisticLockingConcurrencyTest.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── transaction-service/
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/com/fintech/transaction/
│   │       │   ├── TransactionServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   └── TransactionController.java
│   │       │   ├── service/
│   │       │   │   ├── TransactionService.java
│   │       │   │   └── TransactionOrchestrationService.java
│   │       │   ├── repository/
│   │       │   │   ├── TransactionRepository.java
│   │       │   │   └── IdempotencyRecordRepository.java
│   │       │   ├── entity/
│   │       │   │   ├── Transaction.java
│   │       │   │   └── IdempotencyRecord.java
│   │       │   ├── client/
│   │       │   │   ├── WalletServiceClient.java (Feign)
│   │       │   │   └── FraudServiceClient.java (Feign)
│   │       │   ├── dto/
│   │       │   │   ├── TransactionRequest.java
│   │       │   │   └── TransactionResponse.java
│   │       │   ├── event/
│   │       │   │   └── TransactionEventPublisher.java
│   │       │   ├── exception/
│   │       │   │   ├── IdempotencyKeyCollisionException.java
│   │       │   │   ├── DuplicateTransactionException.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   └── config/
│   │       │       ├── OpenApiConfig.java
│   │       │       ├── FeignConfig.java
│   │       │       ├── Resilience4jConfig.java
│   │       │       └── RabbitMqConfig.java
│   │       ├── test/java/com/fintech/transaction/
│   │       │   ├── service/TransactionServiceTest.java
│   │       │   ├── controller/TransactionControllerIntegrationTest.java
│   │       │   └── idempotency/IdempotencyTest.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   └── fraud-service/
│       ├── Dockerfile
│       ├── pom.xml
│       └── src/
│           ├── main/java/com/fintech/fraud/
│           │   ├── FraudServiceApplication.java
│           │   ├── controller/
│           │   │   └── FraudController.java
│           │   ├── service/
│           │   │   ├── FraudAnalysisService.java
│           │   │   └── FraudRuleEngine.java
│           │   ├── repository/
│           │   │   ├── FraudAnalysisRepository.java
│           │   │   └── UserFraudHistoryRepository.java
│           │   ├── entity/
│           │   │   ├── FraudAnalysis.java
│           │   │   ├── FraudAlert.java
│           │   │   └── UserFraudHistory.java
│           │   ├── consumer/
│           │   │   └── TransactionEventConsumer.java
│           │   ├── dto/
│           │   │   ├── FraudAnalysisRequest.java
│           │   │   └── FraudAnalysisResponse.java
│           │   ├── event/
│           │   │   ├── TransactionCompletedEvent.java
│           │   │   └── FraudAlertEvent.java
│           │   ├── exception/
│           │   │   └── GlobalExceptionHandler.java
│           │   └── config/
│           │       ├── OpenApiConfig.java
│           │       └── RabbitMqConfig.java
│           ├── test/java/com/fintech/fraud/
│           │   ├── service/FraudRuleEngineTest.java
│           │   └── consumer/TransactionEventConsumerTest.java
│           └── resources/
│               └── application.yml
│
├── infra/                                 # ← INFRASTRUCTURE
│   ├── docker-compose.yml                 # Full-stack local environment
│   └── postgres/
│       ├── init.sql                       # PostgreSQL initialization
│       └── init-multiple-dbs.sh           # Create 4 databases
│
├── docs/                                  # ← DOCUMENTATION
│   ├── architecture.md                    # System architecture overview
│   └── adr/                               # Architecture Decision Records
│       ├── 001-microservices-architecture.md
│       └── 002-idempotency-strategy.md
│
└── .gitignore                             # Git ignore rules
```

---

## DEPLOYMENT & INFRASTRUCTURE

### Local Development Setup

**Prerequisites:**
- Docker & Docker Compose
- Java 17 JDK
- Maven 3.9+
- Git

**Start Full Stack:**
```bash
cd infra/
docker-compose up -d

# Wait for all services to be healthy
docker-compose ps
```

**Service Health Checks:**
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# User Service
curl http://localhost:8081/actuator/health

# Wallet Service
curl http://localhost:8082/actuator/health

# Transaction Service
curl http://localhost:8083/actuator/health

# Fraud Service
curl http://localhost:8084/actuator/health

# RabbitMQ Management
# Visit: http://localhost:15672 (guest/guest)
```

**Database Access:**
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U fintech -d users_db

# View all databases
\l

# View wallet schema
\c wallets_db
\dt
\d wallets
```

### Docker Compose Stack

```yaml
version: '3.9'

services:
  # PostgreSQL (4 logical databases)
  postgres:
    image: postgres:16-alpine
    ports: 5432:5432
    environment:
      POSTGRES_USER: fintech
      POSTGRES_PASSWORD: fintech_secret
      POSTGRES_MULTIPLE_DATABASES: users_db,wallets_db,transactions_db,fraud_db

  # RabbitMQ Message Broker
  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    ports:
      - 5672:5672
      - 15672:15672

  # API Gateway
  api-gateway:
    build: ../api-gateway
    ports: 8080:8080
    depends_on: [postgres, rabbitmq]

  # User Service
  user-service:
    build: ../services/user-service
    ports: 8081:8081
    depends_on: [postgres]

  # Wallet Service
  wallet-service:
    build: ../services/wallet-service
    ports: 8082:8082
    depends_on: [postgres]

  # Transaction Service
  transaction-service:
    build: ../services/transaction-service
    ports: 8083:8083
    depends_on: [postgres, rabbitmq, wallet-service, fraud-service]

  # Fraud Service
  fraud-service:
    build: ../services/fraud-service
    ports: 8084:8084
    depends_on: [postgres, rabbitmq]
```

### Production Deployment Considerations

**Kubernetes Manifests:**
- Separate namespace for fintech platform
- ConfigMaps for service configuration
- Secrets for database credentials, JWT keys
- StatefulSets for stateful components (if any)
- Deployments for stateless services
- PersistentVolumes for PostgreSQL data
- Services & Ingress for routing
- HorizontalPodAutoscalers for auto-scaling
- NetworkPolicies for service-to-service communication

**Monitoring & Logging:**
- Prometheus scrapes `/actuator/metrics` from each service
- Grafana dashboards for visualization
- ELK Stack (Elasticsearch, Logstash, Kibana) for centralized logging
- Jaeger for distributed tracing (correlationId integration)
- AlertManager for critical incidents

**Database Strategy:**
- Separate PostgreSQL instances per service (not logical separation)
- Read replicas for transaction service (read-heavy)
- Automated backups with point-in-time recovery
- Replication lag monitoring

---

## API OVERVIEW

### Base URL
- **Local Development**: `http://localhost:8080`
- **Production**: `https://api.fintech.example.com`

### Authentication
All requests (except `/health`) require JWT Bearer token:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Headers
```
Content-Type: application/json
Accept: application/json
X-Correlation-Id: corr-12345 (optional, generated if not provided)
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000 (for POST/PUT/PATCH)
```

### Error Responses

All errors follow standard format:
```json
{
  "timestamp": "2026-04-22T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input: amount must be greater than 0",
  "path": "/api/v1/transactions",
  "correlationId": "corr-12345"
}
```

### Common HTTP Status Codes

| Code | Meaning                                              |
| ---- | ---------------------------------------------------- |
| 200  | OK — Request successful                              |
| 201  | Created — Resource created successfully              |
| 202  | Accepted — Request accepted for processing           |
| 400  | Bad Request — Invalid input or validation error      |
| 401  | Unauthorized — Missing or invalid JWT token          |
| 403  | Forbidden — Fraud check failed, insufficient funds   |
| 404  | Not Found — Resource not found                        |
| 409  | Conflict — Duplicate email, wallet already exists    |
| 422  | Unprocessable Entity — Idempotency key collision     |
| 429  | Too Many Requests — Rate limit exceeded              |
| 500  | Internal Server Error — Unexpected server error      |
| 503  | Service Unavailable — Dependent service down         |

### Example API Flows

#### Flow 1: Create User → Create Wallet → Process Transaction

```bash
# 1. Create User
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1-555-1234"
  }'

# Response (201 Created)
{
  "id": "user-123",
  "email": "john@example.com",
  "status": "ACTIVE"
}

# 2. Create Wallet for User
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "currency": "USD"
  }'

# Response (201 Created)
{
  "id": "wallet-abc",
  "userId": "user-123",
  "balance": 0.00,
  "status": "ACTIVE"
}

# 3. Credit Wallet (Deposit $1000)
curl -X POST http://localhost:8080/api/v1/wallets/wallet-abc/credit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: credit-1000-001" \
  -d '{
    "amount": 1000.00,
    "currency": "USD",
    "description": "Initial deposit"
  }'

# Response (200 OK)
{
  "status": "success",
  "wallet": {
    "id": "wallet-abc",
    "balance": 1000.00,
    "version": 1
  }
}

# 4. Process Transaction (Transfer $100 to Another User's Wallet)
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: tx-flow-001" \
  -H "Idempotency-Key: tx-20260422-001" \
  -d '{
    "fromWalletId": "wallet-abc",
    "toWalletId": "wallet-xyz",
    "amount": 100.00,
    "currency": "USD",
    "description": "Payment to Jane"
  }'

# Response (201 Created)
{
  "id": "tx-550e8400",
  "status": "COMPLETED",
  "amount": 100.00,
  "fraudCheckResult": {
    "riskScore": 0.15,
    "approved": true
  }
}

# 5. Verify Transaction (Idempotent)
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: tx-20260422-001" \  # ← Same key
  -d '{ ... }'

# Response (200 OK) - Returns cached result
{
  "status": "CACHED",
  "id": "tx-550e8400",
  "message": "Transaction with this key was already processed"
}
```

#### Flow 2: Recover from Service Failure with Retry

```bash
# First attempt to transfer $50
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Idempotency-Key: retry-test-001" \
  -d '{"fromWalletId": "wallet-abc", "toWalletId": "wallet-xyz", "amount": 50}'

# Response: 503 Service Unavailable (Fraud Service down)

# Retry with SAME Idempotency-Key
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Idempotency-Key: retry-test-001" \  # ← Same key
  -d '{ ... }'

# Response: 201 Created (Transaction now succeeds, no duplicate created)
```

---

## DEVELOPMENT WORKFLOW

### Setup Local Development

```bash
# 1. Clone repository
git clone https://github.com/yourorg/fintech-platform.git
cd fintech-platform

# 2. Build all services
mvn clean install -DskipTests

# 3. Start Docker Compose stack
cd infra/
docker-compose up -d
docker-compose logs -f

# 4. Verify all services are healthy
for port in 8080 8081 8082 8083 8084; do
  curl -s http://localhost:$port/actuator/health | jq '.status'
done
```

### Running Tests

```bash
# Run all unit tests
mvn test

# Run integration tests
mvn verify

# Run specific service tests
mvn -pl services/user-service test

# Run tests with coverage
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html

# Run specific test class
mvn -Dtest=WalletServiceTest test
mvn -Dtest=WalletServiceTest#testDebitWithOptimisticLocking test
```

### Building and Running Locally

```bash
# Build specific service
mvn -pl services/wallet-service clean package

# Run service directly (not in Docker)
java -jar services/wallet-service/target/wallet-service-1.0.0-SNAPSHOT.jar

# With custom properties
java -Dspring.profiles.active=dev \
     -Ddb.host=localhost \
     -jar services/wallet-service/target/wallet-service-1.0.0-SNAPSHOT.jar
```

### Debugging

**IntelliJ IDEA:**
```
Run → Edit Configurations → Add "Remote JVM Debug"
  Host: localhost
  Port: 5005

# Start service with debug flag
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar target/wallet-service-1.0.0-SNAPSHOT.jar
```

**VSCode:**
```json
// launch.json
{
  "name": "Wallet Service Debug",
  "type": "java",
  "name": "Launch Wallet Service",
  "request": "launch",
  "mainClass": "com.fintech.wallet.WalletServiceApplication",
  "preLaunchTask": "maven: build",
  "projectName": "wallet-service",
  "cwd": "${workspaceFolder}",
  "console": "integratedTerminal"
}
```

---

## KEY DESIGN DECISIONS (ADRS)

### ADR-001: Microservices Architecture

**Status:** Accepted

**Decision:** Adopt microservices architecture with 4 independent services (User, Wallet, Transaction, Fraud), each owning its database and communicating via REST and RabbitMQ.

**Rationale:**
- Independent scaling: Transaction Service can scale separately from User Service
- Technology flexibility: Each service can use different tech stack if needed
- Fault isolation: Fraud Service failure doesn't crash Transaction Service
- Team autonomy: Teams can own services independently
- Faster deployments: Update User Service without redeploying all services

**Tradeoffs:**
- Increased operational complexity (5+ deployments)
- Distributed transaction management (eventual consistency)
- Network latency between services
- Requires robust inter-service communication patterns

**Mitigations:**
- Docker Compose for local dev parity
- Resilience4j circuit breakers
- Correlation IDs for tracing
- Well-defined service contracts (OpenAPI)

---

### ADR-002: Idempotency Strategy

**Status:** Accepted

**Decision:** Implement server-side idempotency using client-provided UUID keys with 24-hour expiration, stored in database.

**Rationale:**
- Safe retries without side effects
- Network failure tolerance
- Works across service restarts (persisted in DB, not in-memory)
- Supports all write operations uniformly
- Enables client retry loops with confidence

**Implementation:**
1. Client generates UUID v4 as `Idempotency-Key`
2. Server stores `(key, operation_type, request_hash, response)` in dedicated table
3. Duplicate requests with same key return cached response (200 OK)
4. Different payload (hash mismatch) returns 422 Unprocessable Entity
5. Background job cleans expired keys (24 hours)

**Tradeoffs:**
- Additional database write per transaction
- 24-hour storage overhead (millions of keys)
- Requires client to generate and manage keys
- Cache lookup adds ~5-10ms latency

**Alternatives Considered:**
1. **Database unique constraints only**: Simpler but doesn't return cached responses
2. **In-memory cache (Redis)**: Lower latency but volatile — risky for financial ops
3. **Idempotency at API Gateway**: Too coarse-grained, different operations need different scoping

---

### Other Design Decisions

1. **Optimistic Locking for Wallets**: Detect conflicts early, retry at application level
2. **Event-Driven Architecture**: Async processing via RabbitMQ for fraud analysis
3. **API Gateway Pattern**: Single entry point for all clients
4. **Database-per-Service**: Independent databases prevent tight coupling
5. **Correlation IDs**: Distributed tracing without external dependencies (initially)
6. **Spring Cloud**: Proven patterns, large ecosystem, industry adoption

---

## IMPLEMENTATION PATTERNS

### Hexagonal Architecture (Ports & Adapters)

Each service follows clean architecture principles to separate business logic from infrastructure:

```
USER-FACING LAYER (REST Controllers)
    ↓
APPLICATION LAYER (Request DTOs, Response DTOs)
    ↓
DOMAIN LAYER (Business logic, Entities, Domain Rules)
    ↓
INFRASTRUCTURE LAYER (Repositories, Feign Clients, Configuration)
```

**Benefits:**
- Business logic is framework-agnostic
- Easy to unit test without Spring
- Clear separation of concerns
- Infrastructure details hidden behind interfaces

### Repository Pattern

```java
// Interface (Domain Layer)
public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    List<User> findAll(Pageable pageable);
    User save(User user);
}

// Implementation (Infrastructure Layer)
@Repository
public class UserRepositoryAdapter implements UserRepository {
    @Autowired
    private UserJpaRepository jpaRepository;
    
    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserMapper::toDomain);
    }
}

// Spring Data JPA (Infrastructure Layer)
@org.springframework.data.repository.Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
}
```

### Service Layer Pattern

```java
@Service
@Transactional
public class WalletService {
    
    private final WalletRepository walletRepository;
    private final UserServiceClient userService;
    
    public WalletResponse debitWallet(UUID walletId, BigDecimal amount) {
        // 1. Fetch wallet
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(WalletNotFoundException::new);
        
        // 2. Business logic
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
        
        // 3. Update state (will trigger @Version update)
        wallet.debit(amount);
        
        // 4. Persist changes
        Wallet updatedWallet = walletRepository.save(wallet);
        
        // 5. Return response
        return WalletMapper.toResponse(updatedWallet);
    }
}
```

### Circuit Breaker with Resilience4j

```java
@FeignClient(
    name = "walletServiceClient",
    url = "${wallet.service.url:http://wallet-service:8082}",
    fallback = WalletServiceFallback.class
)
@CircuitBreaker(name = "walletService")
@Retry(name = "walletService")
@Timeout(name = "walletService")
public interface WalletServiceClient {
    @PostMapping("/api/v1/wallets/{id}/debit")
    WalletResponse debitWallet(
        @PathVariable UUID id,
        @RequestBody DebitRequest request
    );
}

@Configuration
public class Resilience4jConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    @Bean
    public CircuitBreaker walletServiceCircuitBreaker(
        CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .slowCallRateThreshold(100)
            .slowCallDurationThreshold(Duration.ofSeconds(2))
            .build();
        
        return registry.circuitBreaker("walletService", config);
    }
}
```

### Idempotency Interceptor

```java
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {
    
    private final IdempotencyRecordRepository idempotencyRepo;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        if (isIdempotentOperation(request)) {
            String idempotencyKey = request.getHeader("Idempotency-Key");
            
            if (idempotencyKey == null) {
                throw new MissingIdempotencyKeyException();
            }
            
            Optional<IdempotencyRecord> existing = 
                idempotencyRepo.findByKey(idempotencyKey);
            
            if (existing.isPresent()) {
                // Return cached response
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().write(existing.get().getResponsePayload());
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isIdempotentOperation(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || 
               "PATCH".equals(method);
    }
}
```

### Event Publisher & Subscriber

```java
// Publisher (Transaction Service)
@Component
public class TransactionEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishTransactionCompleted(Transaction transaction) {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            transaction.getId(),
            transaction.getUserId(),
            transaction.getAmount(),
            transaction.getStatus()
        );
        
        rabbitTemplate.convertAndSend(
            "transactions",
            "transaction.completed",
            event
        );
    }
}

// Subscriber (Fraud Service)
@Component
public class TransactionEventConsumer {
    
    private final FraudAnalysisService fraudService;
    
    @RabbitListener(queues = "fraud-analysis")
    public void handleTransactionCompleted(
        @Payload TransactionCompletedEvent event,
        @Header(name = "X-Correlation-Id") String correlationId
    ) {
        // Asynchronously analyze fraud risk
        fraudService.analyzeAsync(event, correlationId);
    }
}
```

---

## TESTING STRATEGY

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {
    
    @Mock
    private WalletRepository walletRepository;
    
    @Mock
    private UserServiceClient userService;
    
    @InjectMocks
    private WalletService walletService;
    
    @Test
    void testDebitWallet_WithSufficientFunds_Success() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("1000.00");
        BigDecimal debitAmount = new BigDecimal("100.00");
        
        Wallet wallet = new Wallet(walletId, balance);
        when(walletRepository.findById(walletId))
            .thenReturn(Optional.of(wallet));
        
        // Act
        WalletResponse response = walletService.debitWallet(walletId, debitAmount);
        
        // Assert
        assertThat(response.getBalance())
            .isEqualTo(new BigDecimal("900.00"));
        
        verify(walletRepository).save(any(Wallet.class));
    }
    
    @Test
    void testDebitWallet_WithInsufficientFunds_ThrowsException() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("50.00");
        BigDecimal debitAmount = new BigDecimal("100.00");
        
        Wallet wallet = new Wallet(walletId, balance);
        when(walletRepository.findById(walletId))
            .thenReturn(Optional.of(wallet));
        
        // Act & Assert
        assertThatThrownBy(() -> walletService.debitWallet(walletId, debitAmount))
            .isInstanceOf(InsufficientFundsException.class);
        
        verify(walletRepository, never()).save(any());
    }
}
```

### Integration Testing

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class WalletControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
        .withDatabaseName("wallets_test")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testCreateWallet_WithValidUser_Success() {
        // Arrange
        CreateWalletRequest request = new CreateWalletRequest("user-123", "USD");
        
        // Act & Assert
        webTestClient.post()
            .uri("/api/v1/wallets")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(WalletResponse.class)
            .consumeWith(response -> {
                assertThat(response.getResponseBody().getBalance())
                    .isZero();
            });
    }
    
    @Test
    void testDebitWallet_WithOptimisticLockingConflict_Retry() {
        // Simulate concurrent updates
        // Wallet version incremented by another transaction
        // Expect retry logic to succeed
    }
}
```

### Contract Testing

Using Spring Cloud Contract for API contracts:

```yaml
# contracts/wallets/should_debit_wallet.yml
request:
  method: POST
  url: /api/v1/wallets/550e8400-e29b-41d4/debit
  body:
    amount: 100.00
    currency: USD
  headers:
    Idempotency-Key: "key-123"

response:
  status: 200
  body:
    wallet:
      balance: 900.00
    status: success
  headers:
    Content-Type: application/json
```

---

## SECURITY ARCHITECTURE

### Authentication (JWT)

All endpoints (except `/health`, `/swagger-ui.html`) require JWT Bearer token:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJ1c2VyLTEyMyIsImlhdCI6MTcwMDAwMDAwMCwi
ZXhwIjoxNzAwMDg2NDAwfQ.
signature
```

**Token Claims:**
```json
{
  "sub": "user-123",
  "email": "john@example.com",
  "roles": ["USER", "ADMIN"],
  "iat": 1700000000,
  "exp": 1700086400,
  "iss": "fintech-platform"
}
```

### Authorization

```java
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public WalletResponse createWallet(
        @RequestBody CreateWalletRequest request,
        @AuthenticationPrincipal UserDetails user
    ) {
        // Only authenticated users can create wallets
        // userId from request must match authenticated user
        if (!request.getUserId().equals(getCurrentUserId(user))) {
            throw new AccessDeniedException("Cannot create wallet for other user");
        }
        
        return walletService.createWallet(request);
    }
}
```

### Input Validation

```java
public class CreateTransactionRequest {
    
    @NotNull(message = "From wallet ID is required")
    @NotBlank
    private UUID fromWalletId;
    
    @NotNull(message = "To wallet ID is required")
    @NotBlank
    private UUID toWalletId;
    
    @NotNull
    @Positive(message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;
    
    @Size(min = 3, max = 3, message = "Currency must be 3-letter code")
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency;
    
    @UUID(message = "Idempotency key must be valid UUID")
    private String idempotencyKey;
}
```

### SQL Injection Prevention

Using JPA parameterized queries:

```java
// ✅ Safe - JPA parameterized
@Query("SELECT t FROM Transaction t WHERE t.fromWalletId = :walletId")
List<Transaction> findByWalletId(@Param("walletId") UUID walletId);

// ✅ Safe - String interpolation with repositories
List<Transaction> findByFromWalletIdAndStatusOrderByCreatedAtDesc(
    UUID walletId, 
    String status
);

// ❌ DANGEROUS - String concatenation (DO NOT USE)
// String query = "SELECT * FROM transactions WHERE wallet_id = '" + walletId + "'";
```

### PII Masking in Logs

```java
public class SensitiveDataMasker {
    
    public static String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        return email.substring(0, 1) + "***" + email.substring(email.length() - 1);
        // john@example.com → j***m
    }
    
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
        // +1-555-1234 → ***1234
    }
    
    public static String maskBalance(BigDecimal balance) {
        return balance == null ? "***" : balance.toString(); // Don't mask amounts
    }
}

// Usage in logging
log.info("User created: email={}, phone={}", 
    maskEmail(user.getEmail()), 
    maskPhone(user.getPhone())
);
```

### Rate Limiting (API Gateway)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: wallet-service
          uri: http://wallet-service:8082
          predicates:
            - Path=/api/v1/wallets/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100  # 100 requests
                redis-rate-limiter.burstCapacity: 200   # per minute
                key-resolver: "#{@userKeyResolver}"
```

### HTTPS/TLS

All production deployments must use TLS 1.3:

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: fintech
    protocol: TLSv1.3
    enabled-protocols:
      - TLSv1.3
      - TLSv1.2
```

---

## OBSERVABILITY & MONITORING

### Health Checks

Each service exposes health endpoint:

```bash
curl http://localhost:8083/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL"
      }
    },
    "rabbit": {
      "status": "UP",
      "details": {
        "version": "3.13.0"
      }
    },
    "walletServiceClient": {
      "status": "UP",
      "details": {
        "httpStatus": 200
      }
    }
  }
}
```

### Metrics Collection

Micrometer collects metrics from each service:

```bash
curl http://localhost:8083/actuator/metrics
```

Key metrics:
- `http.server.requests` — HTTP request count, latency
- `jvm.memory.used` — JVM memory usage
- `jvm.gc.memory.allocated` — Garbage collection
- `process.cpu.usage` — CPU usage
- `db.connection.pool.usage` — Database connection pool
- `rabbitmq.queue.size` — RabbitMQ queue depths
- `resilience4j.circuitbreaker.state` — Circuit breaker state

### Distributed Tracing

Correlation IDs propagated through call stack:

```
Client Request (IP: 192.168.1.100)
    X-Correlation-Id: corr-20260422-001
    ↓
API Gateway logs:
    correlationId=corr-20260422-001 action=route_request target=transaction-service

Transaction Service logs:
    correlationId=corr-20260422-001 action=process_transaction
    
Calls Wallet Service:
    X-Correlation-Id: corr-20260422-001
    
Wallet Service logs:
    correlationId=corr-20260422-001 action=debit_wallet

Calls User Service:
    X-Correlation-Id: corr-20260422-001

All logs can be queried by: correlationId=corr-20260422-001
```

### Logging

Structured JSON logging for easy parsing:

```json
{
  "@timestamp": "2026-04-22T10:15:30.123Z",
  "level": "INFO",
  "logger": "com.fintech.transaction.service.TransactionService",
  "message": "Transaction processing completed",
  "fields": {
    "correlationId": "corr-12345",
    "transactionId": "tx-550e8400",
    "userId": "user-456",
    "amount": 100.00,
    "status": "COMPLETED",
    "fromWalletId": "wallet-123",
    "toWalletId": "wallet-456",
    "durationMs": 245,
    "fraudRiskScore": 0.15
  },
  "threadName": "http-nio-8083-exec-1"
}
```

---

## ROADMAP & STATUS

### Completed (v1.0.0)

- ✅ Microservices architecture with 4 services
- ✅ User management (registration, profiles)
- ✅ Wallet operations (create, credit, debit)
- ✅ Transaction processing with idempotency
- ✅ Fraud detection with rule engine
- ✅ Resilience patterns (circuit breaker, retry)
- ✅ Event-driven architecture (RabbitMQ)
- ✅ Comprehensive documentation
- ✅ OpenAPI/Swagger documentation
- ✅ Docker Compose local development

### Planned (v1.1.0)

- 🔲 Notification Service (email, SMS for alerts)
- 🔲 Kafka integration (alternative to RabbitMQ)
- 🔲 Redis caching layer (balance caching, session storage)
- 🔲 API rate limiting per user/IP
- 🔲 Advanced fraud rules (ML-based risk scoring)
- 🔲 Kubernetes manifests (production deployment)
- 🔲 Prometheus + Grafana integration
- 🔲 Jaeger distributed tracing
- 🔲 GraphQL API gateway
- 🔲 Multi-currency support

### Future Considerations (v2.0.0)

- 🔲 CQRS (Command Query Responsibility Segregation)
- 🔲 Event Sourcing for transaction audit trail
- 🔲 Machine learning fraud detection
- 🔲 Real-time dashboard
- 🔲 Mobile app (iOS/Android)
- 🔲 Third-party payment integration (Stripe, PayPal)
- 🔲 Regulatory compliance (PCI-DSS, GDPR)
- 🔲 Multi-tenant support

---

## SUMMARY

This **Fintech Transaction & Wallet Platform** is a comprehensive, production-grade microservices architecture designed to handle financial transactions with the highest standards of reliability, consistency, and auditability. The platform demonstrates industry best practices in:

- **Domain-Driven Design**: Clear service boundaries with dedicated databases
- **Distributed Systems**: Resilience patterns, eventual consistency, idempotency
- **Software Craftsmanship**: Clean code, comprehensive testing, clear documentation
- **Operational Excellence**: Observability, monitoring, health checks
- **Security**: JWT authentication, input validation, SQL injection prevention
- **Scalability**: Stateless services, horizontal scaling, async processing

By following this architecture and the documented principles, the platform can scale from thousands to millions of transactions daily while maintaining consistency, traceability, and resilience.

---

**Document Version:** 1.0.0  
**Last Updated:** April 22, 2026  
**Next Review:** Q3 2026

For questions or updates, please refer to the project's specification documents in `/speckit/` directory.
