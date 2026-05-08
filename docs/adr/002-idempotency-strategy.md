# ADR-002: Idempotency Strategy

## Status

Accepted

## Context

Financial transactions must never be processed twice, even if clients retry requests due to network failures, timeouts, or load balancer retries. We need a robust idempotency mechanism.

## Decision

Implement server-side idempotency using client-provided `Idempotency-Key` headers with the following approach:

1. Client generates a UUID v4 as the idempotency key
2. Server stores `(key, operation, request_hash, response)` in a dedicated table
3. Duplicate requests with the same key return the cached response
4. Keys expire after 24 hours (cleaned by scheduled job)
5. If a duplicate key arrives with a different request payload (hash mismatch), return 422

## Consequences

### Positive
- Safe client retries without side effects
- Network failure tolerance
- Load balancer retry safety
- Works across service restarts (persisted in database)

### Negative
- Additional database write per transaction (idempotency record)
- 24-hour storage overhead
- Client must generate and manage idempotency keys
- Adds latency for the initial lookup

### Alternatives Considered
- **Database unique constraints only**: Simpler but doesn't return cached responses
- **In-memory cache (Redis)**: Lower latency but volatile — unsafe for financial operations
- **Idempotency at API Gateway level**: Too coarse-grained, different operations need different scoping
