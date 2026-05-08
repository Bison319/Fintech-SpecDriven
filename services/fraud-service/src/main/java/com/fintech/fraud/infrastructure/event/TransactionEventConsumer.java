package com.fintech.fraud.infrastructure.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    @RabbitListener(queues = "transaction.completed")
    public void handleTransactionCompleted(Map<String, Object> event) {
        log.info("Received TransactionCompleted event: {}", event.get("eventId"));

        Object payload = event.get("payload");
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload;
            log.info("Post-transaction fraud analysis for transaction: {}, amount: {}, type: {}",
                    data.get("transactionId"),
                    data.get("amount"),
                    data.get("type"));
        }
    }

    @RabbitListener(queues = "transaction.failed")
    public void handleTransactionFailed(Map<String, Object> event) {
        log.info("Received TransactionFailed event: {}", event.get("eventId"));
    }
}
