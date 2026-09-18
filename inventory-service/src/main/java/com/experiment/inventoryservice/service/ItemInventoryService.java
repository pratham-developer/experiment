package com.experiment.inventoryservice.service;

import com.experiment.inventoryservice.dto.seller.AddItemInventoryRequest;
import com.experiment.inventoryservice.entity.Item;
import com.experiment.inventoryservice.entity.ItemInventory;
import com.experiment.inventoryservice.repostiory.ItemInventoryRepository;
import com.experiment.inventoryservice.repostiory.ItemRepository;
import com.experiment.microservicesecuritystarter.security.SecurityContext;
import com.experiment.microservicesecuritystarter.security.SecurityPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ItemInventoryService {

    private final SecurityContext securityContext;
    private final ItemInventoryRepository itemInventoryRepository;
    private final ItemRepository itemRepository;

    /**
     * Adds inventory to an existing item.
     */
    @Transactional
    public void addItemInventory(
            Long itemId,
            AddItemInventoryRequest request
    ) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Item not found with id: " + itemId
                        )
                );

        createInventory(
                item,
                request.unitsAvailable(),
                request.sellingPrice()
        );
    }

    /**
     * Creates initial inventory when creating a new item.
     * Called internally by ItemService.
     */
    public void createInventory(
            Item item,
            Long unitsAvailable,
            BigDecimal sellingPrice
    ) {
        SecurityPrincipal user = securityContext.require();

        ItemInventory inventory = ItemInventory.builder()
                .item(item)
                .sellerId(user.userId())
                .unitsAvailable(unitsAvailable)
                .sellingPrice(sellingPrice)
                .build();

        itemInventoryRepository.save(inventory);
    }
}