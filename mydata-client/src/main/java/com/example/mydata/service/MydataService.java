package com.example.mydata.service;

import com.example.mydata.client.bank.BankMessageClient;
import com.example.mydata.client.card.CardMessageClient;
import com.example.mydata.client.insurance.InsuranceMessageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MydataService {

    private final BankMessageClient bankMessageClient;
    private final CardMessageClient cardMessageClient;
    private final InsuranceMessageClient insuranceMessageClient;

    // ========== Banking ==========

    public Map<String, Object> getAccountList() {
        log.info("[Banking] 계좌목록조회 요청");
        return bankMessageClient.request("계좌목록조회", Map.of());
    }

    public Map<String, Object> transfer(String fromAccountNo, String toAccountNo, long amount) {
        log.info("[Banking] 이체 요청: {} -> {} ({}원)", fromAccountNo, toAccountNo, amount);
        return bankMessageClient.request("이체", Map.of(
                "fromAccountNo", fromAccountNo,
                "toAccountNo", toAccountNo,
                "amount", amount
        ));
    }

    public Map<String, Object> getTransactionHistory(String accountNo, String fromDate, String toDate) {
        log.info("[Banking] 계좌거래내역조회 요청: accountNo={}", accountNo);
        return bankMessageClient.request("계좌거래내역조회", Map.of(
                "accountNo", accountNo,
                "fromDate", fromDate,
                "toDate", toDate
        ));
    }

    // ========== Card ==========

    public Map<String, Object> getCardList() {
        log.info("[Card] 보유카드목록조회 요청");
        return cardMessageClient.request("보유카드목록조회", Map.of());
    }

    public Map<String, Object> getScheduledPayment(String cardNo) {
        log.info("[Card] 결제예정금액조회 요청: cardNo={}", cardNo);
        return cardMessageClient.request("결제예정금액조회", Map.of(
                "cardNo", cardNo
        ));
    }

    // ========== Insurance ==========

    public Map<String, Object> getPolicyList(String customerId) {
        log.info("[Insurance] 보험가입내역조회 요청: customerId={}", customerId);
        return insuranceMessageClient.request("보험가입내역조회", Map.of(
                "customerId", customerId
        ));
    }

    public Map<String, Object> payPremium(String policyNo, long amount) {
        log.info("[Insurance] 보험료납부 요청: policyNo={}, amount={}", policyNo, amount);
        return insuranceMessageClient.request("보험료납부", Map.of(
                "policyNo", policyNo,
                "amount", amount
        ));
    }
}
