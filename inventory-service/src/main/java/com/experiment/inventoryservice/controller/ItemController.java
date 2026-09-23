package com.experiment.inventoryservice.controller;

import com.experiment.inventoryservice.dto.AddItemRequest;
import com.experiment.inventoryservice.dto.ReserveInventoryRequest;
import com.experiment.inventoryservice.service.ItemInventoryService;
import com.experiment.inventoryservice.service.ItemService;
import com.experiment.microservicesecuritystarter.annotation.RequiresRole;
import com.experiment.microservicesecuritystarter.security.SecurityRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final ItemInventoryService itemInventoryService;

    @RequiresRole(SecurityRole.SELLER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(
            @RequestBody AddItemRequest request
    ) {
        itemService.addItem(request);
    }

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public void reserveInventory(
            @Valid @RequestBody ReserveInventoryRequest request
    ) {
        itemInventoryService.reserveInventory(request);
    }
}