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
public class BillDto {

    @JsonProperty("bill_no")
    private String billNo;

    @JsonProperty("bill_nm")
    private String billNm;

    @JsonProperty("pay_amt")
    private long payAmt;

    @JsonProperty("due_dt")
    private String dueDt;

    @JsonProperty("pay_st")
    private String paySt;

    @JsonProperty("org_nm")
    private String orgNm;
}
