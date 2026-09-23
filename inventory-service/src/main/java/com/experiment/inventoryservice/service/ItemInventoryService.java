package com.experiment.inventoryservice.service;

import com.experiment.inventoryservice.dto.AddItemInventoryRequest;
import com.experiment.inventoryservice.dto.InventoryUnitsDto;
import com.experiment.inventoryservice.dto.ReserveInventoryRequest;
import com.experiment.inventoryservice.entity.*;
import com.experiment.inventoryservice.messaging.event.ReleaseInventoryEvent;
import com.experiment.inventoryservice.repository.ItemInventoryRepository;
import com.experiment.inventoryservice.repository.ItemRepository;
import com.experiment.inventoryservice.repository.OrderSagaRepository;
import com.experiment.inventoryservice.repository.ProcessedEventRepository;
import com.experiment.microservicesecuritystarter.security.SecurityContext;
import com.experiment.microservicesecuritystarter.security.SecurityPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemInventoryService {

    private final SecurityContext securityContext;
    private final ItemInventoryRepository itemInventoryRepository;
    private final ItemRepository itemRepository;
    private final OrderSagaRepository orderSagaRepository;
    private final ProcessedEventRepository processedEventRepository;

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

    @Transactional
    public void reserveInventory(ReserveInventoryRequest request) {
        if (request.sagaId() == null) {
            throw new IllegalArgumentException("sagaId is required");
        }
        if (request.inventories() == null ||
                request.inventories().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one inventory item is required"
            );
        }

        UUID sagaId = request.sagaId();

        // Idempotency check
        Optional<OrderSaga> existing =
                orderSagaRepository.findById(sagaId);
        if (existing.isPresent()) {
            if (existing.get().getSagaStatus()
                    == OrderSagaStatus.INVENTORY_RESERVED) {
                return;
            }
            throw new IllegalStateException(
                    "Saga " + sagaId + " has already been released"
            );
        }

        // Build inventory map
        Map<Long, Long> capacityMap = request.inventories()
                .stream()
                .collect(Collectors.toMap(
                        InventoryUnitsDto::inventoryId,
                        InventoryUnitsDto::unitsRequired,
                        (a, b) -> {
                            throw new IllegalArgumentException(
                                    "Duplicate inventoryId"
                            );
                        }
                ));

        // Global lock ordering
        List<Long> inventoryIds = capacityMap.keySet()
                .stream()
                .sorted()
                .toList();

        // Acquire all locks in deterministic order
        List<ItemInventory> inventories =
                itemInventoryRepository.findAllByIdForUpdate(
                        inventoryIds
                );

        if (inventories.size() != capacityMap.size()) {
            throw new IllegalArgumentException(
                    "Inventory not found"
            );
        }

        // Validate everything before mutation
        for (ItemInventory inventory : inventories) {
            Long required =
                    capacityMap.get(inventory.getId());
            if (required == null || required <= 0) {
                throw new IllegalArgumentException(
                        "Units required must be positive"
                );
            }
            if (inventory.getUnitsAvailable() < required) {
                throw new IllegalArgumentException(
                        "Insufficient inventory"
                );
            }
        }

        // Mutate managed entities
        for (ItemInventory inventory : inventories) {
            Long required =
                    capacityMap.get(inventory.getId());
            inventory.setUnitsAvailable(
                    inventory.getUnitsAvailable() - required
            );
        }

        // Record successful reservation
        orderSagaRepository.save(
                OrderSaga.builder()
                        .sagaId(sagaId)
                        .sagaStatus(
                                OrderSagaStatus.INVENTORY_RESERVED
                        )
                        .build()
        );
    }

    @Transactional
    public void releaseInventory(ReleaseInventoryEvent event) {
        if(event.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (event.sagaId() == null) {
            throw new IllegalArgumentException("sagaId is required");
        }
        if (event.inventories() == null ||
                event.inventories().isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one inventory item is required"
            );
        }

        UUID eventId = event.eventId();
        UUID sagaId = event.sagaId();

        // Idempotency Check with eventId
        if(processedEventRepository.existsByEventId(eventId)){
            return;
        }

        // Find Saga
        OrderSaga orderSaga = orderSagaRepository.findBySagaId(sagaId)
                .orElseThrow(
                        ()->new IllegalStateException(
                                "Saga not found: " + sagaId
                        )
                );
        OrderSagaStatus currentSagaStatus = orderSaga.getSagaStatus();

        // If already released, save processed event and return early
        if(currentSagaStatus==OrderSagaStatus.INVENTORY_RELEASED){
            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(eventId).build()
            );
            return;
        }

        // If not reserved throw exception
        if(currentSagaStatus!=OrderSagaStatus.INVENTORY_RESERVED){
            throw new IllegalStateException(
                    "Invalid Saga state: " + currentSagaStatus
            );
        }

        // Build quantity map
        Map<Long,Long> quantityMap = event.inventories().stream()
                .collect(
                        Collectors.toMap(
                                InventoryUnitsDto::inventoryId,
                                InventoryUnitsDto::unitsRequired,
                                (a,b) -> {
                                    throw new IllegalArgumentException(
                                            "Duplicate inventoryId"
                                    );
                                }
                        )
                );

        // Sort Ids ASC
        List<Long> inventoryIds = quantityMap.keySet()
                .stream()
                .sorted()
                .toList();

        // Acquire Locks in Deterministic Order
        List<ItemInventory> itemInventories = itemInventoryRepository
                .findAllByIdForUpdate(inventoryIds);

        if (itemInventories.size() != quantityMap.size()) {
            throw new IllegalArgumentException(
                    "Inventory not found"
            );
        }

        // Validate Quantities
        for (ItemInventory inventory : itemInventories) {
            Long quantity =
                    quantityMap.get(inventory.getId());

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException(
                        "Release quantity must be positive"
                );
            }
        }

        // Restore Inventory
        for (ItemInventory inventory : itemInventories) {
            Long quantity =
                    quantityMap.get(inventory.getId());

            inventory.setUnitsAvailable(
                    inventory.getUnitsAvailable() + quantity
            );
        }

        // Update Saga Status
        orderSaga.setSagaStatus(OrderSagaStatus.INVENTORY_RELEASED);
        orderSagaRepository.save(orderSaga);

        // Save processed event
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId).build()
        );
    }
}