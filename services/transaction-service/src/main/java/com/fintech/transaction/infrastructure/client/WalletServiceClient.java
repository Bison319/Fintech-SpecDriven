package com.fintech.transaction.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "wallet-service", url = "${feign.client.config.wallet-service.url}")
public interface WalletServiceClient {

    @PostMapping("/api/v1/wallets/{walletId}/credit")
    Object creditWallet(@PathVariable("walletId") UUID walletId, @RequestBody Map<String, Object> request);

    @PostMapping("/api/v1/wallets/{walletId}/debit")
    Object debitWallet(@PathVariable("walletId") UUID walletId, @RequestBody Map<String, Object> request);
}
