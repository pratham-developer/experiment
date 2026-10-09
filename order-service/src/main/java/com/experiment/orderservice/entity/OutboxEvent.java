package com.experiment.orderservice.entity;

import com.experiment.orderservice.messaging.event.EventType;
import tools.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
@Table(name = "outbox_events")
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    @Id
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = false;
}