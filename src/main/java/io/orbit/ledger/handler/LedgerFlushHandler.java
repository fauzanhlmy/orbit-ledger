package io.orbit.ledger.handler;

import com.lmax.disruptor.EventHandler;
import io.orbit.ledger.core.LedgerRingEvent;

/**
 * Cleanup handler that runs after all work handlers have processed the event.
 * <p>
 * Clears the event state to allow GC of referenced objects (keys, futures)
 * while the event object itself remains in the RingBuffer.
 * </p>
 */
public class LedgerFlushHandler implements EventHandler<LedgerRingEvent> {

    @Override
    public void onEvent(LedgerRingEvent event, long sequence, boolean endOfBatch) {
        event.clear();
    }
}

