package com.fintech.wallet.application.controller;

import com.fintech.wallet.application.dto.AdjustBalanceRequest;
import com.fintech.wallet.application.dto.CreateWalletRequest;
import com.fintech.wallet.application.dto.WalletResponse;
import com.fintech.wallet.domain.service.WalletService;
import com.fintech.wallet.infrastructure.exception.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Wallet lifecycle and balance operations")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @Operation(summary = "Create a new wallet for a user")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateWalletRequest request) {
        WalletResponse wallet = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(wallet));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet details and balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @PathVariable UUID walletId) {
        WalletResponse wallet = walletService.getWallet(walletId);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List all wallets for a user")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getWalletsByUser(
            @PathVariable UUID userId) {
        List<WalletResponse> wallets = walletService.getWalletsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(wallets));
    }

    @PostMapping("/{walletId}/credit")
    @Operation(summary = "Credit (add funds to) a wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> creditWallet(
            @PathVariable UUID walletId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AdjustBalanceRequest request) {
        WalletResponse wallet = walletService.creditWallet(walletId, request);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }

    @PostMapping("/{walletId}/debit")
    @Operation(summary = "Debit (withdraw funds from) a wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> debitWallet(
            @PathVariable UUID walletId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AdjustBalanceRequest request) {
        WalletResponse wallet = walletService.debitWallet(walletId, request);
        return ResponseEntity.ok(ApiResponse.success(wallet));
    }
}
