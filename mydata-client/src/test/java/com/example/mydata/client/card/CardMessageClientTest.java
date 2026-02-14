package com.example.mydata.client.card;

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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("CardMessageClient 테스트")
class CardMessageClientTest {

    private CardMessageClient cardMessageClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GenericHttpClient httpClient = new GenericHttpClient(restClient);

        SystemProperties cardProperties = new SystemProperties();
        cardProperties.setBaseUrl("http://localhost:8082");
        cardProperties.setSuccessCodeField("status");
        cardProperties.setSuccessCodeValue("SUCCESS");
        cardProperties.setErrorMessageField("message");
        cardProperties.setDataField("payload");

        // 보유카드목록조회
        MessageSpecProperties cardListSpec = new MessageSpecProperties();
        cardListSpec.setTransactionCode("보유카드목록조회");
        cardListSpec.setMethod("GET");
        cardListSpec.setPath("/api/card/cards");

        // 결제예정금액조회
        MessageSpecProperties paymentSpec = new MessageSpecProperties();
        paymentSpec.setTransactionCode("결제예정금액조회");
        paymentSpec.setMethod("GET");
        paymentSpec.setPath("/api/card/cards/{cardNo}/scheduled-payments");
        paymentSpec.setPathVariables(List.of("cardNo"));

        cardProperties.setMessages(Map.of(
                "card-list", cardListSpec,
                "scheduled-payment", paymentSpec
        ));

        cardMessageClient = new CardMessageClient(httpClient, cardProperties, objectMapper);
    }

    @Nested
    @DisplayName("정상 케이스")
    class SuccessCases {

        @Test
        @DisplayName("보유카드목록조회 - 정상 응답 시 카드 목록을 반환한다")
        void getCardList_success() {
            String response = """
                    {
                        "status": "SUCCESS",
                        "message": "처리완료",
                        "payload": [
                            {"cardNo": "1234-5678-9012-3456", "cardName": "신한 Deep Dream", "cardType": "CREDIT"},
                            {"cardNo": "9876-5432-1098-7654", "cardName": "삼성 taptap O", "cardType": "CREDIT"}
                        ]
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8082/api/card/cards"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = cardMessageClient.request("보유카드목록조회", Map.of());

            assertNotNull(result);
            assertTrue(result.containsKey("items"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(2, items.size());

            mockServer.verify();
        }

        @Test
        @DisplayName("결제예정금액조회 - 정상 응답 시 결제예정 정보를 반환한다")
        void getScheduledPayment_success() {
            String response = """
                    {
                        "status": "SUCCESS",
                        "message": "처리완료",
                        "payload": {
                            "cardNo": "1234-5678-9012-3456",
                            "paymentDate": "20240415",
                            "totalAmount": 650000,
                            "details": [
                                {"merchantName": "쿠팡", "amount": 150000},
                                {"merchantName": "하이마트", "amount": 500000}
                            ]
                        }
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8082/api/card/cards/1234-5678-9012-3456/scheduled-payments"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            Map<String, Object> result = cardMessageClient.request("결제예정금액조회", Map.of(
                    "cardNo", "1234-5678-9012-3456"
            ));

            assertNotNull(result);
            assertEquals("1234-5678-9012-3456", result.get("cardNo"));
            assertEquals(650000, ((Number) result.get("totalAmount")).intValue());

            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 카드 조회 시 ExternalSystemException이 발생한다")
        void cardNotFound_throwsException() {
            String response = """
                    {
                        "status": "FAIL",
                        "message": "카드를 찾을 수 없습니다",
                        "error_code": "CARD_NOT_FOUND"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8082/api/card/cards/9999-9999-9999-9999/scheduled-payments"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    cardMessageClient.request("결제예정금액조회", Map.of(
                            "cardNo", "9999-9999-9999-9999"
                    ))
            );

            assertEquals("FAIL", exception.getErrorCode());
            assertEquals("카드를 찾을 수 없습니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("해지된 카드 조회 시 ExternalSystemException이 발생한다")
        void cardCancelled_throwsException() {
            String response = """
                    {
                        "status": "FAIL",
                        "message": "해지된 카드입니다",
                        "error_code": "CARD_CANCELLED"
                    }
                    """;

            mockServer.expect(requestTo("http://localhost:8082/api/card/cards/0000-0000-0000-0000/scheduled-payments"))
                    .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    cardMessageClient.request("결제예정금액조회", Map.of(
                            "cardNo", "0000-0000-0000-0000"
                    ))
            );

            assertEquals("FAIL", exception.getErrorCode());
            assertEquals("해지된 카드입니다", exception.getErrorMessage());
        }

        @Test
        @DisplayName("등록되지 않은 거래코드로 호출 시 IllegalArgumentException이 발생한다")
        void unknownTransactionCode_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    cardMessageClient.request("없는거래코드", Map.of())
            );
        }

        @Test
        @DisplayName("필수 경로변수(cardNo) 누락 시 IllegalArgumentException이 발생한다")
        void missingPathVariable_throwsException() {
            assertThrows(IllegalArgumentException.class, () ->
                    cardMessageClient.request("결제예정금액조회", Map.of())
            );
        }

        @Test
        @DisplayName("외부 시스템이 404 응답 시 NOT_FOUND ExternalSystemException이 발생한다")
        void notFound_throwsException() {
            mockServer.expect(requestTo("http://localhost:8082/api/card/cards"))
                    .andRespond(withResourceNotFound());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    cardMessageClient.request("보유카드목록조회", Map.of())
            );

            assertEquals("NOT_FOUND", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 리소스를 찾을 수 없습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 400 응답 시 BAD_REQUEST ExternalSystemException이 발생한다")
        void badRequest_throwsException() {
            mockServer.expect(requestTo("http://localhost:8082/api/card/cards/1234-5678-9012-3456/scheduled-payments"))
                    .andRespond(withBadRequest());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    cardMessageClient.request("결제예정금액조회", Map.of(
                            "cardNo", "1234-5678-9012-3456"
                    ))
            );

            assertEquals("BAD_REQUEST", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 요청이 잘못되었습니다"));
        }

        @Test
        @DisplayName("외부 시스템이 500 응답 시 SERVER_ERROR ExternalSystemException이 발생한다")
        void serverError_throwsException() {
            mockServer.expect(requestTo("http://localhost:8082/api/card/cards"))
                    .andRespond(withServerError());

            ExternalSystemException exception = assertThrows(ExternalSystemException.class, () ->
                    cardMessageClient.request("보유카드목록조회", Map.of())
            );

            assertEquals("SERVER_ERROR", exception.getErrorCode());
            assertTrue(exception.getErrorMessage().contains("외부 시스템 서버 오류"));
        }
    }
}
