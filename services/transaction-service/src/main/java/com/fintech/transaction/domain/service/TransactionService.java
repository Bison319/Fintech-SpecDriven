package com.fintech.transaction.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transaction.application.dto.CreateTransactionRequest;
import com.fintech.transaction.application.dto.TransactionResponse;
import com.fintech.transaction.domain.model.*;
import com.fintech.transaction.domain.repository.IdempotencyRepository;
import com.fintech.transaction.domain.repository.TransactionRepository;
import com.fintech.transaction.infrastructure.client.FraudServiceClient;
import com.fintech.transaction.infrastructure.client.WalletServiceClient;
import com.fintech.transaction.infrastructure.event.TransactionEventPublisher;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String PROCESS_TXN_OPERATION = "PROCESS_TRANSACTION";

    private final TransactionRepository transactionRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final WalletServiceClient walletServiceClient;
    private final FraudServiceClient fraudServiceClient;
    private final TransactionEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                               IdempotencyRepository idempotencyRepository,
                               WalletServiceClient walletServiceClient,
                               FraudServiceClient fraudServiceClient,
                               TransactionEventPublisher eventPublisher,
                               ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.walletServiceClient = walletServiceClient;
        this.fraudServiceClient = fraudServiceClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse processTransaction(UUID idempotencyKey, CreateTransactionRequest request) {
        log.info("Processing transaction with idempotency key: {}", idempotencyKey);

        // Step 1: Check idempotency
        String requestHash = computeHash(request);
        Optional<IdempotencyRecord> existingRecord =
                idempotencyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, PROCESS_TXN_OPERATION);

        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyMismatchException(
                        "Idempotency key already used with a different request payload");
            }
            log.info("Idempotent replay for key: {}", idempotencyKey);
            return deserializeResponse(record.getResponseBody());
        }

        // Step 2: Fraud check
        String fraudDecision = performFraudCheck(request);
        if ("REJECTED".equals(fraudDecision)) {
            Transaction failedTxn = createTransaction(request, idempotencyKey);
            failedTxn.setFraudCheckResult("REJECTED");
            failedTxn.fail();
            transactionRepository.save(failedTxn);
            throw new FraudRejectedException("Transaction rejected by fraud detection");
        }

        // Step 3: Execute wallet operation
        Transaction transaction = createTransaction(request, idempotencyKey);
        transaction.setFraudCheckResult(fraudDecision);

        try {
            executeWalletOperation(request);
            transaction.complete();
        } catch (Exception e) {
            log.error("Wallet operation failed for transaction: {}", idempotencyKey, e);
            transaction.fail();
            transactionRepository.save(transaction);
            throw new TransactionProcessingException("Failed to execute wallet operation: " + e.getMessage());
        }

        // Step 4: Save transaction
        transaction = transactionRepository.save(transaction);
        TransactionResponse response = TransactionResponse.from(transaction);

        // Step 5: Store idempotency record
        storeIdempotencyRecord(idempotencyKey, requestHash, 201, response);

        // Step 6: Publish event
        eventPublisher.publishTransactionCompleted(transaction);

        log.info("Transaction processed successfully: {}", transaction.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
        return TransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(UUID walletId, Pageable pageable) {
        if (walletId != null) {
            return transactionRepository.findByWalletId(walletId, pageable)
                    .map(TransactionResponse::from);
        }
        return transactionRepository.findAll(pageable).map(TransactionResponse::from);
    }

    @CircuitBreaker(name = "fraudService", fallbackMethod = "fraudCheckFallback")
    private String performFraudCheck(CreateTransactionRequest request) {
        log.debug("Performing fraud check for wallet: {}", request.sourceWalletId());
        Map<String, Object> fraudRequest = Map.of(
                "userId", request.sourceWalletId(), // Simplified: using walletId as userId reference
                "walletId", request.sourceWalletId(),
                "amount", request.amount(),
                "currency", request.currency(),
                "transactionType", request.type().name()
        );
        Map<String, Object> result = fraudServiceClient.evaluateTransaction(fraudRequest);
        Object decision = result.get("decision");
        return decision != null ? decision.toString() : "APPROVED";
    }

    private String fraudCheckFallback(CreateTransactionRequest request, Throwable throwable) {
        log.warn("Fraud service unavailable, defaulting to APPROVED. Reason: {}", throwable.getMessage());
        return "APPROVED";
    }

    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    private void executeWalletOperation(CreateTransactionRequest request) {
        Map<String, Object> walletRequest = Map.of(
                "amount", request.amount(),
                "description", request.description() != null ? request.description() : "",
                "referenceId", UUID.randomUUID().toString()
        );

        switch (request.type()) {
            case CREDIT -> walletServiceClient.creditWallet(request.sourceWalletId(), walletRequest);
            case DEBIT -> walletServiceClient.debitWallet(request.sourceWalletId(), walletRequest);
            case TRANSFER -> {
                walletServiceClient.debitWallet(request.sourceWalletId(), walletRequest);
                walletServiceClient.creditWallet(request.targetWalletId(), walletRequest);
            }
        }
    }

    private Transaction createTransaction(CreateTransactionRequest request, UUID idempotencyKey) {
        return new Transaction(
                request.sourceWalletId(),
                request.targetWalletId(),
                request.type(),
                request.amount(),
                request.currency(),
                idempotencyKey,
                request.description()
        );
    }

    private void storeIdempotencyRecord(UUID key, String requestHash, int status, TransactionResponse response) {
        try {
            String responseBody = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = new IdempotencyRecord(key, PROCESS_TXN_OPERATION, requestHash, status, responseBody);
            idempotencyRepository.save(record);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize idempotency response", e);
        }
    }

    private String computeHash(CreateTransactionRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    private TransactionResponse deserializeResponse(String json) {
        try {
            return objectMapper.readValue(json, TransactionResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize idempotency response", e);
        }
    }

    public static class IdempotencyKeyMismatchException extends RuntimeException {
        public IdempotencyKeyMismatchException(String message) { super(message); }
    }

    public static class FraudRejectedException extends RuntimeException {
        public FraudRejectedException(String message) { super(message); }
    }

    public static class TransactionProcessingException extends RuntimeException {
        public TransactionProcessingException(String message) { super(message); }
    }
}
