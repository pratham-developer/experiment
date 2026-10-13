package com.experiment.inventoryservice.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EVENTS_EXCHANGE =
            "order.events";

    public static final String ORDER_COMPLETED_QUEUE =
            "inventory.order-events";

    public static final String RELEASE_INVENTORY_QUEUE =
            "inventory.release-events";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(
                ORDER_EVENTS_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue orderCompletedQueue() {
        return new Queue(ORDER_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue releaseInventoryQueue() {
        return new Queue(RELEASE_INVENTORY_QUEUE, true);
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
}