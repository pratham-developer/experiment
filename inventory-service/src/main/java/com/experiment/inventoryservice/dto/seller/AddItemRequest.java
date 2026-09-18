package com.experiment.inventoryservice.dto.seller;

import java.math.BigDecimal;

public record AddItemRequest(
        String name,
        String description,
        BigDecimal mrp,
        Long unitsAvailable,
        BigDecimal sellingPrice
){}
