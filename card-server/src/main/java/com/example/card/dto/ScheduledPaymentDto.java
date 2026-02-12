package com.example.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPaymentDto {
    private String cardNo;
    private String paymentDate;
    private long totalAmount;
    private List<PaymentDetail> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetail {
        private String merchantName;
        private String transactionDate;
        private long amount;
        private int installmentMonth;
        private int totalInstallments;
    }
}
