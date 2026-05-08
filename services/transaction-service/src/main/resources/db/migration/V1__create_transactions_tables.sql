CREATE TABLE IF NOT EXISTS transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_wallet_id  UUID NOT NULL,
    target_wallet_id  UUID,
    type              VARCHAR(20) NOT NULL,
    amount            DECIMAL(19,4) NOT NULL,
    currency          VARCHAR(3) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key   UUID NOT NULL UNIQUE,
    fraud_check_result VARCHAR(20),
    description       VARCHAR(500),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_txn_source_wallet ON transactions (source_wallet_id);
CREATE INDEX IF NOT EXISTS idx_txn_target_wallet ON transactions (target_wallet_id);
CREATE INDEX IF NOT EXISTS idx_txn_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_txn_created_at ON transactions (created_at);
CREATE INDEX IF NOT EXISTS idx_txn_idempotency ON transactions (idempotency_key);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body   TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
    UNIQUE (idempotency_key, operation)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_keys (idempotency_key, operation);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON idempotency_keys (expires_at);
