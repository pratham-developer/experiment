package com.experiment.inventoryservice.dto;

import java.math.BigDecimal;

public record ReservedInventoryDto(
        Long inventoryId,
        Long itemId,
        Long unitsReserved,
        BigDecimal price,
        String sellerId
) {
}
