package com.example.mydata.client.bank;

import com.example.mydata.client.core.ExternalSystemException;
import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageSpecProperties;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("BankMessageClient 테스트")
class BankMessageClientTest {

    private BankMessageClient bankMessageClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GenericHttpClient httpClient = new GenericHttpClient(restClient);

        SystemProperties bankProperties = new SystemProperties();
        bankProperties.setBaseUrl("http://localhost:8081");
        bankProperties.setSuccessCodeField("result_code");
        bankProperties.setSuccessCodeValue("0000");
        bankProperties.setErrorMessageField("result_msg");
        bankProperties.setDataField("data");

        // 계좌목록조회
        MessageSpecProperties accountListSpec = new MessageSpecProperties();
        accountListSpec.setTransactionCode("계좌목록조회");
        accountListSpec.setMethod("GET");
        accountListSpec.setPath("/api/bank/accounts");

        // 이체
        MessageSpecProperties transferSpec = new MessageSpecProperties();
        transferSpec.setTransactionCode("이체");
        transferSpec.setMethod("POST");
        transferSpec.setPath("/api/bank/transfer");
        transferSpec.setBodyFields(Map.of(
                "fromAccountNo", "fromAccountNo",
                "toAccountNo", "toAccountNo",
                "amount", "amount"
        ));

        // 계좌거래내역조회
        MessageSpecProperties txHistorySpec = new MessageSpecProperties();
        txHistorySpec.setTransactionCode("계좌거래내역조회");
        txHistorySpec.setMethod("GET");
        txHistorySpec.setPath("/api/bank/accounts/{accountNo}/transactions");
        txHistorySpec.setPathVariables(List.of("accountNo"));
        LinkedHashMap<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("fromDate", "fromDate");
        queryParams.put("toDate", "toDate");
        txHistorySpec.setQueryParams(queryParams);

        bankProperties.setMessages(Map.of(
                "account-list", accountListSpec,
                "transfer", transferSpec,
                "account-transactions", txHistorySpec
        ));

        bankMessageClient = new BankMessageClient(httpClient, bankProperties, objectMapper);
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("계좌목록조회 - 정상 응답 시 계좌 목록을 반환한다")
        void getAccountList_success() {
            String response = """
                    {
                        "result_code": "0000",
                        "result_msg": "성공",
                        "data": [
                            {"accountNo": "110-234-567890", "accountName": "급여계좌", "balance": 1500000},
                            {"accountNo": "110-987-654321", "accountName": "저축계좌", "balance": 5000000}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/bank/accounts"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = bankMessageClient.request("계좌목록조회", Map.of());

            assertNotNull(result);
            assertTrue(result.containsKey("items"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(2, items.size());

            mockServer.verify();
        }

        @Test
        @DisplayName("이체 - 정상 이체 시 거래 결과를 반환한다")
        void transfer_success() {
            String response = """
                    {
                        "result_code": "0000",
                        "result_msg": "성공",
                        "data": {
                            "transactionId": "TXN123456",
                            "status": "COMPLETED",
                            "transferredAmount": 100000
                        }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/bank/transfer"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = bankMessageClient.request("이체", Map.of(
                    "fromAccountNo", "110-234-567890",
                    "toAccountNo", "110-987-654321",
                    "amount", 100000
            ));

            assertNotNull(result);
            assertEquals("TXN123456", result.get("transactionId"));
            assertEquals("COMPLETED", result.get("status"));

            mockServer.verify();
        }

        @Test
        @DisplayName("계좌거래내역조회 - 정상 응답 시 거래 내역 목록을 반환한다")
        void getTransactionHistory_success() {
            String response = """
                    {
                        "result_code": "0000",
                        "result_msg": "성공",
                        "data": [
                            {"transactionId": "TR001", "description": "급여입금", "amount": 3500000},
                            {"transactionId": "TR002", "description": "카드대금", "amount": 450000}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/bank/accounts/110-234-567890/transactions?fromDate=20240101&toDate=20241231"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = bankMessageClient.request("계좌거래내역조회", Map.of(
                    "accountNo", "110-234-567890",
                    "fromDate", "20240101",
                    "toDate", "20241231"
            ));

            assertNotNull(result);
            assertTrue(result.containsKey("items"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(2, items.size());

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 계좌 조회 시 ExternalSystemException이 발생한다")
        void accountNotFound_throwsException() {
            String response = """
                    {
                        "result_code": "E001",
                        "result_msg": "계좌를 찾을 수 없습니다"
                    }
                    """;

            mockServer.expect(requestTo(containsString("/api/bank/accounts/999-999-999/transactions")))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    bankMessageClient.request("계좌거래내역조회", Map.of(
                            "accountNo", "999-999-999",
                            "fromDate", "20240101",
                            "toDate", "20241231"
                    ))
            );

            assertEquals("E001", exception.getErrorCode());
            assertEquals("계좌를 찾을 수 없습니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("이체한도 초과 시 ExternalSystemException이 발생한다")
        void transferLimitExceeded_throwsException() {
            String response = """
                    {
                        "result_code": "E002",
                        "result_msg": "이체한도를 초과하였습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/bank/transfer"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    bankMessageClient.request("이체", Map.of(
                            "fromAccountNo", "110-234-567890",
                            "toAccountNo", "110-987-654321",
                            "amount", 50000000
                    ))
            );

            assertEquals("E002", exception.getErrorCode());
        }

        @Test
        @DisplayName("잔액 부족 시 ExternalSystemException이 발생한다")
        void insufficientBalance_throwsException() {
            String response = """
                    {
                        "result_code": "E003",
                        "result_msg": "잔액이 부족합니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8081/api/bank/transfer"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    bankMessageClient.request("이체", Map.of(
                            "fromAccountNo", "110-234-567890",
                            "toAccountNo", "110-987-654321",
                            "amount", 8000000
                    ))
            );

            assertEquals("E003", exception.getErrorCode());
            assertEquals("잔액이 부족합니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("등록되지 않은 거래코드로 호출 시 IllegalArgumentException이 발생한다")
        void unknownTransactionCode_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankMessageClient.request("없는거래코드", Map.of())
            );
        }

        @Test
        @DisplayName("필수 경로변수 누락 시 IllegalArgumentException이 발생한다")
        void missingPathVariable_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    bankMessageClient.request("계좌거래내역조회", Map.of(
                            "fromDate", "20240101",
                            "toDate", "20241231"
                    ))
            );
        }

        @Test
        @DisplayName("외부 시스템 연결 실패 시 ExternalSystemException이 발생한다")
        void connectionFailure_throwsException() {
            mockServer.expect(requestTo("http://localhost:8081/api/bank/accounts"))
                    .andRespond(withServerError());

            assertThrows(Exception.class, () ->
                    bankMessageClient.request("계좌목록조회", Map.of())
            );
        }
    }
}
