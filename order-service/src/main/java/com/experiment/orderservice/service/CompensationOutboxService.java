package com.experiment.orderservice.service;

import com.experiment.orderservice.entity.OutboxEvent;
import com.experiment.orderservice.messaging.event.EventType;
import com.experiment.orderservice.messaging.event.ReleaseInventoryEvent;
import com.experiment.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompensationOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createInventoryReleaseEvent(
            UUID sagaId
    ){

        UUID eventId = UUID.randomUUID();

        ReleaseInventoryEvent event =
                new ReleaseInventoryEvent(
                        eventId,
                        sagaId
                );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .eventType(EventType.RELEASE_INVENTORY)
                .payload(
                        objectMapper.convertValue(
                                event,
                                new tools.jackson.core.type.TypeReference<Map<String, Object>>() {}
                        )
                )
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
