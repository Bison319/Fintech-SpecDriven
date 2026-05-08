# ADR-001: Microservices Architecture

## Status

Accepted

## Context

We need to build a fintech platform that handles user management, wallet operations, transaction processing, and fraud detection. The system must be independently scalable, resilient to partial failures, and deployable without downtime.

## Decision

Adopt a microservices architecture with 4 services (User, Wallet, Transaction, Fraud), each owning its database, communicating via REST (synchronous) and RabbitMQ (asynchronous).

## Consequences

### Positive
- Independent deployment and scaling per service
- Technology flexibility per service
- Fault isolation — one service failure doesn't bring down the whole system
- Team autonomy — services can be owned by different teams

### Negative
- Increased operational complexity (multiple deployments, databases)
- Distributed transaction management required
- Network latency between services
- Data consistency is eventual for cross-service state

### Mitigations
- Docker Compose for local development parity
- Resilience4j for circuit breaking and retry
- Idempotency keys for safe retries
- Structured logging with correlation IDs for distributed tracing
