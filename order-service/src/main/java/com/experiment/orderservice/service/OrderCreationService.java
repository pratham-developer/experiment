package com.experiment.orderservice.service;

import com.experiment.microservicesecuritystarter.security.SecurityContext;
import com.experiment.microservicesecuritystarter.security.SecurityPrincipal;
import com.experiment.orderservice.dto.PlaceOrderRequest;
import com.experiment.orderservice.dto.ReserveInventoryResponse;
import com.experiment.orderservice.entity.Order;
import com.experiment.orderservice.entity.OrderItem;
import com.experiment.orderservice.entity.OutboxEvent;
import com.experiment.orderservice.messaging.event.EventType;
import com.experiment.orderservice.messaging.event.OrderCompletedEvent;
import com.experiment.orderservice.repository.OrderRepository;
import com.experiment.orderservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final SecurityContext securityContext;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createOrderAndEvent(
            UUID sagaId,
            PlaceOrderRequest request,
            ReserveInventoryResponse reserveResponse
    ) {
        SecurityPrincipal customer = securityContext.require();

        Order order = Order.builder()
                .customerId(customer.userId())
                .address(request.address())
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems =
                reserveResponse.inventories().stream()
                        .map(inventory -> OrderItem.builder()
                                .itemId(inventory.itemId())
                                .inventoryId(inventory.inventoryId())
                                .sellerId(inventory.sellerId())
                                .price(inventory.price())
                                .unitsOrdered(inventory.unitsReserved())
                                .order(order)
                                .build())
                        .toList();

        order.setOrderItems(orderItems);

        BigDecimal totalAmount =
                reserveResponse.inventories().stream()
                        .map(inventory ->
                                inventory.price().multiply(
                                        BigDecimal.valueOf(inventory.unitsReserved())
                                )
                        )
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);

        orderRepository.save(order);

        // Persist completion event in the same transaction as the order
        UUID eventId = UUID.randomUUID();

        OrderCompletedEvent event =
                new OrderCompletedEvent(eventId, sagaId);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .eventType(EventType.ORDER_COMPLETED)
                .payload(objectMapper.valueToTree(event))
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}