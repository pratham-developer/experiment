package com.experiment.inventoryservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(

        @NotNull(message = "sagaId is required")
        UUID sagaId,

        @NotEmpty(message = "At least one inventory item is required")
        List<@Valid InventoryUnitsDto> inventories
) {
}