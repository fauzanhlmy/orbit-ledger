package io.orbit.ledger.core;

import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.model.OrbitRelease;
import java.util.concurrent.CompletableFuture;

/**
 * Mutable event holder for Disruptor ring buffer.
 *
 * <p>
 * Pre-allocated and reused by Disruptor to avoid GC pressure.
 * </p>
 *
 * @since 1.0.0
 */
public final class LedgerRingEvent {

    private String key;
    private long sequence;
    private LedgerType type;
    private long amount;
    private long timestampMs;

    private CompletableFuture<OrbitRelease> resultFuture;

    public void set(String key, LedgerType type, long amount) {
        this.key = key;
        this.type = type;
        this.amount = amount;
        this.timestampMs = System.currentTimeMillis();
        this.resultFuture = null;
    }

    public void setCommit(String key, CompletableFuture<OrbitRelease> future) {
        this.key = key;
        this.type = LedgerType.RELEASE;
        this.amount = 0;
        this.timestampMs = System.currentTimeMillis();
        this.resultFuture = future;
    }

    public void setCommitFlush() {
        this.key = null;
        this.type = LedgerType.RELEASE_ALL;
        this.amount = 0;
        this.timestampMs = System.currentTimeMillis();
        this.resultFuture = null;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getKey() {
        return key;
    }

    public long getSequence() {
        return sequence;
    }

    public LedgerType getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public CompletableFuture<OrbitRelease> getResultFuture() {
        return resultFuture;
    }

    public long signedAmount() {
        return type == LedgerType.CREDIT ? amount : -amount;
    }

    public void clear() {
        this.key = null;
        this.sequence = 0;
        this.type = null;
        this.amount = 0;
        this.timestampMs = 0;
        this.resultFuture = null;
    }
}

