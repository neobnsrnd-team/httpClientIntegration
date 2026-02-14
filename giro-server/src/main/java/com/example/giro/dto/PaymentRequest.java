package com.example.giro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @JsonProperty("bill_no")
    private String billNo;

    @JsonProperty("pay_amt")
    private long payAmt;
}
