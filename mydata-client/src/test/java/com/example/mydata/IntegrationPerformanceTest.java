package com.example.mydata;

import com.example.mydata.client.bank.BankMessageClient;
import com.example.mydata.client.card.CardMessageClient;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Performance Test - requires running servers:
 *   Banking Server (port 8081)
 *   Card Server    (port 8082)
 *
 * Run: mvn test -pl mydata-client -Dtest=IntegrationPerformanceTest
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
class IntegrationPerformanceTest {

    @Autowired
    private BankMessageClient bankMessageClient;

    @Autowired
    private CardMessageClient cardMessageClient;

    // ========== Warm-up ==========

    @Test
    @Order(0)
    @DisplayName("Warm-up")
    void warmUp() {
        try {
            bankMessageClient.request("계좌목록조회", Map.of());
            cardMessageClient.request("보유카드목록조회", Map.of());
        } catch (Exception e) {
            fail("Servers not running. Start banking-server(8081) and card-server(8082) first.\n" + e.getMessage());
        }
    }

    // ========== Banking ==========

    @Test
    @Order(1)
    @DisplayName("[Banking] GetAccountList - concurrent 100 requests / 20 threads")
    void banking_getAccountList() throws InterruptedException {
        runLoadTest("Banking - GetAccountList", 100, 20, () ->
                bankMessageClient.request("계좌목록조회", Map.of())
        );
    }

    @Test
    @Order(2)
    @DisplayName("[Banking] Transfer - concurrent 100 requests / 20 threads")
    void banking_transfer() throws InterruptedException {
        runLoadTest("Banking - Transfer", 100, 20, () ->
                bankMessageClient.request("이체", Map.of(
                        "fromAccountNo", "110-234-567890",
                        "toAccountNo", "110-987-654321",
                        "amount", 10000
                ))
        );
    }

    @Test
    @Order(3)
    @DisplayName("[Banking] GetTransactions - concurrent 100 requests / 20 threads")
    void banking_getTransactions() throws InterruptedException {
        runLoadTest("Banking - GetTransactions", 100, 20, () ->
                bankMessageClient.request("계좌거래내역조회", Map.of(
                        "accountNo", "110-234-567890",
                        "fromDate", "20240101",
                        "toDate", "20241231"
                ))
        );
    }

    // ========== Card ==========

    @Test
    @Order(4)
    @DisplayName("[Card] GetCardList - concurrent 100 requests / 20 threads")
    void card_getCardList() throws InterruptedException {
        runLoadTest("Card - GetCardList", 100, 20, () ->
                cardMessageClient.request("보유카드목록조회", Map.of())
        );
    }

    @Test
    @Order(5)
    @DisplayName("[Card] GetScheduledPayment - concurrent 100 requests / 20 threads")
    void card_getScheduledPayment() throws InterruptedException {
        runLoadTest("Card - GetScheduledPayment", 100, 20, () ->
                cardMessageClient.request("결제예정금액조회", Map.of(
                        "cardNo", "1234-5678-9012-3456"
                ))
        );
    }

    // ========== Mixed ==========

    @Test
    @Order(6)
    @DisplayName("[Mixed] Banking + Card - concurrent 200 requests / 30 threads")
    void mixed() throws InterruptedException {
        int totalRequests = 200;
        int threadCount = 30;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(totalRequests);

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalRequests; i++) {
            final int idx = i;
            executor.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                long t0 = System.nanoTime();
                try {
                    if (idx % 2 == 0) {
                        bankMessageClient.request("계좌목록조회", Map.of());
                    } else {
                        cardMessageClient.request("보유카드목록조회", Map.of());
                    }
                    latencies.add((System.nanoTime() - t0) / 1_000_000);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        long wallStart = System.currentTimeMillis();
        start.countDown(); // fire all at once
        done.await(60, TimeUnit.SECONDS);
        long wallTime = System.currentTimeMillis() - wallStart;

        executor.shutdown();
        printReport("Mixed (Banking+Card)", totalRequests, wallTime, latencies, errors);
        assertTrue(errors.isEmpty(), "Errors: " + errors.size());
    }

    // ========== High Load ==========

    @Test
    @Order(7)
    @DisplayName("[HighLoad] Banking - 500 requests / 50 threads")
    void highLoad_banking() throws InterruptedException {
        runLoadTest("HighLoad Banking", 500, 50, () ->
                bankMessageClient.request("계좌목록조회", Map.of())
        );
    }

    // ========== Helper ==========

    private void runLoadTest(String name, int totalRequests, int threadCount, Runnable task)
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(totalRequests);

        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

                long t0 = System.nanoTime();
                try {
                    task.run();
                    latencies.add((System.nanoTime() - t0) / 1_000_000);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        long wallStart = System.currentTimeMillis();
        start.countDown();
        done.await(60, TimeUnit.SECONDS);
        long wallTime = System.currentTimeMillis() - wallStart;

        executor.shutdown();
        printReport(name, totalRequests, wallTime, latencies, errors);
        assertTrue(errors.isEmpty(), "Errors: " + errors.size());
    }

    private void printReport(String name, int totalRequests, long wallTimeMs,
                              List<Long> latencies, List<String> errors) {
        long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();

        System.out.println();
        System.out.println("========================================");
        System.out.println("  " + name);
        System.out.println("========================================");
        System.out.println("  Total Requests : " + totalRequests);
        System.out.println("  Success        : " + latencies.size());
        System.out.println("  Failure        : " + errors.size());
        System.out.println("  Wall Time      : " + wallTimeMs + " ms");
        System.out.printf("  TPS            : %.2f req/s%n",
                (double) latencies.size() / wallTimeMs * 1000);

        if (sorted.length > 0) {
            System.out.printf("  Avg Latency    : %.2f ms%n", LongStream.of(sorted).average().orElse(0));
            System.out.println("  Min Latency    : " + sorted[0] + " ms");
            System.out.println("  Max Latency    : " + sorted[sorted.length - 1] + " ms");
            System.out.println("  P50            : " + pct(sorted, 50) + " ms");
            System.out.println("  P90            : " + pct(sorted, 90) + " ms");
            System.out.println("  P95            : " + pct(sorted, 95) + " ms");
            System.out.println("  P99            : " + pct(sorted, 99) + " ms");
        }
        System.out.println("========================================");
    }

    private long pct(long[] sorted, int p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, idx)];
    }
}
