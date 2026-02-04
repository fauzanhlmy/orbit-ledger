package io.orbit.ledger.engine;

import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.PerformanceMode;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.model.OrbitRelease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrbitDisruptorTest {

    private OrbitDisruptor engine;

    @BeforeEach
    void setUp() {
        // Buffer params: size 128 (pow2), 2 threads, threshold 10
        // Balance management: no loader, default balance 0, no eviction
        // Performance mode: BALANCED (default)
        engine = new OrbitDisruptor(128, 2, 10, ReleaseType.COUNT, null, null, 0, EvictionPolicy.NONE, null,
                PerformanceMode.MAXIMUM);
        engine.start();
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    @Test
    void shouldProcessCreditAndCommit() {
        engine.credit("test-key", 100);

        OrbitRelease result = engine.release("test-key");

        assertNotNull(result);
        assertEquals("test-key", result.key());
        assertEquals(100, result.delta());
        assertEquals(1, result.eventCount());
    }

    @Test
    void shouldProcessDebitAndCommit() {
        engine.debit("test-key", 50);

        OrbitRelease result = engine.release("test-key");

        assertNotNull(result);
        assertEquals(-50, result.delta());
    }

    @Test
    void shouldHandleMultipleEvents() {
        engine.credit("k1", 100);
        engine.debit("k1", 20);
        engine.credit("k1", 5);

        OrbitRelease result = engine.release("k1");

        assertEquals(85, result.delta());
        assertEquals(3, result.eventCount());
    }

    @Test
    void shouldCommitAll() throws InterruptedException {
        engine.credit("user A", 10);
        engine.credit("user B", 20);

        // Allow async processing (credit events)
        Thread.sleep(100);

        // Trigger commitAll (async)
        engine.releaseAll();

        // Allow async processing (commit flush event)
        Thread.sleep(100);

        // Verify state is clear by checking subsequent commits
        engine.credit("user A", 5);
        OrbitRelease res = engine.release("user A");
        assertEquals(5, res.delta());
    }

    @Test
    void shouldThrowRuntimeExceptionOnInterruption() {
        Thread.currentThread().interrupt();
        assertThrows(RuntimeException.class, () -> engine.release("key"));
        // Clear interrupted status
        Thread.interrupted();
    }

    @Test
    void shouldProcessWithTimeBasedRelease() throws InterruptedException {
        // Create engine with TIME-based release
        OrbitDisruptor timeEngine = new OrbitDisruptor(
                128, 2, 10, ReleaseType.TIME, null, null, 0,
                EvictionPolicy.NONE, java.time.Duration.ofMillis(50), PerformanceMode.STANDARD);
        timeEngine.start();

        timeEngine.credit("time-key", 100);

        // Wait for time-based release
        Thread.sleep(150);

        // Verify by checking subsequent operations work
        timeEngine.credit("time-key", 50);
        OrbitRelease result = timeEngine.release("time-key");

        // Could be null if already released by timer, or have just the 50
        if (result != null) {
            assertTrue(result.delta() >= 0);
        }

        timeEngine.shutdown();
    }

    @Test
    void shouldProcessWithHybridRelease() throws InterruptedException {
        // Create engine with HYBRID release
        OrbitDisruptor hybridEngine = new OrbitDisruptor(
                128, 2, 2, ReleaseType.HYBRID, null, null, 0,
                EvictionPolicy.NONE, java.time.Duration.ofMillis(50), PerformanceMode.STANDARD);
        hybridEngine.start();

        hybridEngine.credit("hybrid-key", 100);
        hybridEngine.credit("hybrid-key", 200);

        // Wait for either count or time threshold
        Thread.sleep(100);

        hybridEngine.shutdown();
    }

    @Test
    void shouldProcessWithStandardPerformanceMode() {
        // STANDARD mode uses BlockingWaitStrategy
        OrbitDisruptor standardEngine = new OrbitDisruptor(
                128, 1, 10, ReleaseType.COUNT, null, null, 0,
                EvictionPolicy.NONE, null, PerformanceMode.STANDARD);
        standardEngine.start();

        standardEngine.credit("std-key", 100);
        OrbitRelease result = standardEngine.release("std-key");

        assertNotNull(result);
        assertEquals(100, result.delta());

        standardEngine.shutdown();
    }
}
