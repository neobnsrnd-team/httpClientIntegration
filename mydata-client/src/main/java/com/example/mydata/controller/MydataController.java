package com.example.mydata.controller;

import com.example.mydata.client.core.ExternalSystemException;
import com.example.mydata.service.MydataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mydata")
@RequiredArgsConstructor
public class MydataController {

    private final MydataService mydataService;

    // ========== Banking ==========

    @GetMapping("/bank/accounts")
    public ResponseEntity<Map<String, Object>> getAccountList() {
        return ResponseEntity.ok(mydataService.getAccountList());
    }

    @PostMapping("/bank/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> request) {
        String fromAccountNo = (String) request.get("fromAccountNo");
        String toAccountNo = (String) request.get("toAccountNo");
        long amount = ((Number) request.get("amount")).longValue();
        return ResponseEntity.ok(mydataService.transfer(fromAccountNo, toAccountNo, amount));
    }

    @GetMapping("/bank/accounts/{accountNo}/transactions")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(
            @PathVariable String accountNo,
            @RequestParam(defaultValue = "20240101") String fromDate,
            @RequestParam(defaultValue = "20241231") String toDate) {
        return ResponseEntity.ok(mydataService.getTransactionHistory(accountNo, fromDate, toDate));
    }

    // ========== Card ==========

    @GetMapping("/card/cards")
    public ResponseEntity<Map<String, Object>> getCardList() {
        return ResponseEntity.ok(mydataService.getCardList());
    }

    @GetMapping("/card/cards/{cardNo}/scheduled-payments")
    public ResponseEntity<Map<String, Object>> getScheduledPayment(@PathVariable String cardNo) {
        return ResponseEntity.ok(mydataService.getScheduledPayment(cardNo));
    }

    // ========== Exception Handler ==========

    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<Map<String, Object>> handleExternalSystemException(ExternalSystemException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "errorCode", e.getErrorCode(),
                "errorMessage", e.getErrorMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "errorCode", "INVALID_REQUEST",
                "errorMessage", e.getMessage()
        ));
    }
}
