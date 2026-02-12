package com.example.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String cardNo;
    private String cardName;
    private String cardType;
    private String issuerName;
    private long monthlyLimit;
    private long usedAmount;
}
