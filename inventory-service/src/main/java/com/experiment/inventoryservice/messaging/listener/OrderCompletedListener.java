package com.experiment.inventoryservice.messaging.listener;

import com.experiment.inventoryservice.messaging.config.RabbitMQConfig;
import com.experiment.inventoryservice.messaging.event.OrderCompletedEvent;
import com.experiment.inventoryservice.service.ItemInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompletedListener {

    private final ItemInventoryService itemInventoryService;

    @RabbitListener(
            queues = RabbitMQConfig.ORDER_COMPLETED_QUEUE,
            containerFactory = "retryRabbitListenerContainerFactory"
    )
    @Retryable(
            maxRetries = 4,
            delay = 1000,
            multiplier = 2.0,
            maxDelay = 10000,
            excludes = {
                    IllegalArgumentException.class,
                    IllegalStateException.class
            }
    )
    public void handleOrderCompleted(OrderCompletedEvent event) {
        itemInventoryService.completeOrder(event);

        log.info(
                "Processed ORDER_COMPLETED event {} for saga {}",
                event.eventId(),
                event.sagaId()
        );
    }
}