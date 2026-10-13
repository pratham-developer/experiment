package com.experiment.orderservice.service;

import com.experiment.orderservice.entity.OutboxEvent;
import com.experiment.orderservice.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markAllAsPublished(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return;
        }
        List<OutboxEvent> events =
                outboxEventRepository.findByEventIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            throw new IllegalStateException(
                    "One or more outbox events could not be found"
            );
        }

        Instant publishedAt = Instant.now();
        for (OutboxEvent event : events) {
            event.setPublished(true);
            event.setPublishedAt(publishedAt);
        }

        outboxEventRepository.saveAll(events);
    }
}
