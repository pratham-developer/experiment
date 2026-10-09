package com.experiment.orderservice.dto;

public record InventoryUnitsDto(
        Long inventoryId,
        Long unitsRequired
) {
}
