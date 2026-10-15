package com.experiment.orderservice.messaging.event;

import java.util.UUID;

public record ReleaseInventoryEvent(
        UUID eventId,
        UUID sagaId
) {
}