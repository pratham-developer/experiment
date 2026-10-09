package com.experiment.orderservice.service;

import com.experiment.orderservice.client.InventoryClient;
import com.experiment.orderservice.dto.ReserveInventoryRequest;
import com.experiment.orderservice.dto.ReserveInventoryResponse;
import com.experiment.orderservice.exception.TransientInventoryException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryReservationService {

    private final InventoryClient inventoryClient;

    @Retryable(
            includes = TransientInventoryException.class,
            maxRetries = 3,
            delay = 200,
            multiplier = 2.0,
            maxDelay = 2000
    )
    public ReserveInventoryResponse reserve(
            ReserveInventoryRequest request
    ) {
        try {
            return inventoryClient.reserveInventory(request);
        } catch (FeignException e) {
            if (e.status() >= 500 || e.status() == -1) {
                throw new TransientInventoryException(e);
            }
            throw e;
        }
    }
}