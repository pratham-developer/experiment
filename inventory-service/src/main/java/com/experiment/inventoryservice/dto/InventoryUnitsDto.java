package com.experiment.inventoryservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryUnitsDto(

        @NotNull(message = "inventoryId is required")
        Long inventoryId,

        @NotNull(message = "unitsRequired is required")
        @Positive(message = "unitsRequired must be positive")
        Long unitsRequired
) {
}