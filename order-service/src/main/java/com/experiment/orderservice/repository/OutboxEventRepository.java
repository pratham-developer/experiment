package com.experiment.orderservice.repository;

import com.experiment.orderservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {
}