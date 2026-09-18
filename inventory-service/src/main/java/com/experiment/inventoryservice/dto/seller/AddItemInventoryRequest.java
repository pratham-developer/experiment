package com.experiment.inventoryservice.dto.seller;

import java.math.BigDecimal;

public record AddItemInventoryRequest(
        Long itemId,
        Long unitsAvailable,
        BigDecimal sellingPrice
) {
}
