package com.fintech.fraud.domain.service;

import com.fintech.fraud.application.dto.FraudCheckRequest;
import com.fintech.fraud.application.dto.FraudCheckResponse;
import com.fintech.fraud.domain.model.FraudCheck;
import com.fintech.fraud.domain.model.FraudDecision;
import com.fintech.fraud.domain.repository.FraudCheckRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private FraudCheckRepository fraudCheckRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private void setDefaultThresholds() {
        ReflectionTestUtils.setField(fraudDetectionService, "highAmountThreshold", new BigDecimal("10000.00"));
        ReflectionTestUtils.setField(fraudDetectionService, "veryHighAmountThreshold", new BigDecimal("50000.00"));
        ReflectionTestUtils.setField(fraudDetectionService, "velocityWindowSeconds", 60);
        ReflectionTestUtils.setField(fraudDetectionService, "velocityMaxTransactions", 10);
        ReflectionTestUtils.setField(fraudDetectionService, "velocityWarningTransactions", 5);
    }

    @Nested
    @DisplayName("evaluateTransaction")
    class EvaluateTransaction {

        @Test
        @DisplayName("should APPROVE low-risk transaction")
        void shouldApproveLowRiskTransaction() {
            setDefaultThresholds();
            UUID userId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            FraudCheckRequest request = new FraudCheckRequest(
                    userId, walletId, new BigDecimal("100.00"), "USD", "DEBIT");

            when(fraudCheckRepository.countRecentChecksByWallet(eq(walletId), any(Instant.class)))
                    .thenReturn(0L);
            when(fraudCheckRepository.save(any(FraudCheck.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);

            assertThat(response.decision()).isEqualTo(FraudDecision.APPROVED);
            assertThat(response.riskScore()).isEqualTo(0);
            assertThat(response.reasons()).isEmpty();
        }

        @Test
        @DisplayName("should flag high amount transaction for REVIEW")
        void shouldReviewHighAmount() {
            setDefaultThresholds();
            UUID userId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            FraudCheckRequest request = new FraudCheckRequest(
                    userId, walletId, new BigDecimal("15000.00"), "USD", "DEBIT");

            when(fraudCheckRepository.countRecentChecksByWallet(eq(walletId), any(Instant.class)))
                    .thenReturn(0L);
            when(fraudCheckRepository.save(any(FraudCheck.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);

            assertThat(response.decision()).isEqualTo(FraudDecision.REVIEW);
            assertThat(response.riskScore()).isEqualTo(40);
            assertThat(response.reasons()).contains("High transaction amount: 15000.00");
        }

        @Test
        @DisplayName("should REJECT very high amount transaction")
        void shouldRejectVeryHighAmount() {
            setDefaultThresholds();
            UUID userId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            FraudCheckRequest request = new FraudCheckRequest(
                    userId, walletId, new BigDecimal("60000.00"), "USD", "DEBIT");

            when(fraudCheckRepository.countRecentChecksByWallet(eq(walletId), any(Instant.class)))
                    .thenReturn(0L);
            when(fraudCheckRepository.save(any(FraudCheck.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);

            assertThat(response.decision()).isEqualTo(FraudDecision.REJECTED);
            assertThat(response.riskScore()).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("should REJECT when velocity limit exceeded")
        void shouldRejectOnVelocityBreach() {
            setDefaultThresholds();
            UUID userId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            FraudCheckRequest request = new FraudCheckRequest(
                    userId, walletId, new BigDecimal("100.00"), "USD", "DEBIT");

            when(fraudCheckRepository.countRecentChecksByWallet(eq(walletId), any(Instant.class)))
                    .thenReturn(15L); // Exceeds max of 10
            when(fraudCheckRepository.save(any(FraudCheck.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);

            assertThat(response.decision()).isEqualTo(FraudDecision.REVIEW);
            assertThat(response.riskScore()).isEqualTo(50);
            assertThat(response.reasons()).anyMatch(r -> r.contains("Velocity exceeded"));
        }

        @Test
        @DisplayName("should combine multiple risk signals")
        void shouldCombineRiskSignals() {
            setDefaultThresholds();
            UUID userId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            FraudCheckRequest request = new FraudCheckRequest(
                    userId, walletId, new BigDecimal("60000.00"), "USD", "DEBIT");

            when(fraudCheckRepository.countRecentChecksByWallet(eq(walletId), any(Instant.class)))
                    .thenReturn(12L); // Exceeds velocity
            when(fraudCheckRepository.save(any(FraudCheck.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FraudCheckResponse response = fraudDetectionService.evaluateTransaction(request);

            assertThat(response.decision()).isEqualTo(FraudDecision.REJECTED);
            assertThat(response.riskScore()).isEqualTo(100); // Capped at 100
            assertThat(response.reasons()).hasSizeGreaterThan(1);
        }
    }
}
