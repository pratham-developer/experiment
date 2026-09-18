package com.experiment.inventoryservice.controller;

import com.experiment.inventoryservice.dto.seller.AddItemRequest;
import com.experiment.inventoryservice.service.ItemService;
import com.experiment.microservicesecuritystarter.annotation.RequiresRole;
import com.experiment.microservicesecuritystarter.security.SecurityRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @RequiresRole(SecurityRole.SELLER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addItem(
            @RequestBody AddItemRequest request
    ) {
        itemService.addItem(request);
    }
}