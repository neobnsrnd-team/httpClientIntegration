package com.example.giro.controller;

import com.example.giro.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/giro")
public class GiroController {

    @GetMapping("/bills")
    public ResponseEntity<GiroResponse<?>> getBills(
            @RequestParam(required = false) String cust_id) {

        log.info("[GIRO] GET /api/giro/bills - 지로청구서목록조회 요청: cust_id={}", cust_id);

        // cust_id 누락/빈값 → 400
        if (cust_id == null || cust_id.isBlank()) {
            log.warn("[GIRO] 지로청구서목록조회 실패 - cust_id 누락");
            return ResponseEntity.badRequest().body(
                    GiroResponse.error("BAD_REQUEST", "cust_id는 필수 파라미터입니다"));
        }

        // 시스템 장애 시뮬레이션 → 500
        if ("ERROR".equals(cust_id)) {
            log.error("[GIRO] 지로청구서목록조회 실패 - 시스템 장애 시뮬레이션");
            return ResponseEntity.internalServerError().body(
                    GiroResponse.error("SYS001", "시스템 장애가 발생했습니다"));
        }

        // 고객 미존재 → 200 + 비즈니스 에러
        if ("9999999".equals(cust_id)) {
            log.warn("[GIRO] 지로청구서목록조회 실패 - GIRO001: 고객을 찾을 수 없습니다 ({})", cust_id);
            return ResponseEntity.ok(GiroResponse.error("GIRO001", "고객을 찾을 수 없습니다"));
        }

        // 정상 응답
        List<BillDto> bills = List.of(
                BillDto.builder()
                        .billNo("BILL-2024-001")
                        .billNm("전기요금")
                        .payAmt(50000L)
                        .dueDt("20240430")
                        .paySt("UNPAID")
                        .orgNm("한국전력공사")
                        .build(),
                BillDto.builder()
                        .billNo("BILL-2024-002")
                        .billNm("수도요금")
                        .payAmt(30000L)
                        .dueDt("20240430")
                        .paySt("UNPAID")
                        .orgNm("서울시상수도사업본부")
                        .build(),
                BillDto.builder()
                        .billNo("BILL-2024-003")
                        .billNm("가스요금")
                        .payAmt(45000L)
                        .dueDt("20240515")
                        .paySt("PAID")
                        .orgNm("서울도시가스")
                        .build()
        );

        log.info("[GIRO] 지로청구서목록조회 응답: {} 건", bills.size());
        return ResponseEntity.ok(GiroResponse.success(bills));
    }

    @PostMapping("/payment")
    public ResponseEntity<GiroResponse<?>> payment(@RequestBody PaymentRequest request) {
        log.info("[GIRO] POST /api/giro/payment - 지로납부 요청: bill_no={}, pay_amt={}",
                request.getBillNo(), request.getPayAmt());

        // 시스템 장애 시뮬레이션 → 500
        if ("ERROR".equals(request.getBillNo())) {
            log.error("[GIRO] 지로납부 실패 - 시스템 장애 시뮬레이션");
            return ResponseEntity.internalServerError().body(
                    GiroResponse.error("SYS001", "시스템 장애가 발생했습니다"));
        }

        // 청구서 미존재 → 200 + 비즈니스 에러
        if ("BILL-9999-999".equals(request.getBillNo())) {
            log.warn("[GIRO] 지로납부 실패 - GIRO001: 청구서를 찾을 수 없습니다 ({})", request.getBillNo());
            return ResponseEntity.ok(GiroResponse.error("GIRO001", "청구서를 찾을 수 없습니다"));
        }

        // 납부기한 만료 → 200 + 비즈니스 에러
        if ("BILL-EXPIRED-001".equals(request.getBillNo())) {
            log.warn("[GIRO] 지로납부 실패 - GIRO002: 납부기한이 만료되었습니다 ({})", request.getBillNo());
            return ResponseEntity.ok(GiroResponse.error("GIRO002", "납부기한이 만료되었습니다"));
        }

        // 금액 오류 → 200 + 비즈니스 에러
        if (request.getPayAmt() <= 0 || request.getPayAmt() > 10_000_000) {
            log.warn("[GIRO] 지로납부 실패 - GIRO003: 납부 금액이 올바르지 않습니다 (pay_amt={})", request.getPayAmt());
            return ResponseEntity.ok(GiroResponse.error("GIRO003", "납부 금액이 올바르지 않습니다"));
        }

        // 정상 납부
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        PaymentResult result = PaymentResult.builder()
                .payNo("PAY" + System.currentTimeMillis())
                .billNo(request.getBillNo())
                .payAmt(request.getPayAmt())
                .payDt(today)
                .paySt("COMPLETED")
                .build();

        log.info("[GIRO] 지로납부 성공 - pay_no={}, pay_amt={}", result.getPayNo(), result.getPayAmt());
        return ResponseEntity.ok(GiroResponse.success(result));
    }
}
