package com.fintech.wallet.domain.service;

import com.fintech.wallet.application.dto.AdjustBalanceRequest;
import com.fintech.wallet.application.dto.CreateWalletRequest;
import com.fintech.wallet.application.dto.WalletResponse;
import com.fintech.wallet.domain.model.Wallet;
import com.fintech.wallet.domain.model.WalletAuditLog;
import com.fintech.wallet.domain.repository.WalletAuditLogRepository;
import com.fintech.wallet.domain.repository.WalletRepository;
import com.fintech.wallet.infrastructure.client.UserServiceClient;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final WalletRepository walletRepository;
    private final WalletAuditLogRepository auditLogRepository;
    private final UserServiceClient userServiceClient;

    public WalletService(WalletRepository walletRepository,
                         WalletAuditLogRepository auditLogRepository,
                         UserServiceClient userServiceClient) {
        this.walletRepository = walletRepository;
        this.auditLogRepository = auditLogRepository;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        log.info("Creating wallet for user: {}, currency: {}", request.userId(), request.currency());

        // Verify user exists via Feign call
        userServiceClient.getUserById(request.userId());

        if (walletRepository.existsByUserIdAndCurrency(request.userId(), request.currency())) {
            throw new DuplicateWalletException(
                    String.format("Wallet already exists for user %s with currency %s", request.userId(), request.currency()));
        }

        Wallet wallet = new Wallet(request.userId(), request.currency());

        try {
            wallet = walletRepository.save(wallet);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateWalletException(
                    String.format("Wallet already exists for user %s with currency %s", request.userId(), request.currency()));
        }

        log.info("Wallet created: {}", wallet.getId());
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + walletId));
        return WalletResponse.from(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUser(UUID userId) {
        return walletRepository.findByUserId(userId).stream()
                .map(WalletResponse::from)
                .toList();
    }

    public WalletResponse creditWallet(UUID walletId, AdjustBalanceRequest request) {
        return executeWithOptimisticRetry(walletId, () -> {
            return performCredit(walletId, request);
        });
    }

    public WalletResponse debitWallet(UUID walletId, AdjustBalanceRequest request) {
        return executeWithOptimisticRetry(walletId, () -> {
            return performDebit(walletId, request);
        });
    }

    @Transactional
    protected WalletResponse performCredit(UUID walletId, AdjustBalanceRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.amount());
        wallet = walletRepository.save(wallet);

        recordAuditLog(wallet.getId(), "CREDIT", request.amount(),
                balanceBefore, wallet.getBalance(), request.description(), request.referenceId());

        log.info("Wallet {} credited: {} {}", walletId, request.amount(), wallet.getCurrency());
        return WalletResponse.from(wallet);
    }

    @Transactional
    protected WalletResponse performDebit(UUID walletId, AdjustBalanceRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found: " + walletId));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.debit(request.amount());
        wallet = walletRepository.save(wallet);

        recordAuditLog(wallet.getId(), "DEBIT", request.amount(),
                balanceBefore, wallet.getBalance(), request.description(), request.referenceId());

        log.info("Wallet {} debited: {} {}", walletId, request.amount(), wallet.getCurrency());
        return WalletResponse.from(wallet);
    }

    private WalletResponse executeWithOptimisticRetry(UUID walletId, RetryableOperation operation) {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                return operation.execute();
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("Optimistic locking failed after {} attempts for wallet: {}", MAX_RETRY_ATTEMPTS, walletId);
                    throw new OptimisticLockRetryExhaustedException(
                            "Failed to update wallet after " + MAX_RETRY_ATTEMPTS + " attempts due to concurrent modification");
                }
                log.warn("Optimistic lock conflict for wallet: {}, attempt: {}/{}", walletId, attempts, MAX_RETRY_ATTEMPTS);
            }
        }
    }

    private void recordAuditLog(UUID walletId, String action, BigDecimal amount,
                                 BigDecimal balanceBefore, BigDecimal balanceAfter,
                                 String description, UUID referenceId) {
        String corrId = MDC.get("correlationId");
        UUID correlationId = corrId != null ? UUID.fromString(corrId) : null;

        WalletAuditLog auditLog = new WalletAuditLog(
                walletId, action, amount, balanceBefore, balanceAfter,
                description, referenceId, correlationId);
        auditLogRepository.save(auditLog);
    }

    @FunctionalInterface
    private interface RetryableOperation {
        WalletResponse execute();
    }

    public static class DuplicateWalletException extends RuntimeException {
        public DuplicateWalletException(String message) { super(message); }
    }

    public static class OptimisticLockRetryExhaustedException extends RuntimeException {
        public OptimisticLockRetryExhaustedException(String message) { super(message); }
    }
}
