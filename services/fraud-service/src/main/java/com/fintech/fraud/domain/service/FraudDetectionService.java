package com.fintech.fraud.domain.service;

import com.fintech.fraud.application.dto.FraudCheckRequest;
import com.fintech.fraud.application.dto.FraudCheckResponse;
import com.fintech.fraud.domain.model.FraudCheck;
import com.fintech.fraud.domain.model.FraudDecision;
import com.fintech.fraud.domain.repository.FraudCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudCheckRepository fraudCheckRepository;

    @Value("${app.fraud.high-amount-threshold:10000.00}")
    private BigDecimal highAmountThreshold;

    @Value("${app.fraud.very-high-amount-threshold:50000.00}")
    private BigDecimal veryHighAmountThreshold;

    @Value("${app.fraud.velocity-window-seconds:60}")
    private int velocityWindowSeconds;

    @Value("${app.fraud.velocity-max-transactions:10}")
    private int velocityMaxTransactions;

    @Value("${app.fraud.velocity-warning-transactions:5}")
    private int velocityWarningTransactions;

    public FraudDetectionService(FraudCheckRepository fraudCheckRepository) {
        this.fraudCheckRepository = fraudCheckRepository;
    }

    @Transactional
    public FraudCheckResponse evaluateTransaction(FraudCheckRequest request) {
        log.info("Evaluating fraud risk for user: {}, wallet: {}, amount: {}",
                request.userId(), request.walletId(), request.amount());

        int totalScore = 0;
        List<String> reasons = new ArrayList<>();

        // Rule 1: High amount check
        if (request.amount().compareTo(veryHighAmountThreshold) > 0) {
            totalScore += 70;
            reasons.add("Very high transaction amount: " + request.amount());
        } else if (request.amount().compareTo(highAmountThreshold) > 0) {
            totalScore += 40;
            reasons.add("High transaction amount: " + request.amount());
        }

        // Rule 2: Velocity check (transactions per minute)
        Instant windowStart = Instant.now().minusSeconds(velocityWindowSeconds);
        long recentCount = fraudCheckRepository.countRecentChecksByWallet(request.walletId(), windowStart);

        if (recentCount >= velocityMaxTransactions) {
            totalScore += 50;
            reasons.add("Velocity exceeded: " + recentCount + " transactions in last " + velocityWindowSeconds + "s");
        } else if (recentCount >= velocityWarningTransactions) {
            totalScore += 25;
            reasons.add("High velocity: " + recentCount + " transactions in last " + velocityWindowSeconds + "s");
        }

        // Cap score at 100
        totalScore = Math.min(totalScore, 100);

        // Determine decision
        FraudDecision decision;
        if (totalScore >= 71) {
            decision = FraudDecision.REJECTED;
        } else if (totalScore >= 31) {
            decision = FraudDecision.REVIEW;
        } else {
            decision = FraudDecision.APPROVED;
        }

        // Persist fraud check
        FraudCheck fraudCheck = new FraudCheck(
                request.userId(),
                request.walletId(),
                request.amount(),
                request.currency(),
                request.transactionType(),
                totalScore,
                decision,
                reasons
        );
        fraudCheck = fraudCheckRepository.save(fraudCheck);

        log.info("Fraud check completed: decision={}, score={}, reasons={}", decision, totalScore, reasons);

        return FraudCheckResponse.from(fraudCheck);
    }

    @Transactional(readOnly = true)
    public Page<FraudCheckResponse> getFraudHistory(UUID userId, Pageable pageable) {
        return fraudCheckRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(FraudCheckResponse::from);
    }
}
