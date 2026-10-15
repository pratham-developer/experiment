package com.experiment.inventoryservice.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE = "order.events";

    public static final String ORDER_COMPLETED_QUEUE =
            "inventory.order-events";

    public static final String RELEASE_INVENTORY_QUEUE =
            "inventory.release-events";

    public static final String DLX_EXCHANGE = "inventory.dlx";

    public static final String ORDER_COMPLETED_DLQ =
            "inventory.order-events.dlq";

    public static final String RELEASE_INVENTORY_DLQ =
            "inventory.release-events.dlq";

    public static final String ORDER_COMPLETED_DLQ_KEY =
            "ORDER_COMPLETED.DLQ";

    public static final String RELEASE_INVENTORY_DLQ_KEY =
            "RELEASE_INVENTORY.DLQ";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCompletedQueue() {
        return new Queue(
                ORDER_COMPLETED_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", ORDER_COMPLETED_DLQ_KEY
                )
        );
    }

    @Bean
    public Queue releaseInventoryQueue() {
        return new Queue(
                RELEASE_INVENTORY_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", RELEASE_INVENTORY_DLQ_KEY
                )
        );
    }

    @Bean
    public Queue orderCompletedDlq() {
        return new Queue(ORDER_COMPLETED_DLQ, true);
    }

    @Bean
    public Queue releaseInventoryDlq() {
        return new Queue(RELEASE_INVENTORY_DLQ, true);
    }

    @Bean
    public Binding orderCompletedBinding() {
        return BindingBuilder
                .bind(orderCompletedQueue())
                .to(orderEventsExchange())
                .with("ORDER_COMPLETED");
    }

    @Bean
    public Binding releaseInventoryBinding() {
        return BindingBuilder
                .bind(releaseInventoryQueue())
                .to(orderEventsExchange())
                .with("RELEASE_INVENTORY");
    }

    @Bean
    public Binding orderCompletedDlqBinding() {
        return BindingBuilder
                .bind(orderCompletedDlq())
                .to(deadLetterExchange())
                .with(ORDER_COMPLETED_DLQ_KEY);
    }

    @Bean
    public Binding releaseInventoryDlqBinding() {
        return BindingBuilder
                .bind(releaseInventoryDlq())
                .to(deadLetterExchange())
                .with(RELEASE_INVENTORY_DLQ_KEY);
    }
}