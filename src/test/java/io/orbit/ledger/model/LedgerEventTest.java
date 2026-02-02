package io.orbit.ledger.model;

import io.orbit.ledger.enums.LedgerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LedgerEvent.
 */
class LedgerEventTest {

    @Test
    @DisplayName("Create credit event with factory")
    void createCreditEvent() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.CREDIT, 1000);

        assertEquals("wallet-1", event.key());
        assertEquals(LedgerType.CREDIT, event.type());
        assertEquals(1000, event.amount());
        assertEquals(0, event.sequence());
        assertNotNull(event.timestamp());
    }

    @Test
    @DisplayName("Create debit event")
    void createDebitEvent() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.DEBIT, 500);

        assertEquals(LedgerType.DEBIT, event.type());
        assertEquals(500, event.amount());
    }

    @Test
    @DisplayName("Event with sequence assignment")
    void eventWithSequence() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.CREDIT, 1000);
        LedgerEvent sequenced = event.withSequence(42);

        assertEquals(42, sequenced.sequence());
        assertEquals(event.key(), sequenced.key());
        assertEquals(event.type(), sequenced.type());
    }

    @Test
    @DisplayName("Signed amount for credit is positive")
    void signedAmountCredit() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.CREDIT, 1000);
        assertEquals(1000, event.signedAmount());
    }

    @Test
    @DisplayName("Signed amount for debit is negative")
    void signedAmountDebit() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.DEBIT, 1000);
        assertEquals(-1000, event.signedAmount());
    }

    @Test
    @DisplayName("Null key throws exception")
    void nullKeyThrows() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () -> new LedgerEvent(null, 0, LedgerType.CREDIT, 100, now));
    }

    @Test
    @DisplayName("Blank key throws exception")
    void blankKeyThrows() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEvent("   ", 0, LedgerType.CREDIT, 100, now));
    }

    @Test
    @DisplayName("Negative amount throws exception")
    void negativeAmountThrows() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEvent("wallet-1", 0, LedgerType.CREDIT, -100, now));
    }

    @Test
    @DisplayName("Negative sequence throws exception")
    void negativeSequenceThrows() {
        Instant now = Instant.now();
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEvent("wallet-1", -1, LedgerType.CREDIT, 100, now));
    }

    @Test
    @DisplayName("Null type throws exception")
    void nullTypeThrows() {
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () -> new LedgerEvent("wallet-1", 0, null, 100, now));
    }

    @Test
    @DisplayName("Equals and hashCode")
    void equalsAndHashCode() {
        Instant ts = Instant.now();
        LedgerEvent e1 = new LedgerEvent("k1", 1, LedgerType.CREDIT, 100, ts);
        LedgerEvent e2 = new LedgerEvent("k1", 1, LedgerType.CREDIT, 100, ts);
        LedgerEvent e3 = new LedgerEvent("k2", 1, LedgerType.CREDIT, 100, ts);

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
        assertNotEquals(null, e1);
        assertNotEquals("string", e1);
    }

    @Test
    @DisplayName("toString contains key info")
    void toStringContainsInfo() {
        LedgerEvent event = LedgerEvent.of("wallet-1", LedgerType.CREDIT, 1000);
        String str = event.toString();

        assertTrue(str.contains("wallet-1"));
        assertTrue(str.contains("CREDIT"));
        assertTrue(str.contains("1000"));
    }
}

