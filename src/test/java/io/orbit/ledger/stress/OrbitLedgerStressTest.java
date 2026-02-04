package io.orbit.ledger.stress;

import io.orbit.ledger.api.OrbitLedger;
import io.orbit.ledger.enums.PerformanceMode;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.model.LedgerEvent;
import io.orbit.ledger.model.OrbitRelease;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive stress test for Orbit Ledger.
 * <p>
 * Validates throughput, memory usage, and calculation correctness
 * under high load conditions (100k-500k orders).
 * </p>
 *
 * @since 1.0.0
 */
public class OrbitLedgerStressTest {

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════

    private static final int ACCOUNT_COUNT = 100_000;
    private static final int BUFFER_SIZE = 524288;
    private static final int THREAD_COUNT = 6;
    private static final int RELEASE_THRESHOLD = 1000;

    // ═══════════════════════════════════════════════════════════════
    // TEST METRICS
    // ═══════════════════════════════════════════════════════════════

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

    // Results tracking
    private final Map<String, Long> expectedBalances = new ConcurrentHashMap<>();
    private final Map<String, Long> actualBalances = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> eventSequences = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsReleased = new AtomicLong(0);
    private final AtomicLong totalReleaseCalls = new AtomicLong(0);

    // Production-style completion detection (no Thread.sleep)
    private volatile int expectedOrderCount = 0;
    private volatile java.util.concurrent.CountDownLatch completionLatch;

    // Memory metrics
    private long heapBefore;
    private long heapAfter;
    private long peakHeap;

    // ═══════════════════════════════════════════════════════════════
    // PARAMETERIZED STRESS TESTS (1M, 2M, 3M × COUNT, TIME, HYBRID)
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Stress test: {0} orders with COUNT")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_COUNT_STANDARD(int orderCount) throws InterruptedException {
        System.out.println("\n🔵 RELEASE TYPE: COUNT (threshold-based)");
        runStressTestWithReleaseType(orderCount, ReleaseType.COUNT, null, PerformanceMode.STANDARD);
    }
    @ParameterizedTest(name = "Stress test: {0} orders with COUNT")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_COUNT_MAXIMUM(int orderCount) throws InterruptedException {
        System.out.println("\n🔵 RELEASE TYPE: COUNT (threshold-based)");
        runStressTestWithReleaseType(orderCount, ReleaseType.COUNT, null, PerformanceMode.MAXIMUM);
    }

    @ParameterizedTest(name = "Stress test: {0} orders with TIME")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_TIME_STANDARD(int orderCount) throws InterruptedException {
        System.out.println("\n🟢 RELEASE TYPE: TIME (interval-based)");
        runStressTestWithReleaseType(orderCount, ReleaseType.TIME, Duration.ofMillis(50), PerformanceMode.STANDARD);
    }

    @ParameterizedTest(name = "Stress test: {0} orders with TIME")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_TIME_MAXIMUM(int orderCount) throws InterruptedException {
        System.out.println("\n🟢 RELEASE TYPE: TIME (interval-based)");
        runStressTestWithReleaseType(orderCount, ReleaseType.TIME, Duration.ofMillis(50), PerformanceMode.MAXIMUM);
    }

    @ParameterizedTest(name = "Stress test: {0} orders with HYBRID")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_HYBRID_STANDARD(int orderCount) throws InterruptedException {
        System.out.println("\n🟠 RELEASE TYPE: HYBRID (count OR time)");
        runStressTestWithReleaseType(orderCount, ReleaseType.HYBRID, Duration.ofMillis(50), PerformanceMode.STANDARD);
    }
    @ParameterizedTest(name = "Stress test: {0} orders with HYBRID")
    @ValueSource(ints = { 1_000_000, 2_000_000, 3_000_000 })
    void stressTest_HYBRID_MAXIMUM(int orderCount) throws InterruptedException {
        System.out.println("\n🟠 RELEASE TYPE: HYBRID (count OR time)");
        runStressTestWithReleaseType(orderCount, ReleaseType.HYBRID, Duration.ofMillis(50), PerformanceMode.MAXIMUM);
    }


    // ═══════════════════════════════════════════════════════════════
    // CSV GENERATION UTILITIES
    // ═══════════════════════════════════════════════════════════════
//
//    /**
//     * Generate 1M orders CSV + expected balances CSV.
//     */
//    @Test
//    void generateCsvFile_1M() throws Exception {
//        generateOrdersAndExpectedBalances(1_000_000, "1m");
//    }
//
//    /**
//     * Generate 2M orders CSV + expected balances CSV.
//     */
//    @Test
//    void generateCsvFile_2M() throws Exception {
//        generateOrdersAndExpectedBalances(2_000_000, "2m");
//    }
//
//    /**
//     * Generate 3M orders CSV + expected balances CSV.
//     */
//    @Test
//    void generateCsvFile_3M() throws Exception {
//        generateOrdersAndExpectedBalances(3_000_000, "3m");
//    }

    /**
     * Generates both orders CSV and expected balances CSV.
     * 
     * @param orderCount number of orders
     * @param suffix     file suffix (e.g., "1m", "2m", "3m")
     */
    private void generateOrdersAndExpectedBalances(int orderCount, String suffix) throws Exception {
        Path ordersPath = Paths.get("src/test/resources/stress_test_orders_" + suffix + ".csv");
        Path expectedPath = Paths.get("src/test/resources/expected_balances_" + suffix + ".csv");

        System.out.println("\n📄 Generating " + numberFormat.format(orderCount) + " orders...");

        // Generate orders CSV
        generateSampleCsv(ordersPath, orderCount);

        // Calculate expected balances from generated orders (same seed = same orders)
        List<Order> orders = generateOrders(orderCount);
        Map<String, Long> balances = new java.util.TreeMap<>(); // Sorted by account ID

        for (Order order : orders) {
            balances.compute(order.accountId, (key, balance) -> {
                long current = balance != null ? balance : 0;
                return order.isCredit ? current + order.amount : current - order.amount;
            });
        }

        // Write expected balances CSV
        try (var writer = Files.newBufferedWriter(expectedPath)) {
            writer.write("account_id,expected_balance,order_count,total_credits,total_debits\n");

            // Calculate detailed stats per account
            Map<String, long[]> stats = new java.util.TreeMap<>();
            for (Order order : orders) {
                stats.computeIfAbsent(order.accountId, k -> new long[3]); // [count, credits, debits]
                long[] s = stats.get(order.accountId);
                s[0]++; // count
                if (order.isCredit) {
                    s[1] += order.amount; // total credits
                } else {
                    s[2] += order.amount; // total debits
                }
            }

            for (Map.Entry<String, Long> entry : balances.entrySet()) {
                long[] s = stats.get(entry.getKey());
                writer.write(String.format("%s,%d,%d,%d,%d%n",
                        entry.getKey(),
                        entry.getValue(),
                        s[0], // order count
                        s[1], // total credits
                        s[2])); // total debits
            }
        }

        System.out.println("✓ Orders CSV: " + ordersPath.toAbsolutePath());
        System.out.println("✓ Expected balances CSV: " + expectedPath.toAbsolutePath());
        System.out.println("  Accounts: " + numberFormat.format(balances.size()));

        assertTrue(Files.exists(ordersPath), "Orders CSV should be created");
        assertTrue(Files.exists(expectedPath), "Expected balances CSV should be created");
    }

    /**
     * Runs stress test with orders loaded from CSV file.
     * 
     * @param csvPath path to CSV file
     */
    public void runStressTestFromCsv(Path csvPath) throws Exception {
        resetState();

        List<Order> orders = loadOrdersFromCsv(csvPath);

        System.out.println("\n" + "═".repeat(66));
        System.out.println("  ORBIT LEDGER CSV STRESS TEST - " + numberFormat.format(orders.size()) + " ORDERS");
        System.out.println("  Source: " + csvPath.toAbsolutePath());
        System.out.println("═".repeat(66));

        // Calculate expected balances from loaded orders
        calculateExpectedBalances(orders);

        // Capture initial memory
        System.gc();
        Thread.sleep(100);
        heapBefore = getHeapUsed();
        peakHeap = heapBefore;

        // Production-style: Use latch to wait for completion
        expectedOrderCount = orders.size();
        completionLatch = new java.util.concurrent.CountDownLatch(1);

        // Build and start ledger
        OrbitLedger ledger = OrbitLedger.builder()
                .bufferSize(BUFFER_SIZE)
                .threadCount(THREAD_COUNT)
                .releaseThreshold(RELEASE_THRESHOLD)
                .releaseType(ReleaseType.COUNT)
                .defaultBalance(0)
                .onRelease(this::handleRelease)
                .build();

        ledger.start();

        // Execute stress test (production-style)
        long startTime = System.nanoTime();

        for (Order order : orders) {
            if (order.isCredit) {
                ledger.credit(order.accountId, order.amount);
            } else {
                ledger.debit(order.accountId, order.amount);
            }
        }

        ledger.releaseAll();

        // Wait for all events to be processed (production-style)
        completionLatch.await(60, TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        heapAfter = getHeapUsed();
        peakHeap = Math.max(peakHeap, heapAfter);

        ledger.shutdown();

        // Validate and report
        StressTestResult result = validateAndReport(orders.size(), durationMs, ReleaseType.COUNT, PerformanceMode.STANDARD);

        assertEquals(0, result.balanceMismatches, "Balance mismatches detected");
        assertEquals(0, result.sequenceErrors, "Sequence errors detected");
    }

    /**
     * Loads orders from a CSV file.
     * <p>
     * Expected CSV format (with or without header):
     * account_id,type,amount
     * ACCOUNT-1,CREDIT,1000
     * ACCOUNT-2,DEBIT,500
     * </p>
     */
    private List<Order> loadOrdersFromCsv(Path csvPath) throws IOException {
        List<Order> orders = new ArrayList<>();
        int ordinal = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                // Skip header row if present
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().contains("account") ||
                            line.toLowerCase().contains("type")) {
                        continue;
                    }
                }

                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.err.println("⚠ Invalid CSV line: " + line);
                    continue;
                }

                String accountId = parts[0].trim();
                String type = parts[1].trim().toUpperCase();
                long amount = Long.parseLong(parts[2].trim());
                boolean isCredit = type.equals("CREDIT") || type.equals("C");

                orders.add(new Order(ordinal++, accountId, isCredit, amount));
            }
        }

        System.out.println("  Loaded " + numberFormat.format(orders.size()) + " orders from CSV");
        return orders;
    }

    /**
     * Generates a sample CSV file for testing with randomized order sequence.
     */
    private void generateSampleCsv(Path csvPath, int orderCount) throws IOException {
        Files.createDirectories(csvPath.getParent());
        System.out.println("  Generating " + numberFormat.format(orderCount) + " randomized orders...");

        Random random = new Random(42);
        try (var writer = Files.newBufferedWriter(csvPath)) {
            writer.write("account_id,type,amount\n");

            for (int i = 0; i < orderCount; i++) {
                // ACC_0001 to ACC_1000 format
                String accountId = String.format("ACC_%06d", random.nextInt(ACCOUNT_COUNT) + 1);
                String type = random.nextBoolean() ? "CREDIT" : "DEBIT";
                long amount = 100 + random.nextInt(10_000);
                writer.write(accountId + "," + type + "," + amount + "\n");

                if (i > 0 && i % 100_000 == 0) {
                    System.out.printf("    %s orders written...%n", numberFormat.format(i));
                }
            }
        }

        System.out.println("  Generated sample CSV: " + csvPath.toAbsolutePath());
    }

    // ═══════════════════════════════════════════════════════════════
    // MAIN STRESS TEST ENGINE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Runs the complete stress test with specified order count and release type.
     * 
     * Expected balances are calculated BEFORE the stress test runs,
     * then compared against actual balances after all releases complete.
     *
     * @param orderCount  number of orders to process
     * @param releaseType COUNT, TIME, or HYBRID
     * @param interval    release interval for TIME/HYBRID modes (null for COUNT)
     */
    public void runStressTestWithReleaseType(int orderCount, ReleaseType releaseType,
            java.time.Duration interval, PerformanceMode performanceMode) throws InterruptedException {
        // Reset state
        resetState();

        System.out.println("\n" + "═".repeat(70));
        System.out.println("  ORBIT LEDGER STRESS TEST");
        System.out.println("  Orders: " + numberFormat.format(orderCount) +
                " | ReleaseType: " + releaseType +
                (interval != null ? " | Interval: " + interval.toMillis() + "ms" : ""));
        System.out.println("═".repeat(70));

        // Pre-generate orders with random account distribution
        List<Order> orders = generateOrders(orderCount);

        // ══════════════════════════════════════════════════════════════
        // CALCULATE EXPECTED BALANCES (BEFORE stress test)
        // This serves as the ground truth for correctness validation
        // ══════════════════════════════════════════════════════════════
        System.out.println("\n📊 Calculating expected balances...");
        calculateExpectedBalances(orders);
        System.out.println("   Expected balances calculated for " +
                numberFormat.format(expectedBalances.size()) + " accounts");

        // Capture initial memory (BEFORE timing)
        System.gc();
        Thread.sleep(100);
        heapBefore = getHeapUsed();
        peakHeap = heapBefore;

        // Build ledger with specified release type
        var builder = OrbitLedger.builder()
                .bufferSize(BUFFER_SIZE)
                .threadCount(THREAD_COUNT)
                .releaseThreshold(RELEASE_THRESHOLD)
                .releaseType(releaseType)
                .performanceMode(performanceMode)
                .defaultBalance(0)
                .onRelease(this::handleRelease);

        // Add interval for TIME and HYBRID modes
        if (interval != null && (releaseType == ReleaseType.TIME || releaseType == ReleaseType.HYBRID)) {
            builder.releaseInterval(interval);
        }

        // Production-style: Use latch to wait for completion (no Thread.sleep)
        expectedOrderCount = orderCount;
        completionLatch = new java.util.concurrent.CountDownLatch(1);

        OrbitLedger ledger = builder.build();
        ledger.start();

        // ══════════════════════════════════════════════════════════════
        // PERFORMANCE TIMING - PRODUCTION STYLE
        // Same as real user: send orders, releaseAll, wait for callback
        // ══════════════════════════════════════════════════════════════
        System.out.println("\n🚀 Processing " + numberFormat.format(orderCount) + " orders...");

        // ⏱ START TIMING - Same as production usage
        long startTime = System.nanoTime();

        for (Order order : orders) {
            if (order.isCredit) {
                ledger.credit(order.accountId, order.amount);
            } else {
                ledger.debit(order.accountId, order.amount);
            }
        }

        // Release all remaining events (same as production)
        ledger.releaseAll();

        // Wait for all events to be processed (production-style, no arbitrary sleep)
        completionLatch.await(60, TimeUnit.SECONDS);

        // ⏱ END TIMING
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Capture final memory (AFTER timing)
        heapAfter = getHeapUsed();
        peakHeap = Math.max(peakHeap, heapAfter);

        // Shutdown
        ledger.shutdown();

        // ══════════════════════════════════════════════════════════════
        // VALIDATION (AFTER timing) - Compare expected vs actual balances
        // ══════════════════════════════════════════════════════════════
        StressTestResult result = validateAndReport(orderCount, durationMs, releaseType, performanceMode);

        // Assertions
        assertEquals(0, result.balanceMismatches, "Balance mismatches detected");
        assertEquals(0, result.sequenceErrors, "Sequence errors detected");
        assertTrue(result.throughputOpsPerSec > 10_000,
                "Throughput too low: " + result.throughputOpsPerSec + " ops/sec");
    }

    /**
     * Backward compatible method - defaults to COUNT release type.
     */
    public void runStressTest(int orderCount) throws InterruptedException {
        runStressTestWithReleaseType(orderCount, ReleaseType.COUNT, null, PerformanceMode.STANDARD);
    }

    // ═══════════════════════════════════════════════════════════════
    // ORDER GENERATION
    // ═══════════════════════════════════════════════════════════════

    private List<Order> generateOrders(int count) {
        List<Order> orders = new ArrayList<>(count);
        Random random = new Random(42); // Fixed seed for reproducibility

        for (int i = 0; i < count; i++) {
            // ACC_0001 to ACC_1000 format - randomized order
            String accountId = String.format("ACC_%06d", random.nextInt(ACCOUNT_COUNT) + 1);
            boolean isCredit = random.nextBoolean();
            long amount = 100 + random.nextInt(10_000); // 100 to 10,099
            orders.add(new Order(i, accountId, isCredit, amount));
        }

        return orders;
    }

    private void calculateExpectedBalances(List<Order> orders) {
        for (Order order : orders) {
            expectedBalances.compute(order.accountId, (key, balance) -> {
                long current = balance != null ? balance : 0;
                return order.isCredit ? current + order.amount : current - order.amount;
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // RELEASE HANDLER
    // ═══════════════════════════════════════════════════════════════

    private void handleRelease(OrbitRelease release) {
        totalReleaseCalls.incrementAndGet();
        long totalReleased = totalEventsReleased.addAndGet(release.eventCount());

        // Track actual balance
        if (release.runningBalance() != null) {
            actualBalances.put(release.key(), release.runningBalance());
        }

        // Track event sequences for ordering validation
        List<Long> sequences = eventSequences.computeIfAbsent(
                release.key(), k -> Collections.synchronizedList(new ArrayList<>()));

        for (LedgerEvent event : release.events()) {
            sequences.add(event.sequence());
        }

        // Production-style: signal completion when all events processed
        if (completionLatch != null && totalReleased >= expectedOrderCount) {
            completionLatch.countDown();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION & REPORTING
    // ═══════════════════════════════════════════════════════════════

    private StressTestResult validateAndReport(int orderCount, long durationMs, ReleaseType releaseType, PerformanceMode performanceMode) {
        int balanceMismatches = 0;
        int sequenceErrors = 0;

        System.out.println("\n🔍 Validating correctness (expected vs actual)...");

        // Validate balances
        for (Map.Entry<String, Long> entry : expectedBalances.entrySet()) {
            Long actual = actualBalances.get(entry.getKey());
            if (actual == null || !actual.equals(entry.getValue())) {
                balanceMismatches++;
                if (balanceMismatches <= 5) {
                    System.out.printf("  ⚠ Balance mismatch for %s: expected=%d, actual=%s%n",
                            entry.getKey(), entry.getValue(), actual);
                }
            }
        }

        // Validate sequence ordering (monotonically increasing per account)
        for (Map.Entry<String, List<Long>> entry : eventSequences.entrySet()) {
            List<Long> sequences = entry.getValue();
            for (int i = 1; i < sequences.size(); i++) {
                if (sequences.get(i) <= sequences.get(i - 1)) {
                    sequenceErrors++;
                    if (sequenceErrors <= 5) {
                        System.out.printf("  ⚠ Sequence error for %s at index %d: %d <= %d%n",
                                entry.getKey(), i, sequences.get(i), sequences.get(i - 1));
                    }
                }
            }
        }

        // Calculate metrics
        double throughput = durationMs > 0 ? (orderCount * 1000.0) / durationMs : 0;
        long memoryDelta = heapAfter - heapBefore;

        // Print report
        printReport(orderCount, durationMs, throughput, balanceMismatches, sequenceErrors,
                memoryDelta, releaseType, performanceMode);

        return new StressTestResult(
                orderCount, durationMs, throughput,
                heapBefore, heapAfter, peakHeap,
                balanceMismatches, sequenceErrors);
    }

    private void printReport(int orderCount, long durationMs, double throughput,
            int balanceMismatches, int sequenceErrors, long memoryDelta, ReleaseType releaseType, PerformanceMode performanceMode) {

        String status = (balanceMismatches == 0 && sequenceErrors == 0) ? "✓ ALL PASSED" : "✗ FAILURES DETECTED";

        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║              ORBIT LEDGER STRESS TEST REPORT                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ CONFIGURATION                                                    ║");
        System.out.printf("║   Release Type:        %-40s   ║%n", releaseType);
        System.out.printf("║   Performance Mode:        %-40s   ║%n", performanceMode);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ THROUGHPUT                                                       ║");
        System.out.printf("║   Orders Processed:    %-,15d                           ║%n", orderCount);
        System.out.printf("║   Execution Time:      %-,15d ms                        ║%n", durationMs);
        System.out.printf("║   Throughput:          %-,15.0f ops/sec                  ║%n", throughput);
        System.out.printf("║   Release Calls:       %-,15d                           ║%n", totalReleaseCalls.get());
        System.out.printf("║   Events Released:     %-,15d                           ║%n", totalEventsReleased.get());
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ MEMORY USAGE                                                     ║");
        System.out.printf("║   Heap Before:         %-,15d MB                        ║%n", heapBefore / (1024 * 1024));
        System.out.printf("║   Heap After:          %-,15d MB                        ║%n", heapAfter / (1024 * 1024));
        System.out.printf("║   Peak Usage:          %-,15d MB                        ║%n", peakHeap / (1024 * 1024));
        System.out.printf("║   Memory Delta:        %+-15d MB                        ║%n", memoryDelta / (1024 * 1024));
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ CORRECTNESS VALIDATION                                           ║");
        System.out.printf("║   Accounts Tested:     %-,15d                           ║%n", expectedBalances.size());
        System.out.printf("║   Balance Mismatches:  %-,15d                           ║%n", balanceMismatches);
        System.out.printf("║   Sequence Errors:     %-,15d                           ║%n", sequenceErrors);
        System.out.printf("║   Status:              %-40s   ║%n", status);
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private void resetState() {
        expectedBalances.clear();
        actualBalances.clear();
        eventSequences.clear();
        totalEventsReleased.set(0);
        totalReleaseCalls.set(0);
        heapBefore = 0;
        heapAfter = 0;
        peakHeap = 0;
    }

    private long getHeapUsed() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getUsed();
    }

    // ═══════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════════

    private record Order(int ordinal, String accountId, boolean isCredit, long amount) {
    }

    private record StressTestResult(
            int orderCount,
            long durationMs,
            double throughputOpsPerSec,
            long heapBefore,
            long heapAfter,
            long peakHeap,
            int balanceMismatches,
            int sequenceErrors) {
    }
}
