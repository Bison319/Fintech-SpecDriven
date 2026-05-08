# Fintech Transaction & Wallet Platform

> A production-grade, spec-driven microservices platform for financial transaction processing — built with Java 17, Spring Boot 3, PostgreSQL, and RabbitMQ.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   API GATEWAY (:8080)                       │
│         Spring Cloud Gateway + Correlation IDs              │
└───┬──────────┬──────────────┬───────────────┬───────────────┘
    │          │              │               │
    ▼          ▼              ▼               ▼
┌────────┐ ┌─────────┐ ┌────────────┐ ┌───────────┐
│ User   │ │ Wallet  │ │Transaction │ │  Fraud    │
│Service │ │ Service │ │  Service   │ │  Service  │
│ :8081  │ │  :8082  │ │   :8083    │ │   :8084   │
└───┬────┘ └──┬──────┘ └─────┬──────┘ └─────┬─────┘
    │         │              │               │
    ▼         ▼              ▼               ▼
┌────────┐ ┌─────────┐ ┌────────────┐ ┌───────────┐
│users_db│ │wallets  │ │transactions│ │ fraud_db  │
│        │ │  _db    │ │    _db     │ │           │
└────────┘ └─────────┘ └────────────┘ └───────────┘
                              │
                    ┌─────────▼──────────┐
                    │     RabbitMQ       │
                    │  (Event Broker)    │
                    └────────────────────┘
```

## Key Features

| Feature                        | Description                                                        |
| ------------------------------ | ------------------------------------------------------------------ |
| **Wallet Management**          | Create wallets, credit/debit with optimistic locking               |
| **Transaction Processing**     | Orchestrated payments with fraud checks and idempotency            |
| **Idempotency**                | Client-provided keys prevent duplicate processing                  |
| **Fraud Detection**            | Rule-based engine: velocity checks, amount thresholds, risk scoring|
| **Audit Logging**              | Immutable audit trail for all balance mutations                    |
| **Circuit Breaker + Retry**    | Resilience4j for fault tolerance between services                  |
| **Event-Driven Architecture**  | RabbitMQ for async fraud analysis and audit logging                |
| **API-First Design**           | OpenAPI/Swagger documentation on every service                     |
| **12-Factor Compliance**       | Environment-driven config, stateless services, structured logging  |

## Tech Stack

| Layer          | Technology                                          |
| -------------- | --------------------------------------------------- |
| Language       | Java 17                                             |
| Framework      | Spring Boot 3.2, Spring Cloud 2023.0                |
| API Gateway    | Spring Cloud Gateway                                |
| Database       | PostgreSQL 16 (database-per-service)                |
| ORM            | Spring Data JPA / Hibernate                         |
| Migrations     | Flyway                                              |
| Messaging      | RabbitMQ 3.13                                       |
| Resilience     | Resilience4j (circuit breaker, retry)               |
| API Docs       | SpringDoc OpenAPI 3                                 |
| Build          | Maven (multi-module)                                |
| Containerization | Docker, Docker Compose                            |
| Testing        | JUnit 5, Mockito, AssertJ, H2 (in-memory)          |

## Project Structure

```
fintech-spec-driven-platform/
├── speckit/                          # Spec-Driven Development artifacts
│   ├── speckit.constitution.md       #   Engineering principles & standards
│   ├── speckit.plan.md               #   Architecture & trade-offs
│   ├── speckit.specify.md            #   OpenAPI specs & contracts
│   ├── speckit.tasks.md              #   Epic/Feature/Task breakdown
│   └── speckit.implement.md          #   Implementation guide
├── api-gateway/                      # Spring Cloud Gateway
├── services/
│   ├── user-service/                 # User registration & profiles
│   ├── wallet-service/               # Wallet lifecycle & balance ops
│   ├── transaction-service/          # Payment orchestration
│   └── fraud-service/                # Rule-based fraud detection
├── infra/
│   ├── docker-compose.yml            # Full-stack local environment
│   └── postgres/                     # DB init scripts
├── docs/
│   ├── architecture.md               # System architecture
│   └── adr/                          # Architecture Decision Records
├── pom.xml                           # Parent POM
└── README.md
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/fintech-spec-driven-platform.git
cd fintech-spec-driven-platform

# 2. Build all services
mvn clean package -DskipTests

# 3. Start the full stack
cd infra
docker-compose up --build -d

# 4. Verify all services are healthy
curl http://localhost:8080/actuator/health    # Gateway
curl http://localhost:8081/actuator/health    # User Service
curl http://localhost:8082/actuator/health    # Wallet Service
curl http://localhost:8083/actuator/health    # Transaction Service
curl http://localhost:8084/actuator/health    # Fraud Service
```

### API Documentation

Once services are running, access Swagger UI:

| Service             | Swagger UI URL                               |
| ------------------- | -------------------------------------------- |
| User Service        | http://localhost:8081/swagger-ui.html         |
| Wallet Service      | http://localhost:8082/swagger-ui.html         |
| Transaction Service | http://localhost:8083/swagger-ui.html         |
| Fraud Service       | http://localhost:8084/swagger-ui.html         |

### Sample API Calls

```bash
# Create a user
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email": "john@example.com", "fullName": "John Doe", "phoneNumber": "+1234567890"}'

# Create a wallet
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"userId": "<user-id>", "currency": "USD"}'

# Credit the wallet
curl -X POST http://localhost:8080/api/v1/wallets/<wallet-id>/credit \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount": 1000.00, "description": "Initial deposit"}'

# Process a transaction
curl -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"sourceWalletId": "<wallet-id>", "type": "DEBIT", "amount": 50.00, "currency": "USD", "description": "Coffee purchase"}'
```

## Design Principles

| Principle            | Implementation                                               |
| -------------------- | ------------------------------------------------------------ |
| **Idempotency**      | Every POST accepts `Idempotency-Key` header; responses cached for 24h |
| **Consistency**      | Wallet balances use optimistic locking (`@Version`) with retry |
| **Observability**    | Structured JSON logging, correlation IDs, Actuator endpoints |
| **Resilience**       | Circuit breakers + retry on all inter-service calls          |
| **Security**         | Input validation, PII masking, parameterized queries         |
| **Auditability**     | Immutable `wallet_audit_log` table for every balance change  |

## Spec-Driven Development

This project follows a **Spec-Driven Development** workflow where architectural decisions, API contracts, and implementation plans are defined before code is written:

1. **Constitution** → Non-negotiable engineering principles
2. **Plan** → Architecture diagrams and trade-off analysis
3. **Specification** → OpenAPI contracts and error codes
4. **Tasks** → Epic → Feature → Task breakdown
5. **Implementation** → Code structure and design patterns

See the [`speckit/`](speckit/) directory for all specification artifacts.

## Architecture Patterns

| Pattern                    | Where                                     |
| -------------------------- | ----------------------------------------- |
| Hexagonal Architecture     | All services (application/domain/infra)   |
| API Gateway                | Edge routing and cross-cutting concerns   |
| Database per Service       | Each service owns its PostgreSQL database |
| Optimistic Locking         | Wallet balance updates                    |
| Idempotent Consumer        | Transaction processing                    |
| Circuit Breaker            | All Feign client calls                    |
| Event-Driven (Pub/Sub)     | Post-transaction analysis via RabbitMQ    |
| Domain-Driven Design       | Aggregates, value objects, domain events  |

## Running Tests

```bash
# Run all unit tests
mvn test

# Run a specific service's tests
cd services/user-service
mvn test

# Run with coverage
mvn test jacoco:report
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/your-feature`)
3. Commit using [Conventional Commits](https://www.conventionalcommits.org/)
4. Push and open a Pull Request

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
