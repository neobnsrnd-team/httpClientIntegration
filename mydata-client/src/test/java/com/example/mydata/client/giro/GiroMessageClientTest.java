package com.example.mydata.client.giro;

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

@DisplayName("GiroMessageClient 테스트")
class GiroMessageClientTest {

    private GiroMessageClient giroMessageClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GenericHttpClient httpClient = new GenericHttpClient(restClient);

        SystemProperties giroProperties = new SystemProperties();
        giroProperties.setBaseUrl("http://localhost:8084");
        giroProperties.setSuccessCodeField("rsp_cd");
        giroProperties.setSuccessCodeValue("000");
        giroProperties.setErrorMessageField("rsp_msg");
        giroProperties.setDataField("rsp_data");

        // 지로청구서목록조회
        MessageSpecProperties billListSpec = new MessageSpecProperties();
        billListSpec.setTransactionCode("지로청구서목록조회");
        billListSpec.setMethod("GET");
        billListSpec.setPath("/api/giro/bills");
        LinkedHashMap<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("cust_id", "custId");
        billListSpec.setQueryParams(queryParams);
        LinkedHashMap<String, String> billMapping = new LinkedHashMap<>();
        billMapping.put("bill_no", "billNumber");
        billMapping.put("bill_nm", "billName");
        billMapping.put("pay_amt", "paymentAmount");
        billMapping.put("due_dt", "dueDate");
        billMapping.put("pay_st", "paymentStatus");
        billMapping.put("org_nm", "organizationName");
        billListSpec.setResponseMapping(billMapping);

        // 지로납부
        MessageSpecProperties paymentSpec = new MessageSpecProperties();
        paymentSpec.setTransactionCode("지로납부");
        paymentSpec.setMethod("POST");
        paymentSpec.setPath("/api/giro/payment");
        paymentSpec.setBodyFields(Map.of(
                "bill_no", "billNo",
                "pay_amt", "amount"
        ));
        LinkedHashMap<String, String> paymentMapping = new LinkedHashMap<>();
        paymentMapping.put("pay_no", "paymentNumber");
        paymentMapping.put("bill_no", "billNumber");
        paymentMapping.put("pay_amt", "paymentAmount");
        paymentMapping.put("pay_dt", "paymentDate");
        paymentMapping.put("pay_st", "paymentStatus");
        paymentSpec.setResponseMapping(paymentMapping);

        giroProperties.setMessages(Map.of(
                "bill-list", billListSpec,
                "payment", paymentSpec
        ));

        giroMessageClient = new GiroMessageClient(httpClient, giroProperties, objectMapper);
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("지로청구서목록조회 - 정상 응답 시 매핑된 청구서 목록을 반환한다")
        void getBillList_success() {
            String response = """
                    {
                        "rsp_cd": "000",
                        "rsp_msg": "정상처리",
                        "rsp_data": [
                            {"bill_no": "BILL-2024-001", "bill_nm": "전기요금", "pay_amt": 50000, "due_dt": "20240430", "pay_st": "UNPAID", "org_nm": "한국전력공사"},
                            {"bill_no": "BILL-2024-002", "bill_nm": "수도요금", "pay_amt": 30000, "due_dt": "20240430", "pay_st": "UNPAID", "org_nm": "서울시상수도사업본부"},
                            {"bill_no": "BILL-2024-003", "bill_nm": "가스요금", "pay_amt": 45000, "due_dt": "20240515", "pay_st": "PAID", "org_nm": "서울도시가스"}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/bills?cust_id=C001"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = giroMessageClient.request("지로청구서목록조회", Map.of(
                    "custId", "C001"
            ));

            assertNotNull(result);
            assertTrue(result.containsKey("items"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(3, items.size());

            mockServer.verify();
        }

        @Test
        @DisplayName("지로납부 - 정상 납부 시 매핑된 결과를 반환한다")
        void payment_success() {
            String response = """
                    {
                        "rsp_cd": "000",
                        "rsp_msg": "정상처리",
                        "rsp_data": {
                            "pay_no": "PAY1234567890",
                            "bill_no": "BILL-2024-001",
                            "pay_amt": 50000,
                            "pay_dt": "20240315",
                            "pay_st": "COMPLETED"
                        }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/payment"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = giroMessageClient.request("지로납부", Map.of(
                    "billNo", "BILL-2024-001",
                    "amount", 50000
            ));

            assertNotNull(result);
            assertEquals("PAY1234567890", result.get("paymentNumber"));
            assertEquals("COMPLETED", result.get("paymentStatus"));

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("비즈니스 에러 케이스")
    class BusinessErrorCases {

        @Test
        @DisplayName("고객 미존재 시 ExternalSystemException이 발생한다 (GIRO001)")
        void customerNotFound_throwsException() {
            String response = """
                    {
                        "rsp_cd": "GIRO001",
                        "rsp_msg": "고객을 찾을 수 없습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/bills?cust_id=9999999"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로청구서목록조회", Map.of(
                            "custId", "9999999"
                    ))
            );

            assertEquals("GIRO001", exception.getErrorCode());
            assertEquals("고객을 찾을 수 없습니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("납부기한 만료 시 ExternalSystemException이 발생한다 (GIRO002)")
        void expiredBill_throwsException() {
            String response = """
                    {
                        "rsp_cd": "GIRO002",
                        "rsp_msg": "납부기한이 만료되었습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/payment"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로납부", Map.of(
                            "billNo", "BILL-EXPIRED-001",
                            "amount", 50000
                    ))
            );

            assertEquals("GIRO002", exception.getErrorCode());
            assertEquals("납부기한이 만료되었습니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("금액 오류 시 ExternalSystemException이 발생한다 (GIRO003)")
        void invalidAmount_throwsException() {
            String response = """
                    {
                        "rsp_cd": "GIRO003",
                        "rsp_msg": "납부 금액이 올바르지 않습니다"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/payment"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로납부", Map.of(
                            "billNo", "BILL-2024-001",
                            "amount", -1000
                    ))
            );

            assertEquals("GIRO003", exception.getErrorCode());
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
                    giroMessageClient.request("없는거래코드", Map.of())
            );
        }
    }

    @Nested
    @DisplayName("HTTP 에러 케이스")
    class HttpErrorCases {

        @Test
        @DisplayName("외부 시스템이 404 응답 시 NOT_FOUND ExternalSystemException이 발생한다")
        void notFound_throwsException() {
            mockServer.expect(requestTo(containsString("/api/giro/bills")))
                    .andRespond(withResourceNotFound());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로청구서목록조회", Map.of(
                            "custId", "C001"
                    ))
            );

            assertEquals("NOT_FOUND", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 리소스를 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 400 응답 시 BAD_REQUEST ExternalSystemException이 발생한다")
        void badRequest_throwsException() {
            mockServer.expect(requestTo("http://localhost:8084/api/giro/payment"))
                    .andRespond(withBadRequest());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로납부", Map.of(
                            "billNo", "BILL-2024-001",
                            "amount", 50000
                    ))
            );

            assertEquals("BAD_REQUEST", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 요청이 잘못되었습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 500 응답 시 SERVER_ERROR ExternalSystemException이 발생한다")
        void serverError_throwsException() {
            mockServer.expect(requestTo(containsString("/api/giro/bills")))
                    .andRespond(withServerError());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    giroMessageClient.request("지로청구서목록조회", Map.of(
                            "custId", "C001"
                    ))
            );

            assertEquals("SERVER_ERROR", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 서버 오류"));
        }
    }

    @Nested
    @DisplayName("응답 매핑 검증")
    class ResponseMappingCases {

        @Test
        @DisplayName("리스트 응답의 각 항목에 필드 매핑이 적용된다")
        void listItems_mappingApplied() {
            String response = """
                    {
                        "rsp_cd": "000",
                        "rsp_msg": "정상처리",
                        "rsp_data": [
                            {"bill_no": "BILL-001", "bill_nm": "전기요금", "pay_amt": 50000, "due_dt": "20240430", "pay_st": "UNPAID", "org_nm": "한국전력공사"}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/bills?cust_id=C001"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = giroMessageClient.request("지로청구서목록조회", Map.of(
                    "custId", "C001"
            ));

            List<?> items = (List<?>) result.get("items");
            assertEquals(1, items.size());

            @SuppressWarnings("unchecked")
            Map<String, Object> bill = (Map<String, Object>) items.get(0);
            assertEquals("BILL-001", bill.get("billNumber"));
            assertEquals("전기요금", bill.get("billName"));
            assertEquals(50000, bill.get("paymentAmount"));
            assertEquals("20240430", bill.get("dueDate"));
            assertEquals("UNPAID", bill.get("paymentStatus"));
            assertEquals("한국전력공사", bill.get("organizationName"));

            // 원래 약어 필드명은 존재하지 않아야 함
            assertFalse(bill.containsKey("bill_no"));
            assertFalse(bill.containsKey("bill_nm"));
            assertFalse(bill.containsKey("pay_amt"));

            mockServer.verify();
        }

        @Test
        @DisplayName("맵 응답의 최상위 키에 필드 매핑이 적용된다")
        void mapResponse_mappingApplied() {
            String response = """
                    {
                        "rsp_cd": "000",
                        "rsp_msg": "정상처리",
                        "rsp_data": {
                            "pay_no": "PAY123",
                            "bill_no": "BILL-001",
                            "pay_amt": 50000,
                            "pay_dt": "20240315",
                            "pay_st": "COMPLETED"
                        }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/payment"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = giroMessageClient.request("지로납부", Map.of(
                    "billNo", "BILL-001",
                    "amount", 50000
            ));

            assertEquals("PAY123", result.get("paymentNumber"));
            assertEquals("BILL-001", result.get("billNumber"));
            assertEquals(50000, result.get("paymentAmount"));
            assertEquals("20240315", result.get("paymentDate"));
            assertEquals("COMPLETED", result.get("paymentStatus"));

            // 원래 약어 필드명은 존재하지 않아야 함
            assertFalse(result.containsKey("pay_no"));
            assertFalse(result.containsKey("bill_no"));
            assertFalse(result.containsKey("pay_amt"));

            mockServer.verify();
        }

        @Test
        @DisplayName("매핑에 없는 필드는 원래 이름이 유지된다 (pass-through)")
        void unmappedFields_passThrough() {
            String response = """
                    {
                        "rsp_cd": "000",
                        "rsp_msg": "정상처리",
                        "rsp_data": [
                            {"bill_no": "BILL-001", "bill_nm": "전기요금", "pay_amt": 50000, "extra_field": "추가데이터"}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8084/api/giro/bills?cust_id=C001"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = giroMessageClient.request("지로청구서목록조회", Map.of(
                    "custId", "C001"
            ));

            List<?> items = (List<?>) result.get("items");
            @SuppressWarnings("unchecked")
            Map<String, Object> bill = (Map<String, Object>) items.get(0);

            // 매핑된 필드
            assertEquals("BILL-001", bill.get("billNumber"));
            assertEquals("전기요금", bill.get("billName"));
            assertEquals(50000, bill.get("paymentAmount"));

            // 매핑에 없는 필드 → 원래 이름 유지
            assertEquals("추가데이터", bill.get("extra_field"));
            assertFalse(bill.containsKey("bill_no"));

            mockServer.verify();
        }
    }
}
