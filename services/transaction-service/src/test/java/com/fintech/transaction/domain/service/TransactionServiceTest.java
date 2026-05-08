package com.fintech.transaction.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transaction.application.dto.CreateTransactionRequest;
import com.fintech.transaction.application.dto.TransactionResponse;
import com.fintech.transaction.domain.model.*;
import com.fintech.transaction.domain.repository.IdempotencyRepository;
import com.fintech.transaction.domain.repository.TransactionRepository;
import com.fintech.transaction.infrastructure.client.FraudServiceClient;
import com.fintech.transaction.infrastructure.client.WalletServiceClient;
import com.fintech.transaction.infrastructure.event.TransactionEventPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private FraudServiceClient fraudServiceClient;

    @Mock
    private TransactionEventPublisher eventPublisher;

    private TransactionService transactionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        transactionService = new TransactionService(
                transactionRepository,
                idempotencyRepository,
                walletServiceClient,
                fraudServiceClient,
                eventPublisher,
                objectMapper
        );
    }

    @Nested
    @DisplayName("processTransaction")
    class ProcessTransaction {

        @Test
        @DisplayName("should process DEBIT transaction successfully")
        void shouldProcessDebitTransaction() {
            UUID idempotencyKey = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    walletId, null, TransactionType.DEBIT,
                    new BigDecimal("100.00"), "USD", "Test debit");

            when(idempotencyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, "PROCESS_TRANSACTION"))
                    .thenReturn(Optional.empty());
            when(fraudServiceClient.evaluateTransaction(any()))
                    .thenReturn(Map.of("decision", "APPROVED"));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(eventPublisher).publishTransactionCompleted(any());

            TransactionResponse response = transactionService.processTransaction(idempotencyKey, request);

            assertThat(response.sourceWalletId()).isEqualTo(walletId);
            assertThat(response.type()).isEqualTo(TransactionType.DEBIT);
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
            verify(walletServiceClient).debitWallet(eq(walletId), any());
        }

        @Test
        @DisplayName("should reject transaction when fraud check returns REJECTED")
        void shouldRejectOnFraudRejection() {
            UUID idempotencyKey = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    walletId, null, TransactionType.DEBIT,
                    new BigDecimal("60000.00"), "USD", "High amount");

            when(idempotencyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, "PROCESS_TRANSACTION"))
                    .thenReturn(Optional.empty());
            when(fraudServiceClient.evaluateTransaction(any()))
                    .thenReturn(Map.of("decision", "REJECTED"));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> transactionService.processTransaction(idempotencyKey, request))
                    .isInstanceOf(TransactionService.FraudRejectedException.class);

            verify(walletServiceClient, never()).debitWallet(any(), any());
        }

        @Test
        @DisplayName("should return cached response for duplicate idempotency key")
        void shouldReturnCachedResponseForDuplicateKey() throws Exception {
            UUID idempotencyKey = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    walletId, null, TransactionType.DEBIT,
                    new BigDecimal("100.00"), "USD", "Test");

            String requestHash = computeTestHash(request);
            TransactionResponse cachedResponse = new TransactionResponse(
                    UUID.randomUUID(), walletId, null, TransactionType.DEBIT,
                    new BigDecimal("100.00"), "USD", TransactionStatus.COMPLETED,
                    idempotencyKey, "APPROVED", "Test", null, null);

            IdempotencyRecord record = mock(IdempotencyRecord.class);
            when(record.getRequestHash()).thenReturn(requestHash);
            when(record.getResponseBody()).thenReturn(objectMapper.writeValueAsString(cachedResponse));

            when(idempotencyRepository.findByIdempotencyKeyAndOperation(idempotencyKey, "PROCESS_TRANSACTION"))
                    .thenReturn(Optional.of(record));

            TransactionResponse response = transactionService.processTransaction(idempotencyKey, request);

            assertThat(response.sourceWalletId()).isEqualTo(walletId);
            verify(walletServiceClient, never()).debitWallet(any(), any());
            verify(fraudServiceClient, never()).evaluateTransaction(any());
        }
    }

    @Nested
    @DisplayName("getTransaction")
    class GetTransaction {

        @Test
        @DisplayName("should throw EntityNotFoundException when transaction not found")
        void shouldThrowWhenNotFound() {
            UUID txnId = UUID.randomUUID();
            when(transactionRepository.findById(txnId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransaction(txnId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    private String computeTestHash(CreateTransactionRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
