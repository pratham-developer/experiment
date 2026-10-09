package com.experiment.orderservice.dto;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryResponse(
        UUID sagaId,
        List<ReservedInventoryDto> inventories
) {
}
