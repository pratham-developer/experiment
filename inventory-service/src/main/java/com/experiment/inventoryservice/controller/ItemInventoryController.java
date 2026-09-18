package com.experiment.inventoryservice.controller;

import com.experiment.inventoryservice.dto.seller.AddItemInventoryRequest;
import com.experiment.inventoryservice.service.ItemInventoryService;
import com.experiment.microservicesecuritystarter.annotation.RequiresRole;
import com.experiment.microservicesecuritystarter.security.SecurityRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{itemId}/inventory")
@RequiredArgsConstructor
public class ItemInventoryController {

    private final ItemInventoryService itemInventoryService;

    @RequiresRole(SecurityRole.SELLER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addItemInventory(
            @PathVariable Long itemId,
            @RequestBody AddItemInventoryRequest request
    ) {
        itemInventoryService.addItemInventory(itemId, request);
    }
}