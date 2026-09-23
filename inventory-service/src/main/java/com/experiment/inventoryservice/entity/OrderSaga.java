package com.experiment.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_saga")
@EntityListeners(AuditingEntityListener.class)
public class OrderSaga {
    @Id
    private UUID sagaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSagaStatus sagaStatus;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
