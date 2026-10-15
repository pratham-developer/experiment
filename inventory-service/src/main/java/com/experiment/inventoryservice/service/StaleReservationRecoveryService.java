
package com.experiment.inventoryservice.service;

import com.experiment.inventoryservice.dto.ReservedInventoryDto;
import com.experiment.inventoryservice.entity.ItemInventory;
import com.experiment.inventoryservice.entity.OrderSaga;
import com.experiment.inventoryservice.entity.OrderSagaStatus;
import com.experiment.inventoryservice.repository.ItemInventoryRepository;
import com.experiment.inventoryservice.repository.OrderSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaleReservationRecoveryService {

    private final OrderSagaRepository orderSagaRepository;
    private final ItemInventoryRepository itemInventoryRepository;

    @Transactional(timeout = 30)
    public boolean recover(UUID sagaId) {
        var optionalSaga = orderSagaRepository.findBySagaIdForUpdate(sagaId);

        if (optionalSaga.isEmpty()) {
            return false;
        }

        OrderSaga saga = optionalSaga.get();

        // Another handler may have completed or released this saga
        // after the scheduled job originally fetched it.
        if (saga.getSagaStatus() != OrderSagaStatus.INVENTORY_RESERVED) {
            return false;
        }

        List<ReservedInventoryDto> reservation =
                saga.getReservationDetails();

        if (reservation == null || reservation.isEmpty()) {
            throw new IllegalStateException(
                    "Missing reservation snapshot for saga " + sagaId
            );
        }

        for (ReservedInventoryDto item : reservation) {
            if (item == null
                    || item.inventoryId() == null
                    || item.unitsReserved() == null
                    || item.unitsReserved() <= 0) {
                throw new IllegalStateException(
                        "Invalid reservation snapshot for saga " + sagaId
                );
            }
        }

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
                    "Inventory records missing for saga " + sagaId
            );
        }

        for (ItemInventory inventory : inventories) {
            inventory.setUnitsAvailable(
                    inventory.getUnitsAvailable()
                            + quantityMap.get(inventory.getId())
            );
        }

        itemInventoryRepository.saveAll(inventories);

        saga.setSagaStatus(OrderSagaStatus.INVENTORY_RELEASED);
        orderSagaRepository.save(saga);

        log.info(
                "Recovered stale inventory reservation for saga {}",
                sagaId
        );

        return true;
    }
}
