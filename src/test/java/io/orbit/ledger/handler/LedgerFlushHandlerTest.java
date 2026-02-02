package io.orbit.ledger.handler;

import io.orbit.ledger.core.LedgerRingEvent;
import io.orbit.ledger.enums.LedgerType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LedgerFlushHandlerTest {

    @Test
    void onEventClearsEvent() {
        LedgerFlushHandler handler = new LedgerFlushHandler();
        LedgerRingEvent event = new LedgerRingEvent();

        // Setup event with data
        event.set("test-key", LedgerType.CREDIT, 100);
        event.setSequence(12345L);

        // Verify pre-conditions
        assertEquals("test-key", event.getKey());
        assertEquals(12345L, event.getSequence());

        // Execute handler
        handler.onEvent(event, 1L, true);

        // Verify post-conditions (cleared)
        assertNull(event.getKey());
        assertNull(event.getType());
        assertEquals(0, event.getAmount());
        assertEquals(0, event.getSequence());
        assertNull(event.getResultFuture());
    }
}

