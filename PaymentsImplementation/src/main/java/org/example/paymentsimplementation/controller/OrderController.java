package org.example.paymentsimplementation.controller;

import lombok.RequiredArgsConstructor;
import org.example.paymentsimplementation.dto.OrderRequest;
import org.example.paymentsimplementation.dto.OrderResponse;
import org.example.paymentsimplementation.entity.Order;
import org.example.paymentsimplementation.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        try {
            Order order = orderService.createOrder(
                    request.getAmount(), // convert INR to paise
                    request.getCustomerName(),
                    request.getCustomerEmail()
            );

            OrderResponse response = OrderResponse.builder()
                    .orderId(order.getOrderId())
                    .paymentLink(order.getPaymentLinkUrl())
                    .amount(order.getAmount() / 100) // back to INR for response
                    .status(order.getStatus())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}