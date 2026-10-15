package com.experiment.inventoryservice.service;

import com.experiment.inventoryservice.dto.*;
import com.experiment.inventoryservice.entity.*;
import com.experiment.inventoryservice.messaging.event.OrderCompletedEvent;
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
    public ReserveInventoryResponse reserveInventory(
            ReserveInventoryRequest request
    ) {
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

            OrderSaga saga = existing.get();

            if (saga.getSagaStatus() == OrderSagaStatus.INVENTORY_RESERVED
                    || saga.getSagaStatus() == OrderSagaStatus.ORDER_COMPLETED) {

                return new ReserveInventoryResponse(
                        sagaId,
                        saga.getReservationDetails()
                );
            }

            if (saga.getSagaStatus() == OrderSagaStatus.INVENTORY_RELEASED) {
                throw new IllegalStateException(
                        "Cannot reserve inventory for released saga: " + sagaId
                );
            }

            throw new IllegalStateException(
                    "Unexpected saga state: " + saga.getSagaStatus()
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

        // Acquire locks in deterministic order
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

        // Create reservation snapshot
        List<ReservedInventoryDto> reservedInventories =
                inventories.stream()
                        .map(inventory ->
                                new ReservedInventoryDto(
                                        inventory.getId(),
                                        inventory.getItem().getId(),
                                        capacityMap.get(inventory.getId()),
                                        inventory.getSellingPrice(),
                                        inventory.getSellerId()
                                )
                        )
                        .toList();

        // Decrement inventory
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
                        .reservationDetails(
                                reservedInventories
                        )
                        .build()
        );

        // Return reservation details to Order Service
        return new ReserveInventoryResponse(
                sagaId,
                reservedInventories
        );
    }

    @Transactional
    public void releaseInventory(ReleaseInventoryEvent event) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        if (event.sagaId() == null) {
            throw new IllegalArgumentException("sagaId is required");
        }

        UUID eventId = event.eventId();
        UUID sagaId = event.sagaId();

        // Lock the saga before checking processed events.
        OrderSaga saga = orderSagaRepository.findBySagaIdForUpdate(sagaId)
                .orElseThrow(() ->
                        new IllegalStateException("Saga not found: " + sagaId));

        // A duplicate event must not repeat the transition or restore stock.
        if (processedEventRepository.existsByEventId(eventId)) {
            return;
        }

        OrderSagaStatus status = saga.getSagaStatus();

        // A separately delivered release event must not restore stock again.
        if (status == OrderSagaStatus.INVENTORY_RELEASED) {
            processedEventRepository.save(
                    ProcessedEvent.builder()
                            .eventId(eventId)
                            .build()
            );
            return;
        }

        if (status != OrderSagaStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException(
                    "Cannot release inventory from saga state: " + status
            );
        }

        List<ReservedInventoryDto> reservation =
                saga.getReservationDetails();

        if (reservation == null || reservation.isEmpty()) {
            throw new IllegalStateException(
                    "Reservation snapshot is missing for saga: " + sagaId
            );
        }

        // Validate the persisted reservation snapshot before building the map.
        for (ReservedInventoryDto item : reservation) {
            if (item == null
                    || item.inventoryId() == null
                    || item.unitsReserved() == null
                    || item.unitsReserved() <= 0) {
                throw new IllegalStateException(
                        "Invalid reservation snapshot for saga: " + sagaId
                );
            }
        }

        // Use the persisted reservation as the source of truth.
        // Duplicate inventory IDs indicate corrupted reservation data.
        Map<Long, Long> quantityMap = reservation.stream()
                .collect(Collectors.toMap(
                        ReservedInventoryDto::inventoryId,
                        ReservedInventoryDto::unitsReserved,
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate inventory in reservation snapshot"
                            );
                        }
                ));

        List<Long> inventoryIds = quantityMap.keySet()
                .stream()
                .sorted()
                .toList();

        List<ItemInventory> inventories =
                itemInventoryRepository.findAllByIdForUpdate(inventoryIds);

        if (inventories.size() != quantityMap.size()) {
            throw new IllegalStateException(
                    "Reserved inventory record is missing for saga: " + sagaId
            );
        }

        // Restore exactly the quantities that were originally reserved.
        for (ItemInventory inventory : inventories) {
            Long quantity = quantityMap.get(inventory.getId());

            inventory.setUnitsAvailable(
                    inventory.getUnitsAvailable() + quantity
            );
        }

        saga.setSagaStatus(OrderSagaStatus.INVENTORY_RELEASED);
        orderSagaRepository.save(saga);

        // Both the stock restoration and event record commit together.
        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(eventId)
                        .build()
        );
    }

    @Transactional
    public void completeOrder(OrderCompletedEvent event) {
        if (event == null || event.eventId() == null) {
            throw new IllegalArgumentException("eventId is required");
        }

        if (event.sagaId() == null) {
            throw new IllegalArgumentException("sagaId is required");
        }

        // Serialize completion and release operations for this saga.
        OrderSaga saga = orderSagaRepository.findBySagaIdForUpdate(
                        event.sagaId()
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Saga not found: " + event.sagaId()
                        ));

        // Duplicate delivery: no state change is needed.
        if (processedEventRepository.existsByEventId(event.eventId())) {
            return;
        }

        OrderSagaStatus status = saga.getSagaStatus();

        if (status == OrderSagaStatus.INVENTORY_RELEASED) {
            throw new IllegalStateException(
                    "Cannot complete released saga: " + event.sagaId()
            );
        }

        if (status == OrderSagaStatus.ORDER_COMPLETED) {
            throw new IllegalStateException(
                    "Saga is already completed with an unprocessed completion event: "
                            + event.sagaId()
            );
        }

        if (status != OrderSagaStatus.INVENTORY_RESERVED) {
            throw new IllegalStateException(
                    "Invalid saga state: " + status
            );
        }

        saga.setSagaStatus(OrderSagaStatus.ORDER_COMPLETED);
        orderSagaRepository.save(saga);

        processedEventRepository.save(
                ProcessedEvent.builder()
                        .eventId(event.eventId())
                        .build()
        );
    }
}
