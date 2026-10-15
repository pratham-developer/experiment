package com.experiment.inventoryservice.repository;

import com.experiment.inventoryservice.entity.OrderSaga;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
