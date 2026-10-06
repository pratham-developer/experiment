package com.experiment.orderservice.controller;

import com.experiment.microservicesecuritystarter.annotation.RequiresRole;
import com.experiment.microservicesecuritystarter.security.SecurityRole;
import com.experiment.orderservice.dto.PlaceOrderRequest;
import com.experiment.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @RequiresRole(SecurityRole.CUSTOMER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void placeOrder(
            @Valid @RequestBody PlaceOrderRequest request
    ) {
        orderService.placeOrder(request);
    }
}
