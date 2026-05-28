package org.example.paymentsimplementation.repository;

import org.example.paymentsimplementation.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    boolean existsByPaymentLinkId(String paymentLinkId);
}