# SpecKit Tasks — Fintech Transaction & Wallet Platform

> Epics → Features → Tasks breakdown with priorities.

---

## Epic 1: Project Foundation & Infrastructure

### Feature 1.1: Repository Setup
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-1.1.1 | Initialize Maven multi-module project        | P0       | ✅ Done     |
| T-1.1.2 | Create parent POM with dependency management | P0       | ✅ Done     |
| T-1.1.3 | Set up .gitignore and EditorConfig           | P0       | ✅ Done     |
| T-1.1.4 | Create speckit/ directory with all spec docs | P0       | ✅ Done     |
| T-1.1.5 | Create README.md (recruiter-facing)          | P1       | ✅ Done     |

### Feature 1.2: Docker & Local Development
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-1.2.1 | Create docker-compose.yml with all services  | P0       | ✅ Done     |
| T-1.2.2 | Create PostgreSQL init script (4 databases)  | P0       | ✅ Done     |
| T-1.2.3 | Create Dockerfile for each service           | P0       | ✅ Done     |
| T-1.2.4 | Add RabbitMQ container configuration         | P1       | ✅ Done     |
| T-1.2.5 | Create common application config             | P1       | ✅ Done     |

---

## Epic 2: User Service

### Feature 2.1: User CRUD
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-2.1.1 | Create User entity with JPA annotations      | P0       | ✅ Done     |
| T-2.1.2 | Create UserRepository interface              | P0       | ✅ Done     |
| T-2.1.3 | Implement UserService (create, get, update, deactivate) | P0 | ✅ Done |
| T-2.1.4 | Create DTOs (CreateUserRequest, UserResponse) | P0      | ✅ Done     |
| T-2.1.5 | Implement UserController with OpenAPI annotations | P0  | ✅ Done     |
| T-2.1.6 | Add input validation (Bean Validation)       | P0       | ✅ Done     |
| T-2.1.7 | Add global exception handler                 | P0       | ✅ Done     |

### Feature 2.2: User Service Testing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-2.2.1 | Unit tests for UserService                   | P0       | ✅ Done     |
| T-2.2.2 | Integration tests for UserController         | P1       | ✅ Done     |
| T-2.2.3 | API contract validation tests                | P2       | 🔲 Backlog |

---

## Epic 3: Wallet Service

### Feature 3.1: Wallet Management
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-3.1.1 | Create Wallet entity with optimistic locking | P0       | ✅ Done     |
| T-3.1.2 | Create WalletRepository interface            | P0       | ✅ Done     |
| T-3.1.3 | Implement WalletService (create, get, credit, debit) | P0 | ✅ Done  |
| T-3.1.4 | Implement optimistic lock retry logic        | P0       | ✅ Done     |
| T-3.1.5 | Create Feign client for User Service         | P0       | ✅ Done     |
| T-3.1.6 | Implement WalletController                   | P0       | ✅ Done     |

### Feature 3.2: Wallet Audit Trail
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-3.2.1 | Create WalletAuditLog entity                 | P1       | ✅ Done     |
| T-3.2.2 | Auto-insert audit entry on balance change    | P1       | ✅ Done     |

### Feature 3.3: Wallet Service Testing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-3.3.1 | Unit tests for WalletService                 | P0       | ✅ Done     |
| T-3.3.2 | Test optimistic locking under concurrency    | P1       | 🔲 Backlog |

---

## Epic 4: Transaction Service

### Feature 4.1: Transaction Processing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-4.1.1 | Create Transaction entity                    | P0       | ✅ Done     |
| T-4.1.2 | Create TransactionRepository                 | P0       | ✅ Done     |
| T-4.1.3 | Implement idempotency key check/store logic  | P0       | ✅ Done     |
| T-4.1.4 | Implement TransactionService (process, list, get) | P0 | ✅ Done     |
| T-4.1.5 | Create Feign client for Wallet Service       | P0       | ✅ Done     |
| T-4.1.6 | Create Feign client for Fraud Service        | P0       | ✅ Done     |
| T-4.1.7 | Implement TransactionController              | P0       | ✅ Done     |

### Feature 4.2: Resilience
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-4.2.1 | Configure Resilience4j circuit breaker       | P0       | ✅ Done     |
| T-4.2.2 | Add retry with exponential backoff           | P0       | ✅ Done     |
| T-4.2.3 | Add fallback for fraud service unavailability | P1      | ✅ Done     |

### Feature 4.3: Event Publishing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-4.3.1 | Configure RabbitMQ template                  | P1       | ✅ Done     |
| T-4.3.2 | Publish TransactionCompleted events          | P1       | ✅ Done     |
| T-4.3.3 | Publish TransactionFailed events             | P1       | ✅ Done     |

### Feature 4.4: Transaction Service Testing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-4.4.1 | Unit tests for TransactionService            | P0       | ✅ Done     |
| T-4.4.2 | Test idempotency key handling                | P0       | ✅ Done     |
| T-4.4.3 | Integration tests with mocked dependencies   | P1       | 🔲 Backlog |

---

## Epic 5: Fraud Service

### Feature 5.1: Rule-Based Fraud Detection
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-5.1.1 | Create FraudCheck entity                     | P0       | ✅ Done     |
| T-5.1.2 | Create FraudCheckRepository                  | P0       | ✅ Done     |
| T-5.1.3 | Implement velocity rule (>N txns/minute)     | P0       | ✅ Done     |
| T-5.1.4 | Implement high-amount rule                   | P0       | ✅ Done     |
| T-5.1.5 | Implement risk score aggregation             | P0       | ✅ Done     |
| T-5.1.6 | Implement FraudController                    | P0       | ✅ Done     |

### Feature 5.2: Async Fraud Analysis
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-5.2.1 | Create RabbitMQ consumer for transaction events | P1    | ✅ Done     |
| T-5.2.2 | Perform post-transaction fraud analysis      | P1       | ✅ Done     |

### Feature 5.3: Fraud Service Testing
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-5.3.1 | Unit tests for FraudDetectionService         | P0       | ✅ Done     |
| T-5.3.2 | Test fraud rules with edge cases             | P1       | 🔲 Backlog |

---

## Epic 6: API Gateway

### Feature 6.1: Gateway Setup
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-6.1.1 | Create Spring Cloud Gateway project          | P0       | ✅ Done     |
| T-6.1.2 | Configure route definitions for all services | P0       | ✅ Done     |
| T-6.1.3 | Add correlation ID filter                    | P1       | ✅ Done     |
| T-6.1.4 | Add rate limiting filter                     | P2       | 🔲 Backlog |

---

## Epic 7: Documentation & DevOps

### Feature 7.1: Documentation
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-7.1.1 | Create architecture documentation            | P1       | ✅ Done     |
| T-7.1.2 | Create ADR for microservices decision        | P2       | ✅ Done     |
| T-7.1.3 | Create ADR for idempotency strategy          | P2       | ✅ Done     |

### Feature 7.2: CI/CD (Future)
| ID     | Task                                          | Priority | Status      |
| ------ | --------------------------------------------- | -------- | ----------- |
| T-7.2.1 | GitHub Actions build pipeline                | P2       | 🔲 Backlog |
| T-7.2.2 | Automated test execution                     | P2       | 🔲 Backlog |
| T-7.2.3 | Docker image publishing                      | P3       | 🔲 Backlog |

---

## Priority Legend

| Priority | Meaning                         |
| -------- | ------------------------------- |
| **P0**   | Must-have for MVP               |
| **P1**   | Important, ship in first release|
| **P2**   | Nice-to-have, second iteration  |
| **P3**   | Future enhancement              |

---

*Tasks are updated as implementation progresses. Each task should map to one or more commits.*
