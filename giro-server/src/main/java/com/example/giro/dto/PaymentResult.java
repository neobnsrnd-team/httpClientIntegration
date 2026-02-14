package com.example.giro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    @JsonProperty("pay_no")
    private String payNo;

    @JsonProperty("bill_no")
    private String billNo;

    @JsonProperty("pay_amt")
    private long payAmt;

    @JsonProperty("pay_dt")
    private String payDt;

    @JsonProperty("pay_st")
    private String paySt;
}
