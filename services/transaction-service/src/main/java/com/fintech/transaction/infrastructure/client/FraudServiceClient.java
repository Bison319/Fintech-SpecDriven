package com.fintech.transaction.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "fraud-service", url = "${feign.client.config.fraud-service.url}")
public interface FraudServiceClient {

    @PostMapping("/api/v1/fraud/check")
    Map<String, Object> evaluateTransaction(@RequestBody Map<String, Object> request);
}
