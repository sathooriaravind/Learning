package org.example.paymentsimplementation.dto;

import lombok.Builder;
import lombok.Data;
import org.example.paymentsimplementation.entity.OrderStatus;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String paymentLink;
    private Long amount;
    private OrderStatus status;
}