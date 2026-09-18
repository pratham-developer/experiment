package com.experiment.inventoryservice.repostiory;

import com.experiment.inventoryservice.entity.ItemInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemInventoryRepository extends JpaRepository<ItemInventory, Long> {
}
