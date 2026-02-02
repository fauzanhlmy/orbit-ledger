package io.orbit.ledger.handler;

import io.orbit.ledger.api.OrbitReleaseListener;
import io.orbit.ledger.core.LedgerKeyState;
import io.orbit.ledger.core.LedgerRingEvent;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.model.OrbitRelease;
import io.orbit.ledger.model.LedgerEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LedgerWorkHandlerTest {

    @Test
    void shouldHandleCreditEvent() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event = new LedgerRingEvent();
        event.set("user1", LedgerType.CREDIT, 100);
        event.setSequence(1);

        handler.onEvent(event, 1, true);

        assertTrue(keyStates.containsKey("user1"));
        LedgerKeyState state = keyStates.get("user1");
        assertEquals(100, state.getPendingDelta());
        assertEquals(1, state.getPendingCount());
    }

    @Test
    void shouldHandleDebitEvent() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event = new LedgerRingEvent();
        event.set("user1", LedgerType.DEBIT, 50);
        event.setSequence(1);

        handler.onEvent(event, 1, true);

        assertTrue(keyStates.containsKey("user1"));
        LedgerKeyState state = keyStates.get("user1");
        assertEquals(-50, state.getPendingDelta());
    }

    @Test
    void shouldIgnoreEventForOtherWorker() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        // Total workers 2. We are worker 0.
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 2,
                null, 0, EvictionPolicy.NONE);

        // Find a key that hashes to worker 1
        int i = 0;
        while (Math.abs(("k" + i).hashCode() % 2) != 1) {
            i++;
        }
        String keyForWorker1 = "k" + i;

        LedgerRingEvent event = new LedgerRingEvent();
        event.set(keyForWorker1, LedgerType.CREDIT, 100);
        event.setSequence(1);

        handler.onEvent(event, 1, true);

        assertFalse(keyStates.containsKey(keyForWorker1), "Should not process key belonging to another worker");
    }

    @Test
    void shouldCommitWhenThresholdReached() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<OrbitRelease> commitRef = new AtomicReference<>();
        OrbitReleaseListener listener = commitRef::set;

        // Threshold 2
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 2, ReleaseType.COUNT, listener, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event1 = new LedgerRingEvent();
        event1.set("user1", LedgerType.CREDIT, 10);
        event1.setSequence(1);

        // First event, count = 1. No commit.
        handler.onEvent(event1, 1, false);
        assertNull(commitRef.get());

        LedgerRingEvent event2 = new LedgerRingEvent();
        event2.set("user1", LedgerType.CREDIT, 20);
        event2.setSequence(2);

        // Second event, count = 2. Should commit.
        handler.onEvent(event2, 2, true);

        assertNotNull(commitRef.get());
        assertEquals("user1", commitRef.get().key());
        assertEquals(2, commitRef.get().eventCount());
        assertEquals(30, commitRef.get().delta());

        // State should be reset
        LedgerKeyState state = keyStates.get("user1");
        assertEquals(0, state.getPendingCount());
        assertEquals(0, state.getPendingDelta());
        assertEquals(2, state.getLastCommittedSequence());
    }

    @Test
    void shouldHandleExplicitCommit() throws ExecutionException, InterruptedException {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.NONE);

        // 1. Add some pending events
        LedgerRingEvent creditEvent = new LedgerRingEvent();
        creditEvent.set("user1", LedgerType.CREDIT, 100);
        creditEvent.setSequence(1);
        handler.onEvent(creditEvent, 1, false);

        // 2. Send Commit event
        CompletableFuture<OrbitRelease> future = new CompletableFuture<>();
        LedgerRingEvent commitEvent = new LedgerRingEvent();
        commitEvent.setCommit("user1", future);
        commitEvent.setSequence(2);

        handler.onEvent(commitEvent, 2, true);

        // 3. Verify future completed
        assertTrue(future.isDone());
        OrbitRelease result = future.get();
        assertEquals(100, result.delta());
        assertEquals(1, result.eventCount());

        // Verify state cleared
        LedgerKeyState state = keyStates.get("user1");
        assertEquals(0, state.getPendingCount());
    }

    @Test
    void shouldIgnoreNullKey() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event = new LedgerRingEvent();
        // Key is null by default in new instance or we set it null
        event.set(null, LedgerType.CREDIT, 100);

        handler.onEvent(event, 1, true);
        assertTrue(keyStates.isEmpty());
    }

    @Test
    void commitAllShouldCommitAllKeys() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<Integer> commitCount = new AtomicReference<>(0);

        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT,
                res -> commitCount.updateAndGet(v -> v + 1), 0, 1,
                null, 0, EvictionPolicy.NONE);

        // User 1
        LedgerRingEvent e1 = new LedgerRingEvent();
        e1.set("user1", LedgerType.CREDIT, 10);
        handler.onEvent(e1, 1, false);

        // User 2
        LedgerRingEvent e2 = new LedgerRingEvent();
        e2.set("user2", LedgerType.CREDIT, 20);
        handler.onEvent(e2, 2, false);

        handler.releaseAll();

        assertEquals(2, commitCount.get());
        assertEquals(0, keyStates.get("user1").getPendingCount());
        assertEquals(0, keyStates.get("user2").getPendingCount());
    }

    @Test
    void shouldHandleCommitFlushEvent() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<Integer> commitCount = new AtomicReference<>(0);

        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT,
                res -> commitCount.updateAndGet(v -> v + 1), 0, 1,
                null, 0, EvictionPolicy.NONE);

        // Setup state
        LedgerKeyState state = keyStates.computeIfAbsent("user1", k -> new LedgerKeyState());
        state.initialize(0); // Initialize balance
        state.credit(50);
        state.addPendingEvent(new LedgerEvent("user1", 1, LedgerType.CREDIT, 50, Instant.now()));

        // Send COMMIT_FLUSH event
        LedgerRingEvent event = new LedgerRingEvent();
        event.setCommitFlush();

        handler.onEvent(event, 1, true);

        assertEquals(1, commitCount.get());
        assertEquals(0, state.getPendingCount());
    }

    @Test
    void shouldCommitWhenThresholdReachedWithMixedType() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<OrbitRelease> commitRef = new AtomicReference<>();
        OrbitReleaseListener listener = commitRef::set;

        // Mixed type, threshold 2
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 2, ReleaseType.HYBRID, listener, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event1 = new LedgerRingEvent();
        event1.set("user1", LedgerType.CREDIT, 10);
        handler.onEvent(event1, 1, false);
        assertNull(commitRef.get());

        LedgerRingEvent event2 = new LedgerRingEvent();
        event2.set("user1", LedgerType.CREDIT, 20);
        handler.onEvent(event2, 2, true);

        assertNotNull(commitRef.get());
        assertEquals(2, commitRef.get().eventCount());
    }

    @Test
    void shouldNotCommitWhenThresholdReachedWithTimeType() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<OrbitRelease> commitRef = new AtomicReference<>();
        OrbitReleaseListener listener = commitRef::set;

        // Time type, threshold 2 (should be ignored for auto-commit)
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 2, ReleaseType.TIME, listener, 0, 1,
                null, 0, EvictionPolicy.NONE);

        LedgerRingEvent event1 = new LedgerRingEvent();
        event1.set("user1", LedgerType.CREDIT, 10);
        handler.onEvent(event1, 1, false);

        LedgerRingEvent event2 = new LedgerRingEvent();
        event2.set("user1", LedgerType.CREDIT, 20);
        handler.onEvent(event2, 2, true);

        assertNull(commitRef.get());

        // Manual commit should still work
        handler.releaseAll();
        // Since we commit directly, the listener runs
        assertNotNull(commitRef.get());
    }

    @Test
    void shouldCompleteFutureWithNullWhenCommittingEmptyState() throws ExecutionException, InterruptedException {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.NONE);

        // Explicit commit event for a key with NO pending events
        CompletableFuture<OrbitRelease> future = new CompletableFuture<>();
        LedgerRingEvent commitEvent = new LedgerRingEvent();
        commitEvent.setCommit("userEmpty", future);
        commitEvent.setSequence(1);

        handler.onEvent(commitEvent, 1, true);

        assertTrue(future.isDone());
        // Should be null as per implementation: if (state.getPendingCount() == 0)
        // return null;
        assertNull(future.get());
    }

    @Test
    void shouldLoadBalanceFromLoader() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();
        AtomicReference<OrbitRelease> commitRef = new AtomicReference<>();

        // Use balance loader that returns 1000
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, commitRef::set, 0, 1,
                key -> 1000L, 0, EvictionPolicy.NONE);

        LedgerRingEvent event = new LedgerRingEvent();
        event.set("user1", LedgerType.CREDIT, 500);
        handler.onEvent(event, 1, true);

        // Manually commit
        handler.releaseAll();

        assertNotNull(commitRef.get());
        assertEquals(500, commitRef.get().delta());
        assertEquals(1500L, commitRef.get().runningBalance()); // 1000 + 500
    }

    @Test
    void shouldEvictAfterCommit() {
        ConcurrentHashMap<String, LedgerKeyState> keyStates = new ConcurrentHashMap<>();

        // Use AFTER_RELEASE eviction
        LedgerWorkHandler handler = new LedgerWorkHandler(keyStates, 10, ReleaseType.COUNT, null, 0, 1,
                null, 0, EvictionPolicy.AFTER_RELEASE);

        LedgerRingEvent event = new LedgerRingEvent();
        event.set("user1", LedgerType.CREDIT, 100);
        handler.onEvent(event, 1, true);

        assertTrue(keyStates.containsKey("user1"));

        // Trigger commit
        handler.releaseAll();

        // After commit, key should be evicted
        assertFalse(keyStates.containsKey("user1"));
    }
}

