-- ============================================================
-- PostgreSQL Initialization Script
-- Creates all schemas for the Fintech Platform
-- ============================================================

-- Run against each database individually or use the
-- multi-database init script for Docker.

-- ============================================================
-- USERS_DB
-- ============================================================
\c users_db;

CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

-- ============================================================
-- WALLETS_DB
-- ============================================================
\c wallets_db;

CREATE TABLE IF NOT EXISTS wallets (
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

CREATE INDEX IF NOT EXISTS idx_wallets_user_id ON wallets (user_id);
CREATE INDEX IF NOT EXISTS idx_wallets_status ON wallets (status);

CREATE TABLE IF NOT EXISTS wallet_audit_log (
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

CREATE INDEX IF NOT EXISTS idx_wallet_audit_wallet_id ON wallet_audit_log (wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_audit_created_at ON wallet_audit_log (created_at);

-- ============================================================
-- TRANSACTIONS_DB
-- ============================================================
\c transactions_db;

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

-- ============================================================
-- FRAUD_DB
-- ============================================================
\c fraud_db;

CREATE TABLE IF NOT EXISTS fraud_checks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    wallet_id           UUID NOT NULL,
    transaction_amount  DECIMAL(19,4) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    transaction_type    VARCHAR(20) NOT NULL,
    risk_score          INTEGER NOT NULL DEFAULT 0,
    decision            VARCHAR(20) NOT NULL,
    reasons             TEXT[],
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fraud_user_id ON fraud_checks (user_id);
CREATE INDEX IF NOT EXISTS idx_fraud_wallet_id ON fraud_checks (wallet_id);
CREATE INDEX IF NOT EXISTS idx_fraud_created_at ON fraud_checks (created_at);
CREATE INDEX IF NOT EXISTS idx_fraud_decision ON fraud_checks (decision);
