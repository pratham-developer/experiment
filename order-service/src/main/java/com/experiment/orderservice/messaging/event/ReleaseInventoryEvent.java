package com.experiment.orderservice.messaging.event;

import com.experiment.orderservice.dto.InventoryUnitsDto;

import java.util.List;
import java.util.UUID;

public record ReleaseInventoryEvent(
        UUID eventId,
        UUID sagaId,
        List<InventoryUnitsDto> inventories
) {
}