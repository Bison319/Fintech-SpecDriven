package com.fintech.fraud.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_checks")
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "transaction_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FraudDecision decision;

    @Column(columnDefinition = "TEXT[]")
    private String[] reasons;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FraudCheck() {
    }

    public FraudCheck(UUID userId, UUID walletId, BigDecimal transactionAmount,
                      String currency, String transactionType,
                      Integer riskScore, FraudDecision decision, List<String> reasons) {
        this.userId = userId;
        this.walletId = walletId;
        this.transactionAmount = transactionAmount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.riskScore = riskScore;
        this.decision = decision;
        this.reasons = reasons != null ? reasons.toArray(new String[0]) : new String[0];
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getWalletId() { return walletId; }
    public BigDecimal getTransactionAmount() { return transactionAmount; }
    public String getCurrency() { return currency; }
    public String getTransactionType() { return transactionType; }
    public Integer getRiskScore() { return riskScore; }
    public FraudDecision getDecision() { return decision; }
    public String[] getReasons() { return reasons; }
    public Instant getCreatedAt() { return createdAt; }
}
