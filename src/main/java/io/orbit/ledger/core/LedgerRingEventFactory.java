package io.orbit.ledger.core;

import com.lmax.disruptor.EventFactory;

public class LedgerRingEventFactory implements EventFactory<LedgerRingEvent> {
    @Override
    public LedgerRingEvent newInstance() {
        return new LedgerRingEvent();
    }
}

