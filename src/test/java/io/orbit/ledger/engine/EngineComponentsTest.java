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

        state.addPendingEvent(
                new LedgerEvent("key", 1, LedgerType.CREDIT, 100, java.time.Instant.now()));
        state.addPendingEvent(
                new LedgerEvent("key", 2, LedgerType.DEBIT, 30, java.time.Instant.now()));
        state.addPendingEvent(
                new LedgerEvent("key", 3, LedgerType.CREDIT, 50, java.time.Instant.now()));

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
        state.addPendingEvent(
                new LedgerEvent("key", 1, LedgerType.CREDIT, 100, java.time.Instant.now()));

        state.resetPendingDelta();
        state.getAndClearPendingEvents();
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
}

