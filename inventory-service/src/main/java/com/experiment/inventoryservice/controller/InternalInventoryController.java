package com.experiment.inventoryservice.controller;

import com.experiment.inventoryservice.dto.ReserveInventoryRequest;
import com.experiment.inventoryservice.dto.ReserveInventoryResponse;
import com.experiment.inventoryservice.service.ItemInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/inventory")
@RequiredArgsConstructor
public class InternalInventoryController {

    private final ItemInventoryService itemInventoryService;

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public ReserveInventoryResponse reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        return itemInventoryService.reserveInventory(request);
    }
}
