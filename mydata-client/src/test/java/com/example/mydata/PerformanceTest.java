package com.example.mydata;

import com.example.mydata.client.bank.BankMessageClient;
import com.example.mydata.client.card.CardMessageClient;
import com.example.mydata.client.core.GenericHttpClient;
import com.example.mydata.client.core.MessageSpecProperties;
import com.example.mydata.client.core.SystemProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("성능 테스트")
class PerformanceTest {

    private BankMessageClient bankMessageClient;
    private CardMessageClient cardMessageClient;
    private MockRestServiceServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        GenericHttpClient httpClient = new GenericHttpClient(restClient);

        // Bank setup
        SystemProperties bankProps = new SystemProperties();
        bankProps.setBaseUrl("http://localhost:8081");
        bankProps.setSuccessCodeField("result_code");
        bankProps.setSuccessCodeValue("0000");
        bankProps.setErrorMessageField("result_msg");
        bankProps.setDataField("data");

        MessageSpecProperties accountListSpec = new MessageSpecProperties();
        accountListSpec.setTransactionCode("계좌목록조회");
        accountListSpec.setMethod("GET");
        accountListSpec.setPath("/api/bank/accounts");
        bankProps.setMessages(Map.of("account-list", accountListSpec));

        bankMessageClient = new BankMessageClient(httpClient, bankProps, objectMapper);

        // Card setup
        SystemProperties cardProps = new SystemProperties();
        cardProps.setBaseUrl("http://localhost:8082");
        cardProps.setSuccessCodeField("status");
        cardProps.setSuccessCodeValue("SUCCESS");
        cardProps.setErrorMessageField("message");
        cardProps.setDataField("payload");

        MessageSpecProperties cardListSpec = new MessageSpecProperties();
        cardListSpec.setTransactionCode("보유카드목록조회");
        cardListSpec.setMethod("GET");
        cardListSpec.setPath("/api/card/cards");
        cardProps.setMessages(Map.of("card-list", cardListSpec));

        cardMessageClient = new CardMessageClient(httpClient, cardProps, objectMapper);
    }

    @Test
    @DisplayName("Banking API 동시 요청 성능 측정")
    void bankingConcurrentRequests() throws InterruptedException {
        int totalRequests = 100;
        String response = """
                {
                    "result_code": "0000",
                    "result_msg": "성공",
                    "data": [
                        {"accountNo": "110-234-567890", "accountName": "급여계좌", "balance": 1500000}
                    ]
                }
                """;

        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo("http://localhost:8081/api/bank/accounts"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long reqStart = System.nanoTime();
                    Map<String, Object> result = bankMessageClient.request("계좌목록조회", Map.of());
                    long reqEnd = System.nanoTime();
                    latencies.add((reqEnd - reqStart) / 1_000_000); // ms
                    assertNotNull(result);
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printPerformanceReport("Banking - GetAccountList", totalRequests, totalTime, latencies, errors);

        assertTrue(errors.isEmpty(), "Errors: " + errors.size());
    }

    @Test
    @DisplayName("Card API 동시 요청 성능 측정")
    void cardConcurrentRequests() throws InterruptedException {
        int totalRequests = 100;
        String response = """
                {
                    "status": "SUCCESS",
                    "message": "처리완료",
                    "payload": [
                        {"cardNo": "1234-5678-9012-3456", "cardName": "신한 Deep Dream"}
                    ]
                }
                """;

        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo("http://localhost:8082/api/card/cards"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    long reqStart = System.nanoTime();
                    Map<String, Object> result = cardMessageClient.request("보유카드목록조회", Map.of());
                    long reqEnd = System.nanoTime();
                    latencies.add((reqEnd - reqStart) / 1_000_000);
                    assertNotNull(result);
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printPerformanceReport("Card - GetCardList", totalRequests, totalTime, latencies, errors);

        assertTrue(errors.isEmpty(), "Errors: " + errors.size());
    }

    @Test
    @DisplayName("Banking + Card 혼합 동시 요청 성능 측정")
    void mixedConcurrentRequests() throws InterruptedException {
        int totalRequests = 200;
        String bankResponse = """
                {"result_code": "0000", "result_msg": "성공", "data": [{"accountNo": "110-234-567890"}]}
                """;
        String cardResponse = """
                {"status": "SUCCESS", "message": "처리완료", "payload": [{"cardNo": "1234-5678-9012-3456"}]}
                """;

        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo("http://localhost:8081/api/bank/accounts"))
                .andRespond(withSuccess(bankResponse, MediaType.APPLICATION_JSON));
        mockServer.expect(ExpectedCount.manyTimes(),
                        requestTo("http://localhost:8082/api/card/cards"))
                .andRespond(withSuccess(cardResponse, MediaType.APPLICATION_JSON));

        ExecutorService executor = Executors.newFixedThreadPool(30);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    long reqStart = System.nanoTime();
                    if (idx % 2 == 0) {
                        bankMessageClient.request("계좌목록조회", Map.of());
                    } else {
                        cardMessageClient.request("보유카드목록조회", Map.of());
                    }
                    long reqEnd = System.nanoTime();
                    latencies.add((reqEnd - reqStart) / 1_000_000);
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printPerformanceReport("Mixed (Banking+Card)", totalRequests, totalTime, latencies, errors);

        assertTrue(errors.isEmpty(), "Errors: " + errors.size());
    }

    private void printPerformanceReport(String testName, int totalRequests, long totalTimeMs,
                                         List<Long> latencies, List<Exception> errors) {
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();

        System.out.println("\n========================================");
        System.out.println("  Performance Report: " + testName);
        System.out.println("========================================");
        System.out.println("  Total Requests : " + totalRequests);
        System.out.println("  Success        : " + latencies.size());
        System.out.println("  Failure        : " + errors.size());
        System.out.println("  Total Time     : " + totalTimeMs + " ms");
        System.out.printf("  TPS            : %.2f req/s%n",
                (double) latencies.size() / totalTimeMs * 1000);

        if (sorted.length > 0) {
            System.out.println("  Avg Latency    : " + LongStream.of(sorted).average().orElse(0) + " ms");
            System.out.println("  Min Latency    : " + sorted[0] + " ms");
            System.out.println("  Max Latency    : " + sorted[sorted.length - 1] + " ms");
            System.out.println("  P50            : " + percentile(sorted, 50) + " ms");
            System.out.println("  P90            : " + percentile(sorted, 90) + " ms");
            System.out.println("  P95            : " + percentile(sorted, 95) + " ms");
            System.out.println("  P99            : " + percentile(sorted, 99) + " ms");
        }
        System.out.println("========================================\n");
    }

    private long percentile(long[] sorted, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, index)];
    }
}
