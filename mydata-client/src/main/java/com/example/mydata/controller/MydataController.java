package com.example.mydata.controller;

import com.example.mydata.dto.MydataResponse;
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
    public ResponseEntity<MydataResponse<?>> getAccountList() {
        return ResponseEntity.ok(MydataResponse.success(mydataService.getAccountList()));
    }

    @PostMapping("/bank/transfer")
    public ResponseEntity<MydataResponse<?>> transfer(@RequestBody Map<String, Object> request) {
        String fromAccountNo = (String) request.get("fromAccountNo");
        String toAccountNo = (String) request.get("toAccountNo");
        long amount = ((Number) request.get("amount")).longValue();
        return ResponseEntity.ok(MydataResponse.success(mydataService.transfer(fromAccountNo, toAccountNo, amount)));
    }

    @GetMapping("/bank/accounts/{accountNo}/transactions")
    public ResponseEntity<MydataResponse<?>> getTransactionHistory(
            @PathVariable String accountNo,
            @RequestParam(defaultValue = "20240101") String fromDate,
            @RequestParam(defaultValue = "20241231") String toDate) {
        return ResponseEntity.ok(MydataResponse.success(mydataService.getTransactionHistory(accountNo, fromDate, toDate)));
    }

    // ========== Card ==========

    @GetMapping("/card/cards")
    public ResponseEntity<MydataResponse<?>> getCardList() {
        return ResponseEntity.ok(MydataResponse.success(mydataService.getCardList()));
    }

    @GetMapping("/card/cards/{cardNo}/scheduled-payments")
    public ResponseEntity<MydataResponse<?>> getScheduledPayment(@PathVariable String cardNo) {
        return ResponseEntity.ok(MydataResponse.success(mydataService.getScheduledPayment(cardNo)));
    }

    // ========== Insurance ==========

    @GetMapping("/insurance/policies")
    public ResponseEntity<MydataResponse<?>> getPolicyList(@RequestParam String customerId) {
        return ResponseEntity.ok(MydataResponse.success(mydataService.getPolicyList(customerId)));
    }

    @PostMapping("/insurance/premium-payment")
    public ResponseEntity<MydataResponse<?>> payPremium(@RequestBody Map<String, Object> request) {
        String policyNo = (String) request.get("policyNo");
        long amount = ((Number) request.get("amount")).longValue();
        return ResponseEntity.ok(MydataResponse.success(mydataService.payPremium(policyNo, amount)));
    }
}
