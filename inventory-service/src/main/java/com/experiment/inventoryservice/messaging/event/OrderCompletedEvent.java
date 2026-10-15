
package com.experiment.inventoryservice.messaging.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderCompletedEvent(

        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotNull(message = "sagaId is required")
        UUID sagaId

) {}
