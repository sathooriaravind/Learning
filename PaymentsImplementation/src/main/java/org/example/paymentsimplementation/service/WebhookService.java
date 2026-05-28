package org.example.paymentsimplementation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentsimplementation.entity.Order;
import org.example.paymentsimplementation.entity.OrderStatus;
import org.example.paymentsimplementation.entity.PaymentEvent;
import org.example.paymentsimplementation.repository.OrderRepository;
import org.example.paymentsimplementation.repository.PaymentEventRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final OrderRepository orderRepository;
    private final PaymentEventRepository paymentEventRepository;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    public void handleWebhook(String payload, String razorpaySignature) throws Exception {
        // Step 1: Verify signature
        if (!isValidSignature(payload, razorpaySignature)) {
            log.warn("Invalid webhook signature received");
            throw new SecurityException("Invalid webhook signature");
        }

        JSONObject body = new JSONObject(payload);
        String event = body.getString("event");
        JSONObject paymentLinkEntity = body.getJSONObject("payload")
                .getJSONObject("payment_link")
                .getJSONObject("entity");

        String paymentLinkId = paymentLinkEntity.getString("id");

        // Step 2: Idempotency check
        if (paymentEventRepository.existsByPaymentLinkId(paymentLinkId)) {
            log.info("Duplicate webhook received for paymentLinkId: {}, skipping", paymentLinkId);
            return;
        }

        // Step 3: Handle event
        Order order = orderRepository.findByRazorpayPaymentLinkId(paymentLinkId)
                .orElseThrow(() -> new RuntimeException("Order not found for paymentLinkId: " + paymentLinkId));

        if ("payment_link.paid".equals(event)) {
            handlePaymentSuccess(order);
        } else if ("payment_link.cancelled".equals(event) || "payment_link.expired".equals(event)) {
            handlePaymentFailure(order, event);
        } else {
            log.info("Unhandled event type: {}", event);
        }

        // Step 4: Save to payment events to prevent duplicate processing
        paymentEventRepository.save(PaymentEvent.builder()
                .paymentLinkId(paymentLinkId)
                .status(order.getStatus().name())
                .processedAt(LocalDateTime.now())
                .build());
    }

    private void handlePaymentSuccess(Order order) {
        order.setStatus(OrderStatus.SUCCESS);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Payment SUCCESS for order: {}", order.getOrderId());
        // TODO: trigger downstream — send email, notify fulfillment service, etc.
    }

    private void handlePaymentFailure(Order order, String event) {
        order.setStatus(OrderStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Payment FAILED/CANCELLED for order: {}, event: {}", order.getOrderId(), event);
        // TODO: release inventory, notify user, etc.
    }

    private boolean isValidSignature(String payload, String razorpaySignature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes());
        String computedSignature = HexFormat.of().formatHex(hash);
        return computedSignature.equals(razorpaySignature);
    }
}