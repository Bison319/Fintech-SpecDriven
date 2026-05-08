package com.fintech.transaction.application.controller;

import com.fintech.transaction.application.dto.CreateTransactionRequest;
import com.fintech.transaction.application.dto.TransactionResponse;
import com.fintech.transaction.domain.service.TransactionService;
import com.fintech.transaction.infrastructure.exception.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction processing with idempotency and fraud checks")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(summary = "Process a new transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> processTransaction(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.processTransaction(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable UUID transactionId) {
        TransactionResponse response = transactionService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List transactions with optional wallet filter")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactions(
            @Parameter(description = "Filter by wallet ID")
            @RequestParam(required = false) UUID walletId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TransactionResponse> transactions = transactionService.listTransactions(walletId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
