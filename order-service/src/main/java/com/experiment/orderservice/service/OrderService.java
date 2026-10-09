package com.experiment.orderservice.service;

import com.experiment.orderservice.dto.InventoryUnitsDto;
import com.experiment.orderservice.dto.PlaceOrderRequest;
import com.experiment.orderservice.dto.ReserveInventoryRequest;
import com.experiment.orderservice.dto.ReserveInventoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryReservationService inventoryReservationService;
    private final OrderCreationService orderCreationService;
    private final CompensationOutboxService compensationOutboxService;

    public void placeOrder(PlaceOrderRequest request) {
        UUID sagaId = UUID.randomUUID();

        ReserveInventoryRequest reserveRequest =
                new ReserveInventoryRequest(
                        sagaId,
                        request.items().stream()
                                .map(item -> new InventoryUnitsDto(
                                        item.inventoryId(),
                                        item.unitsRequired()
                                ))
                                .toList()
                );

        ReserveInventoryResponse reserveResponse =
                inventoryReservationService.reserve(reserveRequest);

        try {
            orderCreationService.createOrderAndEvent(
                    sagaId, request, reserveResponse
            );
        } catch (Exception e) {
            compensationOutboxService.createInventoryReleaseEvent(
                    sagaId,
                    reserveResponse
            );
            throw e;
        }
    }
}