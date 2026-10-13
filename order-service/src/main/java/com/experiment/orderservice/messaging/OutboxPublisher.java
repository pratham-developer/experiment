package com.experiment.orderservice.messaging;

import com.experiment.orderservice.entity.OutboxEvent;
import com.experiment.orderservice.repository.OutboxEventRepository;
import com.experiment.orderservice.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventService outboxEventService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60000)
    public void publishPendingEvents() {

        //get pending events
        List<OutboxEvent> events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        if (events.isEmpty()) {
            return;
        }

        //publish events
        List<UUID> successfullyPublishedEventIds = new ArrayList<>();
        for (OutboxEvent event : events) {
            try {
                CorrelationData correlationData =
                        new CorrelationData(event.getEventId().toString());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                        event.getEventType().name(),
                        objectMapper.writeValueAsString(event.getPayload()),
                        message -> {
                            message.getMessageProperties().setContentType(
                                    MessageProperties.CONTENT_TYPE_JSON
                            );
                            return message;
                        },
                        correlationData
                );

                //block synchronously for ack with timeout
                CorrelationData.Confirm confirm =
                        correlationData.getFuture()
                                .get(5, TimeUnit.SECONDS);

                if (!confirm.ack()) {
                    log.warn(
                            "RabbitMQ negatively acknowledged event {}: {}",
                            event.getEventId(),
                            confirm.reason()
                    );
                    continue;
                }

                //broker ack alone does not guarantee successful routing.
                if (correlationData.getReturned() != null) {
                    log.warn("RabbitMQ returned unroutable event {}", event.getEventId());
                    continue;
                }

                //add to successfully published list of ids
                successfullyPublishedEventIds.add(event.getEventId());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Publisher interrupted while waiting for confirmation");
                break; //stop loop if the thread is interrupted

            } catch (TimeoutException e) {
                log.warn("Timed out waiting for RabbitMQ confirmation for event {}", event.getEventId());

            } catch (ExecutionException e) {
                log.error("Could not obtain RabbitMQ confirmation for event {}", event.getEventId(), e);

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getEventId(), e);
            }
        }

        //mark as published
        if (!successfullyPublishedEventIds.isEmpty()) {
            try {
                outboxEventService.markAllAsPublished(successfullyPublishedEventIds);
                log.info("Marked {} outbox events as published", successfullyPublishedEventIds.size());
            } catch (Exception e) {
                //events remain pending and may be delivered again.
                log.error("Failed to mark published outbox events in the database", e);
            }
        }
    }
}