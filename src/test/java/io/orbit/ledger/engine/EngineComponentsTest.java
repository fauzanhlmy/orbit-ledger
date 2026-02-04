package io.orbit.ledger.engine;

import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.core.LedgerKeyState;
import io.orbit.ledger.core.LedgerRingEvent;
import io.orbit.ledger.core.LedgerRingEventFactory;
import io.orbit.ledger.model.OrbitRelease;
import io.orbit.ledger.model.LedgerEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for engine components.
 */
class EngineComponentsTest {

    @Test
    @DisplayName("KeyState sequence generation")
    void keyStateSequence() {
        LedgerKeyState state = new LedgerKeyState();

        assertEquals(1, state.nextSequence());
        assertEquals(2, state.nextSequence());
        assertEquals(3, state.nextSequence());
    }

    @Test
    @DisplayName("KeyState accumulation")
    void keyStateAccumulation() {
        LedgerKeyState state = new LedgerKeyState();

        state.credit(100);
        state.debit(30);
        state.credit(50);

        state.addPendingEvent(1, LedgerType.CREDIT, 100, System.currentTimeMillis(), 100);
        state.addPendingEvent(2, LedgerType.DEBIT, 30, System.currentTimeMillis(), 70);
        state.addPendingEvent(3, LedgerType.CREDIT, 50, System.currentTimeMillis(), 120);

        assertEquals(120, state.getPendingDelta());
        assertEquals(3, state.getPendingCount());
        assertFalse(state.isEmpty());
    }

    @Test
    @DisplayName("KeyState reset")
    void keyStateReset() {
        LedgerKeyState state = new LedgerKeyState();
        state.nextSequence();
        state.nextSequence();

        state.credit(100);
        state.addPendingEvent(1, LedgerType.CREDIT, 100, System.currentTimeMillis(), 100);

        state.resetPendingDelta();
        state.getAndClearPendingEvents("key");
        state.setLastCommittedSequence(2);

        assertEquals(0, state.getPendingDelta());
        assertEquals(0, state.getPendingCount());
        assertEquals(2, state.getLastCommittedSequence());
        assertTrue(state.isEmpty());
    }

    @Test
    @DisplayName("LedgerRingEvent set and get")
    void ledgerRingEventSetGet() {
        LedgerRingEvent event = new LedgerRingEvent();

        event.set("key1", LedgerType.CREDIT, 1000);

        assertEquals("key1", event.getKey());
        assertEquals(LedgerType.CREDIT, event.getType());
        assertEquals(1000, event.getAmount());
        assertTrue(event.getTimestampMs() > 0);
    }

    @Test
    @DisplayName("LedgerRingEvent signed amount")
    void ledgerRingEventSignedAmount() {
        LedgerRingEvent credit = new LedgerRingEvent();
        credit.set("k1", LedgerType.CREDIT, 100);
        assertEquals(100, credit.signedAmount());

        LedgerRingEvent debit = new LedgerRingEvent();
        debit.set("k1", LedgerType.DEBIT, 100);
        assertEquals(-100, debit.signedAmount());
    }

    @Test
    @DisplayName("LedgerRingEvent sequence")
    void ledgerRingEventSequence() {
        LedgerRingEvent event = new LedgerRingEvent();
        event.setSequence(42);
        assertEquals(42, event.getSequence());
    }

    @Test
    @DisplayName("LedgerRingEvent clear")
    void ledgerRingEventClear() {
        LedgerRingEvent event = new LedgerRingEvent();
        event.set("key1", LedgerType.CREDIT, 1000);
        event.setSequence(5);

        event.clear();

        assertNull(event.getKey());
        assertNull(event.getType());
        assertEquals(0, event.getAmount());
        assertEquals(0, event.getSequence());
    }

    @Test
    @DisplayName("LedgerRingEventFactory creates instance")
    void ledgerRingEventFactory() {
        LedgerRingEventFactory factory = new LedgerRingEventFactory();
        Object event = factory.newInstance();
        assertNotNull(event);
        assertTrue(event instanceof LedgerRingEvent);
    }

    @Test
    @DisplayName("LedgerRingEvent setCommit")
    void ledgerRingEventSetCommit() {
        LedgerRingEvent event = new LedgerRingEvent();
        java.util.concurrent.CompletableFuture<OrbitRelease> future = new java.util.concurrent.CompletableFuture<>();

        event.setCommit("commitKey", future);

        assertEquals("commitKey", event.getKey());
        assertEquals(LedgerType.RELEASE, event.getType());
        assertEquals(0, event.getAmount());
        assertNotNull(event.getResultFuture());
        assertSame(future, event.getResultFuture());
        assertTrue(event.getTimestampMs() > 0);
    }

    @Test
    @DisplayName("LedgerRingEvent setCommitFlush")
    void ledgerRingEventSetCommitFlush() {
        LedgerRingEvent event = new LedgerRingEvent();
        event.set("someKey", LedgerType.CREDIT, 100);

        event.setCommitFlush();

        assertNull(event.getKey());
        assertEquals(LedgerType.RELEASE_ALL, event.getType());
        assertEquals(0, event.getAmount());
        assertEquals(0, event.getTimestampMs());
        assertNull(event.getResultFuture());
    }

    @Test
    @DisplayName("LedgerRingEvent getKeyHashCode caching")
    void ledgerRingEventKeyHashCode() {
        LedgerRingEvent event = new LedgerRingEvent();

        // With key
        event.set("testKey", LedgerType.CREDIT, 100);
        assertEquals("testKey".hashCode(), event.getKeyHashCode());

        // With null key
        event.set(null, LedgerType.CREDIT, 100);
        assertEquals(0, event.getKeyHashCode());
    }

    @Test
    @DisplayName("KeyState balance initialization")
    void keyStateBalanceInitialization() {
        LedgerKeyState state = new LedgerKeyState();

        assertFalse(state.isInitialized());
        assertEquals(0, state.getCommittedBalance());

        state.initialize(1000);

        assertTrue(state.isInitialized());
        assertEquals(1000, state.getCommittedBalance());
    }

    @Test
    @DisplayName("KeyState balance updates")
    void keyStateBalanceUpdates() {
        LedgerKeyState state = new LedgerKeyState();
        state.initialize(1000);

        state.credit(500);
        assertEquals(1500, state.getCurrentBalance()); // 1000 + 500 pending

        state.updateCommittedBalance(500);
        assertEquals(1500, state.getCommittedBalance()); // 1000 + 500 committed

        state.resetPendingDelta();
        assertEquals(1500, state.getCurrentBalance()); // 1500 + 0 pending
    }

    @Test
    @DisplayName("KeyState capacity growth")
    void keyStateCapacityGrowth() {
        LedgerKeyState state = new LedgerKeyState();

        // Add more than initial capacity (32) events
        for (int i = 0; i < 50; i++) {
            state.addPendingEvent(i, LedgerType.CREDIT, 100, System.currentTimeMillis(), 100 * (i + 1));
        }

        assertEquals(50, state.getPendingCount());

        // getAndClearPendingEvents should return all 50
        var events = state.getAndClearPendingEvents("test");
        assertEquals(50, events.size());
        assertEquals(0, state.getPendingCount());
    }
}
