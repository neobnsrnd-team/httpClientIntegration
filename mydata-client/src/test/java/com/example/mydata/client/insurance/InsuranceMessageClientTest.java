package com.example.mydata.client.insurance;

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

@DisplayName("InsuranceMessageClient 테스트")
class InsuranceMessageClientTest {

    private InsuranceMessageClient insuranceMessageClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GenericHttpClient httpClient = new GenericHttpClient(restClient);

        SystemProperties insuranceProperties = new SystemProperties();
        insuranceProperties.setBaseUrl("http://localhost:8083");
        insuranceProperties.setSuccessCodeField("code");
        insuranceProperties.setSuccessCodeValue("00");
        insuranceProperties.setErrorMessageField("msg");
        insuranceProperties.setDataField("result");

        // 보험가입내역조회
        MessageSpecProperties policyListSpec = new MessageSpecProperties();
        policyListSpec.setTransactionCode("보험가입내역조회");
        policyListSpec.setMethod("GET");
        policyListSpec.setPath("/api/insurance/policies");
        LinkedHashMap<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("customerId", "customerId");
        policyListSpec.setQueryParams(queryParams);

        // 보험료납부
        MessageSpecProperties premiumPaymentSpec = new MessageSpecProperties();
        premiumPaymentSpec.setTransactionCode("보험료납부");
        premiumPaymentSpec.setMethod("POST");
        premiumPaymentSpec.setPath("/api/insurance/premium-payment");
        premiumPaymentSpec.setBodyFields(Map.of(
                "policyNo", "policyNo",
                "amount", "amount"
        ));

        insuranceProperties.setMessages(Map.of(
                "policy-list", policyListSpec,
                "premium-payment", premiumPaymentSpec
        ));

        insuranceMessageClient = new InsuranceMessageClient(httpClient, insuranceProperties, objectMapper);
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("보험가입내역조회 - 정상 응답 시 보험 목록을 반환한다")
        void getPolicyList_success() {
            String response = """
                    {
                        "code": "00",
                        "msg": "정상처리",
                        "result": [
                            {"policyNo": "POL-2024-001", "policyName": "무배당 건강보험", "premiumAmount": 150000},
                            {"policyNo": "POL-2024-002", "policyName": "운전자보험", "premiumAmount": 80000}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8083/api/insurance/policies?customerId=C001"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = insuranceMessageClient.request("보험가입내역조회", Map.of(
                    "customerId", "C001"
            ));

            assertNotNull(result);
            assertTrue(result.containsKey("items"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(2, items.size());

            mockServer.verify();
        }

        @Test
        @DisplayName("보험료납부 - 정상 납부 시 결과를 반환한다")
        void payPremium_success() {
            String response = """
                    {
                        "code": "00",
                        "msg": "정상처리",
                        "result": {
                            "paymentId": "PAY123456",
                            "policyNo": "POL-2024-001",
                            "paidAmount": 150000,
                            "paymentDate": "20240315",
                            "status": "COMPLETED"
                        }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8083/api/insurance/premium-payment"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = insuranceMessageClient.request("보험료납부", Map.of(
                    "policyNo", "POL-2024-001",
                    "amount", 150000
            ));

            assertNotNull(result);
            assertEquals("PAY123456", result.get("paymentId"));
            assertEquals("COMPLETED", result.get("status"));

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("비즈니스 에러 케이스")
    class BusinessErrorCases {

        @Test
        @DisplayName("고객 미존재 시 ExternalSystemException이 발생한다 (INS001)")
        void customerNotFound_throwsException() {
            String response = """
                    {
                        "code": "INS001",
                        "msg": "고객을 찾을 수 없습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8083/api/insurance/policies?customerId=9999999"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험가입내역조회", Map.of(
                            "customerId", "9999999"
                    ))
            );

            assertEquals("INS001", exception.getErrorCode());
            assertEquals("고객을 찾을 수 없습니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("만료된 보험 납부 시 ExternalSystemException이 발생한다 (INS002)")
        void expiredPolicy_throwsException() {
            String response = """
                    {
                        "code": "INS002",
                        "msg": "만료된 보험입니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8083/api/insurance/premium-payment"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험료납부", Map.of(
                            "policyNo", "POL-EXPIRED-001",
                            "amount", 150000
                    ))
            );

            assertEquals("INS002", exception.getErrorCode());
            assertEquals("만료된 보험입니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("금액 오류 시 ExternalSystemException이 발생한다 (INS003)")
        void invalidAmount_throwsException() {
            String response = """
                    {
                        "code": "INS003",
                        "msg": "납부 금액이 올바르지 않습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8083/api/insurance/premium-payment"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험료납부", Map.of(
                            "policyNo", "POL-2024-001",
                            "amount", -1000
                    ))
            );

            assertEquals("INS003", exception.getErrorCode());
            assertEquals("납부 금액이 올바르지 않습니다", exception.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("기타 에러 케이스")
    class OtherErrorCases {

        @Test
        @DisplayName("등록되지 않은 거래코드로 호출 시 IllegalArgumentException이 발생한다")
        void unknownTransactionCode_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    insuranceMessageClient.request("없는거래코드", Map.of())
            );
        }
    }

    @Nested
    @DisplayName("HTTP 에러 케이스")
    class HttpErrorCases {

        @Test
        @DisplayName("외부 시스템이 404 응답 시 NOT_FOUND ExternalSystemException이 발생한다")
        void notFound_throwsException() {
            mockServer.expect(requestTo(containsString("/api/insurance/policies")))
                    .andRespond(withResourceNotFound());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험가입내역조회", Map.of(
                            "customerId", "C001"
                    ))
            );

            assertEquals("NOT_FOUND", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 리소스를 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 400 응답 시 BAD_REQUEST ExternalSystemException이 발생한다")
        void badRequest_throwsException() {
            mockServer.expect(requestTo("http://localhost:8083/api/insurance/premium-payment"))
                    .andRespond(withBadRequest());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험료납부", Map.of(
                            "policyNo", "POL-2024-001",
                            "amount", 150000
                    ))
            );

            assertEquals("BAD_REQUEST", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 요청이 잘못되었습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 500 응답 시 SERVER_ERROR ExternalSystemException이 발생한다")
        void serverError_throwsException() {
            mockServer.expect(requestTo(containsString("/api/insurance/policies")))
                    .andRespond(withServerError());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    insuranceMessageClient.request("보험가입내역조회", Map.of(
                            "customerId", "C001"
                    ))
            );

            assertEquals("SERVER_ERROR", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 서버 오류"));
        }
    }
}
