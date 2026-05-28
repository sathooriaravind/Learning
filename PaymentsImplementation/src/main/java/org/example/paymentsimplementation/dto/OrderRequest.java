package org.example.paymentsimplementation.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private String customerName;
    private String customerEmail;
    private Long amount; // in INR, converted to paise in service
}