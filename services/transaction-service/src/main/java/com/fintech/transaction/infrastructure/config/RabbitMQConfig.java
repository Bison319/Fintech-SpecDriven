package com.fintech.transaction.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:fintech-events}")
    private String exchange;

    @Value("${app.rabbitmq.transaction-completed-queue:transaction.completed}")
    private String transactionCompletedQueue;

    @Value("${app.rabbitmq.transaction-failed-queue:transaction.failed}")
    private String transactionFailedQueue;

    @Bean
    public TopicExchange fintechExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue transactionCompletedQueue() {
        return QueueBuilder.durable(transactionCompletedQueue).build();
    }

    @Bean
    public Queue transactionFailedQueue() {
        return QueueBuilder.durable(transactionFailedQueue).build();
    }

    @Bean
    public Binding completedBinding(Queue transactionCompletedQueue, TopicExchange fintechExchange) {
        return BindingBuilder.bind(transactionCompletedQueue).to(fintechExchange).with("transaction.completed");
    }

    @Bean
    public Binding failedBinding(Queue transactionFailedQueue, TopicExchange fintechExchange) {
        return BindingBuilder.bind(transactionFailedQueue).to(fintechExchange).with("transaction.failed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
