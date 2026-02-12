package com.example.banking.controller;

import com.example.banking.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bank")
public class BankController {

    @GetMapping("/accounts")
    public ResponseEntity<BankResponse<List<AccountDto>>> getAccounts() {
        log.info("[Banking] GET /api/bank/accounts - 계좌목록조회 요청");
        List<AccountDto> accounts = List.of(
                AccountDto.builder()
                        .accountNo("110-234-567890")
                        .accountName("급여계좌")
                        .bankCode("004")
                        .balance(1_500_000L)
                        .build(),
                AccountDto.builder()
                        .accountNo("110-987-654321")
                        .accountName("저축계좌")
                        .bankCode("004")
                        .balance(5_000_000L)
                        .build(),
                AccountDto.builder()
                        .accountNo("220-111-222333")
                        .accountName("생활비계좌")
                        .bankCode("011")
                        .balance(800_000L)
                        .build()
        );
        log.info("[Banking] GET /api/bank/accounts - 응답: {} 건", accounts.size());
        return ResponseEntity.ok(BankResponse.success(accounts));
    }

    @PostMapping("/transfer")
    public ResponseEntity<BankResponse<?>> transfer(@RequestBody TransferRequest request) {
        log.info("[Banking] POST /api/bank/transfer - 이체 요청: from={}, to={}, amount={}",
                request.getFromAccountNo(), request.getToAccountNo(), request.getAmount());

        // 존재하지 않는 출금계좌
        if ("999-999-999".equals(request.getFromAccountNo())) {
            log.warn("[Banking] 이체 실패 - E001: 출금계좌를 찾을 수 없습니다 ({})", request.getFromAccountNo());
            return ResponseEntity.ok(BankResponse.error("E001", "출금계좌를 찾을 수 없습니다"));
        }
        // 이체한도 초과 (1천만원)
        if (request.getAmount() > 10_000_000L) {
            log.warn("[Banking] 이체 실패 - E002: 이체한도 초과 (amount={})", request.getAmount());
            return ResponseEntity.ok(BankResponse.error("E002", "이체한도를 초과하였습니다"));
        }
        // 잔액부족
        if (request.getAmount() > 5_000_000L) {
            log.warn("[Banking] 이체 실패 - E003: 잔액부족 (amount={})", request.getAmount());
            return ResponseEntity.ok(BankResponse.error("E003", "잔액이 부족합니다"));
        }

        TransferResult result = TransferResult.builder()
                .transactionId("TXN" + System.currentTimeMillis())
                .status("COMPLETED")
                .transferredAmount(request.getAmount())
                .build();
        log.info("[Banking] 이체 성공 - txnId={}, amount={}", result.getTransactionId(), result.getTransferredAmount());
        return ResponseEntity.ok(BankResponse.success(result));
    }

    @GetMapping("/accounts/{accountNo}/transactions")
    public ResponseEntity<BankResponse<?>> getTransactions(
            @PathVariable String accountNo,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        log.info("[Banking] GET /api/bank/accounts/{}/transactions - 거래내역조회 요청 (from={}, to={})",
                accountNo, fromDate, toDate);

        // 존재하지 않는 계좌
        if ("999-999-999".equals(accountNo)) {
            log.warn("[Banking] 거래내역조회 실패 - E001: 계좌를 찾을 수 없습니다 ({})", accountNo);
            return ResponseEntity.ok(BankResponse.error("E001", "계좌를 찾을 수 없습니다"));
        }

        String baseDate = fromDate != null ? fromDate : LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<TransactionDto> transactions = List.of(
                TransactionDto.builder()
                        .transactionId("TR001")
                        .transactionDate(baseDate)
                        .description("급여입금")
                        .transactionType("DEPOSIT")
                        .amount(3_500_000L)
                        .balanceAfter(5_000_000L)
                        .build(),
                TransactionDto.builder()
                        .transactionId("TR002")
                        .transactionDate(baseDate)
                        .description("카드대금")
                        .transactionType("WITHDRAWAL")
                        .amount(450_000L)
                        .balanceAfter(4_550_000L)
                        .build(),
                TransactionDto.builder()
                        .transactionId("TR003")
                        .transactionDate(baseDate)
                        .description("공과금납부")
                        .transactionType("WITHDRAWAL")
                        .amount(120_000L)
                        .balanceAfter(4_430_000L)
                        .build()
        );
        log.info("[Banking] 거래내역조회 응답: {} 건", transactions.size());
        return ResponseEntity.ok(BankResponse.success(transactions));
    }
}
