package com.experiment.orderservice.messaging.event;

import java.util.UUID;

public record OrderCompletedEvent(
        UUID eventId,
        UUID sagaId
) {}