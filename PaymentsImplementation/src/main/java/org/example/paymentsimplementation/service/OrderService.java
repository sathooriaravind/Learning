package org.example.paymentsimplementation.service;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentsimplementation.entity.Order;
import org.example.paymentsimplementation.entity.OrderStatus;
import org.example.paymentsimplementation.repository.OrderRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public Order createOrder(Long amount, String customerName, String customerEmail) throws Exception {
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject paymentLinkRequest = new JSONObject();
        paymentLinkRequest.put("amount", amount); // amount in paise
        paymentLinkRequest.put("currency", "INR");
        paymentLinkRequest.put("description", "Payment for order");

        JSONObject customer = new JSONObject();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        paymentLinkRequest.put("customer", customer);

        paymentLinkRequest.put("callback_url", appBaseUrl + "/payment-status");
        paymentLinkRequest.put("callback_method", "get");

        PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);

        String internalOrderId = UUID.randomUUID().toString();
        String razorpayPaymentLinkId = paymentLink.get("id");
        String paymentLinkUrl = paymentLink.get("short_url");

        Order order = Order.builder()
                .orderId(internalOrderId)
                .razorpayPaymentLinkId(razorpayPaymentLinkId)
                .paymentLinkUrl(paymentLinkUrl)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("Order created: {} with payment link: {}", internalOrderId, paymentLinkUrl);

        return order;
    }
}