package com.example.insurance.controller;

import com.example.insurance.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/insurance")
public class InsuranceController {

    @GetMapping("/policies")
    public ResponseEntity<InsuranceResponse<?>> getPolicies(
            @RequestParam(required = false) String customerId) {

        log.info("[Insurance] GET /api/insurance/policies - 보험가입내역조회 요청: customerId={}", customerId);

        // customerId 누락/빈값 → 400
        if (customerId == null || customerId.isBlank()) {
            log.warn("[Insurance] 보험가입내역조회 실패 - customerId 누락");
            return ResponseEntity.badRequest().body(
                    InsuranceResponse.error("BAD_REQUEST", "customerId는 필수 파라미터입니다"));
        }

        // 시스템 장애 시뮬레이션 → 500
        if ("ERROR".equals(customerId)) {
            log.error("[Insurance] 보험가입내역조회 실패 - 시스템 장애 시뮬레이션");
            return ResponseEntity.internalServerError().body(
                    InsuranceResponse.error("SYS001", "시스템 장애가 발생했습니다"));
        }

        // 고객 미존재 → 200 + 비즈니스 에러
        if ("9999999".equals(customerId)) {
            log.warn("[Insurance] 보험가입내역조회 실패 - INS001: 고객을 찾을 수 없습니다 ({})", customerId);
            return ResponseEntity.ok(InsuranceResponse.error("INS001", "고객을 찾을 수 없습니다"));
        }

        // 정상 응답
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<PolicyDto> policies = List.of(
                PolicyDto.builder()
                        .policyNo("POL-2024-001")
                        .policyName("무배당 건강보험")
                        .insuranceType("HEALTH")
                        .startDate("20240101")
                        .endDate("20340101")
                        .premiumAmount(150_000L)
                        .status("ACTIVE")
                        .build(),
                PolicyDto.builder()
                        .policyNo("POL-2024-002")
                        .policyName("운전자보험")
                        .insuranceType("AUTO")
                        .startDate("20240301")
                        .endDate("20250301")
                        .premiumAmount(80_000L)
                        .status("ACTIVE")
                        .build()
        );

        log.info("[Insurance] 보험가입내역조회 응답: {} 건", policies.size());
        return ResponseEntity.ok(InsuranceResponse.success(policies));
    }

    @PostMapping("/premium-payment")
    public ResponseEntity<InsuranceResponse<?>> payPremium(@RequestBody PremiumPaymentRequest request) {
        log.info("[Insurance] POST /api/insurance/premium-payment - 보험료납부 요청: policyNo={}, amount={}",
                request.getPolicyNo(), request.getAmount());

        // 시스템 장애 시뮬레이션 → 500
        if ("ERROR".equals(request.getPolicyNo())) {
            log.error("[Insurance] 보험료납부 실패 - 시스템 장애 시뮬레이션");
            return ResponseEntity.internalServerError().body(
                    InsuranceResponse.error("SYS001", "시스템 장애가 발생했습니다"));
        }

        // 보험 미존재 → 200 + 비즈니스 에러
        if ("POL-9999-999".equals(request.getPolicyNo())) {
            log.warn("[Insurance] 보험료납부 실패 - INS001: 보험을 찾을 수 없습니다 ({})", request.getPolicyNo());
            return ResponseEntity.ok(InsuranceResponse.error("INS001", "보험을 찾을 수 없습니다"));
        }

        // 만료된 보험 → 200 + 비즈니스 에러
        if ("POL-EXPIRED-001".equals(request.getPolicyNo())) {
            log.warn("[Insurance] 보험료납부 실패 - INS002: 만료된 보험입니다 ({})", request.getPolicyNo());
            return ResponseEntity.ok(InsuranceResponse.error("INS002", "만료된 보험입니다"));
        }

        // 금액 오류 → 200 + 비즈니스 에러
        if (request.getAmount() <= 0 || request.getAmount() > 10_000_000) {
            log.warn("[Insurance] 보험료납부 실패 - INS003: 납부 금액이 올바르지 않습니다 (amount={})", request.getAmount());
            return ResponseEntity.ok(InsuranceResponse.error("INS003", "납부 금액이 올바르지 않습니다"));
        }

        // 정상 납부
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        PremiumPaymentResult result = PremiumPaymentResult.builder()
                .paymentId("PAY" + System.currentTimeMillis())
                .policyNo(request.getPolicyNo())
                .paidAmount(request.getAmount())
                .paymentDate(today)
                .status("COMPLETED")
                .build();

        log.info("[Insurance] 보험료납부 성공 - paymentId={}, amount={}", result.getPaymentId(), result.getPaidAmount());
        return ResponseEntity.ok(InsuranceResponse.success(result));
    }
}
