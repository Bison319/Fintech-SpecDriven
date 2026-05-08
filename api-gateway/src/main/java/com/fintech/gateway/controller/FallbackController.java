package com.fintech.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/unavailable")
    public ResponseEntity<Map<String, Object>> serviceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "success", false,
                        "error", Map.of(
                                "code", "SERVICE_UNAVAILABLE",
                                "message", "The requested service is currently unavailable. Please try again later."
                        ),
                        "metadata", Map.of(
                                "timestamp", Instant.now().toString(),
                                "service", "api-gateway"
                        )
                ));
    }
}
