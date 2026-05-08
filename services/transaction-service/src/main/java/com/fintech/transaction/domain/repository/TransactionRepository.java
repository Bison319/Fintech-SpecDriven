package com.fintech.transaction.domain.repository;

import com.fintech.transaction.domain.model.Transaction;
import com.fintech.transaction.domain.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);

    Page<Transaction> findBySourceWalletId(UUID sourceWalletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWalletId = :walletId OR t.targetWalletId = :walletId")
    Page<Transaction> findByWalletId(@Param("walletId") UUID walletId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sourceWalletId = :walletId AND t.createdAt > :since")
    long countRecentTransactions(@Param("walletId") UUID walletId, @Param("since") Instant since);

    Page<Transaction> findBySourceWalletIdAndStatus(UUID sourceWalletId, TransactionStatus status, Pageable pageable);
}
