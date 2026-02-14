package com.example.insurance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDto {
    private String policyNo;
    private String policyName;
    private String insuranceType;
    private String startDate;
    private String endDate;
    private long premiumAmount;
    private String status;
}
