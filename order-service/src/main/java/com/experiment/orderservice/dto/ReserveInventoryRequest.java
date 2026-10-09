package com.experiment.orderservice.dto;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        UUID sagaId,
        List<InventoryUnitsDto> inventories
) {
}
