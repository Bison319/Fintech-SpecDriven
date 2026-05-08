package com.fintech.fraud.application.controller;

import com.fintech.fraud.application.dto.FraudCheckRequest;
import com.fintech.fraud.application.dto.FraudCheckResponse;
import com.fintech.fraud.domain.service.FraudDetectionService;
import com.fintech.fraud.infrastructure.exception.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud", description = "Rule-based fraud detection and risk scoring")
public class FraudController {

    private final FraudDetectionService fraudDetectionService;

    public FraudController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/check")
    @Operation(summary = "Evaluate transaction for fraud risk")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> evaluateTransaction(
            @Valid @RequestBody FraudCheckRequest request) {
        FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/checks/{userId}")
    @Operation(summary = "Get fraud check history for a user")
    public ResponseEntity<ApiResponse<Page<FraudCheckResponse>>> getFraudHistory(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<FraudCheckResponse> history = fraudDetectionService.getFraudHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
