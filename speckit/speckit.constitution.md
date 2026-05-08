# SpecKit Constitution — Fintech Transaction & Wallet Platform

> This document defines the non-negotiable engineering principles, coding standards, security guidelines, and API design rules that govern all services in this platform.

---

## 1. Engineering Principles

### 1.1 Idempotency

- **Every write operation** (POST, PUT, PATCH) MUST accept an `Idempotency-Key` header.
- The system MUST return the same response for duplicate requests with the same idempotency key within a 24-hour window.
- Idempotency keys are stored per-service with a composite index on `(key, operation_type)`.
- Retries with the same key MUST NOT create duplicate side effects (double charges, duplicate wallets).

### 1.2 Consistency Model

- **Wallet balance operations** use **strong consistency** with optimistic locking (`@Version` column).
- **Cross-service state** follows **eventual consistency** via domain events.
- All balance mutations are serialized per-wallet using `SELECT ... FOR UPDATE` or optimistic locking with retry.

### 1.3 Observability

- Every service MUST expose `/actuator/health`, `/actuator/metrics`, and `/actuator/info`.
- Structured JSON logging (logback + JSON encoder) with correlation IDs propagated via `X-Correlation-Id` header.
- All inter-service calls MUST log: `correlationId`, `sourceService`, `targetService`, `latencyMs`, `httpStatus`.

### 1.4 Resilience

- All external calls (inter-service, database) MUST be wrapped with **Resilience4j** circuit breakers.
- Default circuit breaker config: `failureRateThreshold=50`, `waitDurationInOpenState=30s`, `slidingWindowSize=10`.
- Retry policy: max 3 attempts, exponential backoff starting at 500ms.
- Timeouts: 5s for inter-service HTTP calls, 3s for database queries.

### 1.5 Auditability

- Every state-changing operation MUST produce an **audit log entry** containing:
  `timestamp`, `userId`, `action`, `resource`, `resourceId`, `previousState`, `newState`, `correlationId`.
- Audit logs are immutable — no UPDATE or DELETE on the audit table.

---

## 2. Coding Standards

### 2.1 Language & Framework

| Concern         | Standard                         |
| --------------- | -------------------------------- |
| Language         | Java 17+                        |
| Framework        | Spring Boot 3.2+                |
| Build Tool       | Maven (multi-module)            |
| ORM              | Spring Data JPA / Hibernate     |
| Migration        | Flyway                          |
| API Docs         | SpringDoc OpenAPI 3             |
| Testing          | JUnit 5, Mockito, Testcontainers |

### 2.2 Package Structure (Hexagonal Architecture)

```
com.fintech.{service}
├── application/          # Inbound adapters (controllers, DTOs)
│   ├── controller/
│   └── dto/
├── domain/               # Core business logic (entities, services, ports)
│   ├── model/
│   ├── repository/       # Port interfaces
│   ├── service/
│   └── event/
└── infrastructure/       # Outbound adapters (persistence, clients, config)
    ├── config/
    ├── client/
    ├── event/
    └── exception/
```

### 2.3 Naming Conventions

| Element                | Convention                         | Example                        |
| ---------------------- | ---------------------------------- | ------------------------------ |
| REST Controller        | `{Entity}Controller`               | `WalletController`             |
| Service                | `{Entity}Service`                  | `WalletService`                |
| Repository             | `{Entity}Repository`               | `WalletRepository`             |
| DTO (Request)          | `{Action}{Entity}Request`          | `CreateWalletRequest`          |
| DTO (Response)         | `{Entity}Response`                 | `WalletResponse`               |
| Entity                 | `{Entity}` (singular, PascalCase)  | `Wallet`                       |
| Database Table         | `{entities}` (plural, snake_case)  | `wallets`                      |
| API Path               | `/api/v1/{entities}`               | `/api/v1/wallets`              |

### 2.4 Code Quality Rules

- No `@Autowired` on fields — use **constructor injection** only.
- No business logic in controllers — controllers are thin adapters.
- No raw SQL strings — use Spring Data queries or `@Query` annotations.
- Immutable DTOs — use Java records for request/response objects.
- No checked exceptions in domain layer — use unchecked domain-specific exceptions.

---

## 3. Security Guidelines

### 3.1 Input Validation

- All inbound DTOs MUST use Jakarta Bean Validation (`@NotNull`, `@Size`, `@DecimalMin`, etc.).
- Amount fields use `BigDecimal` — **never** `double` or `float`.
- SQL injection prevention: parameterized queries only (enforced by JPA).
- XSS prevention: no HTML rendering; all responses are `application/json`.

### 3.2 Authentication & Authorization

- API Gateway handles JWT validation.
- Services trust the `X-User-Id` header propagated by the gateway.
- Internal service-to-service calls use a shared API key in `X-Service-Auth` header.
- No secrets in source code — all sensitive config via environment variables.

### 3.3 Data Protection

- PII fields (email, phone) are logged as masked values: `p***@e***.com`.
- Database connections use TLS in production.
- Passwords are hashed using bcrypt (cost factor 12).

### 3.4 Rate Limiting

- API Gateway enforces rate limits: 100 requests/minute per user.
- Individual services enforce secondary limits for sensitive operations (e.g., 10 transactions/minute per wallet).

---

## 4. API Design Rules

### 4.1 REST Conventions

| Method   | Usage                                  | Idempotent? |
| -------- | -------------------------------------- | ----------- |
| `GET`    | Read resource(s)                       | Yes         |
| `POST`   | Create resource / trigger action       | Yes*        |
| `PUT`    | Full replace                           | Yes         |
| `PATCH`  | Partial update                         | Yes*        |
| `DELETE` | Soft-delete (set status to INACTIVE)   | Yes         |

*Idempotent via `Idempotency-Key` header.

### 4.2 Response Envelope

All API responses follow this envelope:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "metadata": {
    "timestamp": "2026-04-22T10:00:00Z",
    "correlationId": "uuid",
    "service": "wallet-service"
  }
}
```

### 4.3 Error Response Contract

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "WALLET_INSUFFICIENT_BALANCE",
    "message": "Insufficient balance for debit operation",
    "details": [
      {
        "field": "amount",
        "message": "Requested 500.00 but available balance is 200.00"
      }
    ]
  },
  "metadata": { ... }
}
```

### 4.4 HTTP Status Code Usage

| Code  | Meaning                           |
| ----- | --------------------------------- |
| `200` | Success (GET, PUT, PATCH)         |
| `201` | Created (POST)                    |
| `400` | Validation error                  |
| `401` | Unauthorized                      |
| `404` | Resource not found                |
| `409` | Conflict (duplicate, version)     |
| `422` | Business rule violation           |
| `429` | Rate limited                      |
| `500` | Internal server error             |
| `503` | Service unavailable               |

### 4.5 Pagination

```
GET /api/v1/transactions?page=0&size=20&sort=createdAt,desc
```

Response includes pagination metadata:

```json
{
  "data": { "content": [...], "page": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
}
```

### 4.6 Versioning

- URL-based versioning: `/api/v1/...`
- Breaking changes require a new version (`v2`).
- Deprecated endpoints return `Sunset` header with removal date.

---

## 5. Infrastructure Principles

### 5.1 12-Factor Compliance

| Factor              | Implementation                                    |
| ------------------- | ------------------------------------------------- |
| Codebase            | One repo, multiple services (monorepo)            |
| Dependencies        | Maven POM, no system-level dependencies           |
| Config              | Environment variables, Spring profiles            |
| Backing Services    | PostgreSQL, RabbitMQ as attached resources         |
| Build/Release/Run   | Docker multi-stage builds                         |
| Processes           | Stateless services, state in PostgreSQL            |
| Port Binding        | Embedded Tomcat, configurable ports               |
| Concurrency         | Horizontal scaling via container orchestration     |
| Disposability       | Graceful shutdown, fast startup                   |
| Dev/Prod Parity     | Docker Compose mirrors production topology        |
| Logs                | stdout/stderr, structured JSON                    |
| Admin Processes     | Flyway migrations as startup tasks                |

---

*This constitution is the single source of truth for engineering decisions. All code reviews and architectural decisions MUST reference this document.*
