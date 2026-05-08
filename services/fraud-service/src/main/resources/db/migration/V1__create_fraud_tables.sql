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
