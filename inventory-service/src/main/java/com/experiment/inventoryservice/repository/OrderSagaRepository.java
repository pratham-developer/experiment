package com.experiment.inventoryservice.repository;

import com.experiment.inventoryservice.entity.OrderSaga;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderSagaRepository extends JpaRepository<OrderSaga, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select s
           from OrderSaga s
           where s.sagaId = :sagaId
           """)
    Optional<OrderSaga> findBySagaIdForUpdate(
            @Param("sagaId") UUID sagaId
    );

    @Query(value = """
    SELECT *
    FROM order_saga
    WHERE saga_status = 'INVENTORY_RESERVED'
      AND created_at <= CURRENT_TIMESTAMP - (INTERVAL '1 hour' * :hours)
    ORDER BY created_at ASC
    LIMIT 100
    """, nativeQuery = true)
    List<OrderSaga> findTop100StaleReservationsByHours(@Param("hours") int hours);
}
