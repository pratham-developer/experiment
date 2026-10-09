package com.experiment.orderservice.client;

import com.experiment.orderservice.dto.ReserveInventoryRequest;
import com.experiment.orderservice.dto.ReserveInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "inventory-service",
        path = "/internal/inventory"
)
public interface InventoryClient {
    @PostMapping("/reserve")
    ReserveInventoryResponse reserveInventory(
            @RequestBody ReserveInventoryRequest request
            );
}
