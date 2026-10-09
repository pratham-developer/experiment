package com.experiment.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceOrderRequest(

        @NotEmpty(message = "Address is required")
        String address,

        @NotEmpty(message = "At least one item is required")
        List<@Valid OrderItemDto> items

) {
}
