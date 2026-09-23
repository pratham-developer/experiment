package com.experiment.inventoryservice.dto;

import java.math.BigDecimal;

public record AddItemInventoryRequest(
        Long itemId,
        Long unitsAvailable,
        BigDecimal sellingPrice
) {
}
