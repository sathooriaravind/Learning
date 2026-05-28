package org.example.paymentsimplementation.repository;

import org.example.paymentsimplementation.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);
}