package com.fintech.fraud.domain.repository;

import com.fintech.fraud.domain.model.FraudCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {

    Page<FraudCheck> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM FraudCheck f WHERE f.walletId = :walletId AND f.createdAt > :since")
    long countRecentChecksByWallet(@Param("walletId") UUID walletId, @Param("since") Instant since);
}
