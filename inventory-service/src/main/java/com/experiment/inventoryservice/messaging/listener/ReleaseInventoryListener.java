package com.experiment.inventoryservice.messaging.listener;

import com.experiment.inventoryservice.messaging.config.RabbitMQConfig;
import com.experiment.inventoryservice.messaging.event.ReleaseInventoryEvent;
import com.experiment.inventoryservice.service.ItemInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReleaseInventoryListener {

    private final ItemInventoryService itemInventoryService;

    @RabbitListener(
            queues = RabbitMQConfig.RELEASE_INVENTORY_QUEUE,
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
    public void handleReleaseInventory(ReleaseInventoryEvent event) {
        itemInventoryService.releaseInventory(event);

        log.info(
                "Processed RELEASE_INVENTORY event {} for saga {}",
                event.eventId(),
                event.sagaId()
        );
    }
}