package io.orbit.ledger.model;

import io.orbit.ledger.enums.LedgerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrbitRelease.
 */
class OrbitReleaseTest {

    @Test
    void createOrbitRelease() {
        OrbitRelease result = new OrbitRelease("key1", 5, 500, 1000, Collections.emptyList());

        assertEquals("key1", result.key());
        assertEquals(5, result.eventCount());
        assertEquals(500, result.delta());
        assertEquals(1000, result.durationNs());
        assertTrue(result.events().isEmpty());
        assertNull(result.runningBalance());
    }

    @Test
    void eventsListShouldBeImmutable() {
        List<LedgerEvent> events = new ArrayList<>();
        events.add(new LedgerEvent("key", 1, LedgerType.CREDIT, 100, Instant.now()));

        OrbitRelease result = new OrbitRelease("key", events.size(), 50, 200, events);

        assertThrows(UnsupportedOperationException.class, () -> {
            result.events().add(new LedgerEvent("key", 2, LedgerType.DEBIT, 50, Instant.now()));
        });
    }

    @Test
    void shouldRejectNullKey() {
        assertThrows(NullPointerException.class, () -> {
            new OrbitRelease(null, 10, 500, 5000, Collections.emptyList());
        });
    }

    @Test
    void shouldRejectNullEvents() {
        assertThrows(NullPointerException.class, () -> {
            new OrbitRelease("key1", 10, 500, 5000, null);
        });
    }

    @Test
    void createWithRunningBalance() {
        OrbitRelease result = new OrbitRelease("key1", 5, 500, 1000, Collections.emptyList(), 1500L);

        assertEquals("key1", result.key());
        assertEquals(5, result.eventCount());
        assertEquals(500, result.delta());
        assertEquals(1500L, result.runningBalance());
    }
}

