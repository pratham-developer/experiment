package com.experiment.orderservice.dto;

import java.util.List;

public record PlaceOrderRequest(
        List<OrderItemDto> items
) {
}
