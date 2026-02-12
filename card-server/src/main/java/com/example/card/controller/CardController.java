package com.example.card.controller;

import com.example.card.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/card")
public class CardController {

    @GetMapping("/cards")
    public ResponseEntity<CardResponse<List<CardDto>>> getCards() {
        log.info("[Card] GET /api/card/cards - 보유카드목록조회 요청");
        List<CardDto> cards = List.of(
                CardDto.builder()
                        .cardNo("1234-5678-9012-3456")
                        .cardName("신한 Deep Dream")
                        .cardType("CREDIT")
                        .issuerName("신한카드")
                        .monthlyLimit(5_000_000L)
                        .usedAmount(1_200_000L)
                        .build(),
                CardDto.builder()
                        .cardNo("9876-5432-1098-7654")
                        .cardName("삼성 taptap O")
                        .cardType("CREDIT")
                        .issuerName("삼성카드")
                        .monthlyLimit(3_000_000L)
                        .usedAmount(800_000L)
                        .build()
        );
        log.info("[Card] GET /api/card/cards - 응답: {} 건", cards.size());
        return ResponseEntity.ok(CardResponse.success(cards));
    }

    @GetMapping("/cards/{cardNo}/scheduled-payments")
    public ResponseEntity<CardResponse<?>> getScheduledPayments(@PathVariable String cardNo) {
        log.info("[Card] GET /api/card/cards/{}/scheduled-payments - 결제예정금액조회 요청", cardNo);

        // 존재하지 않는 카드
        if ("9999-9999-9999-9999".equals(cardNo)) {
            log.warn("[Card] 결제예정금액조회 실패 - CARD_NOT_FOUND: 카드를 찾을 수 없습니다 ({})", cardNo);
            return ResponseEntity.ok(CardResponse.error("CARD_NOT_FOUND", "카드를 찾을 수 없습니다"));
        }
        // 해지된 카드
        if ("0000-0000-0000-0000".equals(cardNo)) {
            log.warn("[Card] 결제예정금액조회 실패 - CARD_CANCELLED: 해지된 카드입니다 ({})", cardNo);
            return ResponseEntity.ok(CardResponse.error("CARD_CANCELLED", "해지된 카드입니다"));
        }

        String nextPaymentDate = LocalDate.now().plusMonths(1).withDayOfMonth(15)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        ScheduledPaymentDto payment = ScheduledPaymentDto.builder()
                .cardNo(cardNo)
                .paymentDate(nextPaymentDate)
                .totalAmount(650_000L)
                .details(List.of(
                        ScheduledPaymentDto.PaymentDetail.builder()
                                .merchantName("쿠팡")
                                .transactionDate("20240301")
                                .amount(150_000L)
                                .installmentMonth(1)
                                .totalInstallments(1)
                                .build(),
                        ScheduledPaymentDto.PaymentDetail.builder()
                                .merchantName("하이마트")
                                .transactionDate("20240215")
                                .amount(500_000L)
                                .installmentMonth(2)
                                .totalInstallments(3)
                                .build()
                ))
                .build();
        log.info("[Card] 결제예정금액조회 응답: cardNo={}, totalAmount={}", cardNo, payment.getTotalAmount());
        return ResponseEntity.ok(CardResponse.success(payment));
    }
}
