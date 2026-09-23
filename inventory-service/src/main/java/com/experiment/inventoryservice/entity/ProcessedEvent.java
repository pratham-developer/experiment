package com.experiment.inventoryservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "processed_events")
@EntityListeners(AuditingEntityListener.class)
public class ProcessedEvent {

    @Id
    private UUID eventId;

    @CreatedDate
    private Instant processedAt;
}
