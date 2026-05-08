package com.fintech.fraud.application.dto;

import com.fintech.fraud.domain.model.FraudCheck;
import com.fintech.fraud.domain.model.FraudDecision;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record FraudCheckResponse(
        UUID checkId,
        FraudDecision decision,
        Integer riskScore,
        List<String> reasons,
        Instant checkedAt
) {
    public static FraudCheckResponse from(FraudCheck check) {
        return new FraudCheckResponse(
                check.getId(),
                check.getDecision(),
                check.getRiskScore(),
                check.getReasons() != null ? Arrays.asList(check.getReasons()) : List.of(),
                check.getCreatedAt()
        );
    }
}
