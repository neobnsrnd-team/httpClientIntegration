package com.example.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPaymentRequest {
    private String policyNo;
    private long amount;
}
