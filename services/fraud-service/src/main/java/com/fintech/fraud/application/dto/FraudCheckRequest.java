package com.fintech.fraud.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record FraudCheckRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Wallet ID is required")
        UUID walletId,

        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Transaction type is required")
        String transactionType
) {
}
