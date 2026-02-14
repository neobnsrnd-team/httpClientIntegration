package com.example.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentResult {
    private String paymentId;
    private String policyNo;
    private long paidAmount;
    private String paymentDate;
    private String status;
}
