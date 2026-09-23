package com.experiment.inventoryservice.repository;

import com.experiment.inventoryservice.entity.ItemInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ItemInventoryRepository extends JpaRepository<ItemInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from ItemInventory i
            where i.id in :ids
            order by i.id asc
            """)
    List<ItemInventory> findAllByIdForUpdate(
            @Param("ids") Collection<Long> ids
    );
}
