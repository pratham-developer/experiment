package com.experiment.orderservice.dto;

public record OrderItemDto(
        Long inventoryId,
        Long unitsRequired
) {
}
