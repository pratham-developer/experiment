package com.experiment.inventoryservice.service;

import com.experiment.inventoryservice.dto.seller.AddItemRequest;
import com.experiment.inventoryservice.entity.Item;
import com.experiment.inventoryservice.repostiory.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemInventoryService itemInventoryService;

    @Transactional
    public void addItem(AddItemRequest request) {

        Item item = Item.builder()
                .name(request.name())
                .description(request.description())
                .mrp(request.mrp())
                .build();

        Item savedItem = itemRepository.save(item);

        itemInventoryService.createInventory(
                savedItem,
                request.unitsAvailable(),
                request.sellingPrice()
        );
    }
}