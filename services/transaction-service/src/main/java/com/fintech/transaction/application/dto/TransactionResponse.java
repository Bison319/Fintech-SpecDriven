package com.fintech.transaction.application.dto;

import com.fintech.transaction.domain.model.Transaction;
import com.fintech.transaction.domain.model.TransactionStatus;
import com.fintech.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID sourceWalletId,
        UUID targetWalletId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        UUID idempotencyKey,
        String fraudCheckResult,
        String description,
        Instant createdAt,
        Instant completedAt
) {
    public static TransactionResponse from(Transaction txn) {
        return new TransactionResponse(
                txn.getId(),
                txn.getSourceWalletId(),
                txn.getTargetWalletId(),
                txn.getType(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getStatus(),
                txn.getIdempotencyKey(),
                txn.getFraudCheckResult(),
                txn.getDescription(),
                txn.getCreatedAt(),
                txn.getCompletedAt()
        );
    }
}
