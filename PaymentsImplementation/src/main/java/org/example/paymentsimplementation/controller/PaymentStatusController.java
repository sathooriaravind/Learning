package org.example.paymentsimplementation.controller;

import lombok.RequiredArgsConstructor;
import org.example.paymentsimplementation.entity.Order;
import org.example.paymentsimplementation.repository.OrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PaymentStatusController {

    private final OrderRepository orderRepository;

    @GetMapping("/payment-status")
    public String paymentStatus(@RequestParam("razorpay_payment_link_id") String paymentLinkId, Model model) {
        Order order = orderRepository.findByRazorpayPaymentLinkId(paymentLinkId).orElse(null);

        if (order == null) {
            model.addAttribute("status", "UNKNOWN");
            model.addAttribute("message", "Order not found.");
            return "payment-status";
        }

        model.addAttribute("status", order.getStatus().name());
        model.addAttribute("orderId", order.getOrderId());
        model.addAttribute("amount", order.getAmount() / 100);

        return "payment-status";
    }
}