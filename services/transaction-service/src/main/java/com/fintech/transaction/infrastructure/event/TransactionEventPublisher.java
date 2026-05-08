package com.fintech.transaction.infrastructure.event;

import com.fintech.transaction.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:fintech-events}")
    private String exchange;

    public TransactionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTransactionCompleted(Transaction transaction) {
        Map<String, Object> event = Map.of(
                "eventType", "TransactionCompleted",
                "eventId", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString(),
                "payload", Map.of(
                        "transactionId", transaction.getId().toString(),
                        "sourceWalletId", transaction.getSourceWalletId().toString(),
                        "type", transaction.getType().name(),
                        "amount", transaction.getAmount().toString(),
                        "currency", transaction.getCurrency(),
                        "status", transaction.getStatus().name()
                )
        );

        try {
            rabbitTemplate.convertAndSend(exchange, "transaction.completed", event);
            log.info("Published TransactionCompleted event for: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Failed to publish TransactionCompleted event for: {}", transaction.getId(), e);
        }
    }

    public void publishTransactionFailed(Transaction transaction, String reason) {
        Map<String, Object> event = Map.of(
                "eventType", "TransactionFailed",
                "eventId", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString(),
                "payload", Map.of(
                        "transactionId", transaction.getId().toString(),
                        "sourceWalletId", transaction.getSourceWalletId().toString(),
                        "reason", reason
                )
        );

        try {
            rabbitTemplate.convertAndSend(exchange, "transaction.failed", event);
            log.info("Published TransactionFailed event for: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Failed to publish TransactionFailed event for: {}", transaction.getId(), e);
        }
    }
}
