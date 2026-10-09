package com.experiment.orderservice.service;

import com.experiment.microservicesecuritystarter.security.SecurityContext;
import com.experiment.microservicesecuritystarter.security.SecurityPrincipal;
import com.experiment.orderservice.dto.PlaceOrderRequest;
import com.experiment.orderservice.dto.ReserveInventoryResponse;
import com.experiment.orderservice.entity.Order;
import com.experiment.orderservice.entity.OrderItem;
import com.experiment.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final SecurityContext securityContext;
    private final OrderRepository orderRepository;

    @Transactional
    public void createOrder(PlaceOrderRequest request, ReserveInventoryResponse reserveResponse) {
        SecurityPrincipal customer = securityContext.require();

        Order order = Order.builder()
                .customerId(customer.userId())
                .address(request.address())
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = reserveResponse.inventories()
                .stream()
                .map(
                        inventory -> OrderItem.builder()
                                .itemId(inventory.itemId())
                                .inventoryId(inventory.inventoryId())
                                .sellerId(inventory.sellerId())
                                .price(inventory.price())
                                .unitsOrdered(inventory.unitsReserved())
                                .order(order)
                                .build()
                ).toList();

        order.setOrderItems(orderItems);

        BigDecimal totalAmount = reserveResponse.inventories().stream()
                .map(
                        inventory->
                                inventory.price().multiply(
                                        BigDecimal.valueOf(inventory.unitsReserved())
                                )
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        orderRepository.save(order);
    }
}
