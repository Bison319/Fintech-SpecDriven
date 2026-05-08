package com.fintech.transaction.application.dto;

import com.fintech.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(

        @NotNull(message = "Source wallet ID is required")
        UUID sourceWalletId,

        UUID targetWalletId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^(USD|EUR|GBP|INR)$", message = "Unsupported currency")
        String currency,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
