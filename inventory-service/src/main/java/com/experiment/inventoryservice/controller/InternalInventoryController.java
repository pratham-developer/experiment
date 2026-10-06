package com.experiment.inventoryservice.controller;

import com.experiment.inventoryservice.dto.ReserveInventoryRequest;
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
    public void reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        itemInventoryService.reserveInventory(request);
    }
}
