package com.fintech.wallet.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateWalletRequest(

        @NotNull(message = "User ID is required")
        UUID userId,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^(USD|EUR|GBP|INR)$", message = "Unsupported currency. Allowed: USD, EUR, GBP, INR")
        String currency
) {
}
