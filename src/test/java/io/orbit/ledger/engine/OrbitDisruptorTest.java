package io.orbit.ledger.engine;

import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.enums.EvictionPolicy;
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
        engine = new OrbitDisruptor(128, 2, 10, ReleaseType.COUNT, null, null, 0, EvictionPolicy.NONE, null);
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
}

