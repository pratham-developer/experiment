package com.experiment.inventoryservice.messaging.event;

import com.experiment.inventoryservice.dto.InventoryUnitsDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReleaseInventoryEvent(

        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotNull(message = "sagaId is required")
        UUID sagaId,

        @NotEmpty(message = "At least one inventory item is required")
        List<@Valid InventoryUnitsDto> inventories
) {
}