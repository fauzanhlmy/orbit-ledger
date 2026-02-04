package io.orbit.ledger.core;

import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.model.LedgerEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the state for a single key (account/entity).
 * <p>
 * TRACKED BY SINGLE THREAD (Worker). NO LOCKS. NO ATOMICS.
 * </p>
 * <p>
 * OPTIMIZATION v1.2.0: Uses primitive arrays instead of ArrayList for
 * zero-allocation
 * event storage during high-throughput processing.
 * </p>
 *
 * @since 1.0.0
 */
public final class LedgerKeyState {

    // Initial capacity and growth factor for primitive arrays
    private static final int INITIAL_CAPACITY = 32;
    private static final float GROWTH_FACTOR = 1.5f;

    private long currentSequence = 0;
    private long pendingDelta = 0;
    private long lastCommittedSequence = 0;

    // Balance tracking (optional feature, since 1.1.0)
    private boolean initialized = false;
    private long committedBalance = 0;

    // ═══════════════════════════════════════════════════════════════
    // PRIMITIVE ARRAYS for zero-allocation event storage (v1.2.0)
    // ═══════════════════════════════════════════════════════════════
    private int pendingCount = 0;
    private long[] sequences = new long[INITIAL_CAPACITY];
    private int[] types = new int[INITIAL_CAPACITY]; // LedgerType.ordinal()
    private long[] amounts = new long[INITIAL_CAPACITY];
    private long[] timestamps = new long[INITIAL_CAPACITY];
    private long[] balancesAfter = new long[INITIAL_CAPACITY];

    public long nextSequence() {
        return ++currentSequence;
    }

    public void credit(long amount) {
        pendingDelta += amount;
    }

    public void debit(long amount) {
        pendingDelta -= amount;
    }

    /**
     * Adds a pending event using primitive storage.
     * OPTIMIZATION: Zero object allocation (v1.2.0).
     */
    public void addPendingEvent(long sequence, LedgerType type, long amount, long timestampMs, long balanceAfter) {
        ensureCapacity();
        int idx = pendingCount++;
        sequences[idx] = sequence;
        types[idx] = type.ordinal();
        amounts[idx] = amount;
        timestamps[idx] = timestampMs;
        balancesAfter[idx] = balanceAfter;
    }

    private void ensureCapacity() {
        if (pendingCount >= sequences.length) {
            int newCapacity = (int) (sequences.length * GROWTH_FACTOR);
            sequences = java.util.Arrays.copyOf(sequences, newCapacity);
            types = java.util.Arrays.copyOf(types, newCapacity);
            amounts = java.util.Arrays.copyOf(amounts, newCapacity);
            timestamps = java.util.Arrays.copyOf(timestamps, newCapacity);
            balancesAfter = java.util.Arrays.copyOf(balancesAfter, newCapacity);
        }
    }

    public long getPendingDelta() {
        return pendingDelta;
    }

    public void resetPendingDelta() {
        pendingDelta = 0;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public boolean isEmpty() {
        return pendingCount == 0;
    }

    /**
     * Returns pending events as immutable LedgerEvent list and clears internal
     * storage.
     * OPTIMIZATION: Only allocates on release, not per-event (v1.2.0).
     *
     * @param key the account key (needed for LedgerEvent construction)
     * @return list of LedgerEvent for user callback
     */
    public List<LedgerEvent> getAndClearPendingEvents(String key) {
        List<LedgerEvent> result = new ArrayList<>(pendingCount);
        LedgerType[] typeValues = LedgerType.values();

        for (int i = 0; i < pendingCount; i++) {
            result.add(new LedgerEvent(
                    key,
                    sequences[i],
                    typeValues[types[i]],
                    amounts[i],
                    Instant.ofEpochMilli(timestamps[i]),
                    balancesAfter[i]));
        }

        // Reset counters (arrays stay allocated for reuse)
        pendingCount = 0;
        return result;
    }

    public void setLastCommittedSequence(long seq) {
        this.lastCommittedSequence = seq;
    }

    public long getLastCommittedSequence() {
        return lastCommittedSequence;
    }

    // Balance tracking methods (since 1.1.0)

    public void initialize(long balance) {
        this.committedBalance = balance;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public long getCommittedBalance() {
        return committedBalance;
    }

    public void updateCommittedBalance(long delta) {
        this.committedBalance += delta;
    }

    public long getCurrentBalance() {
        return committedBalance + pendingDelta;
    }
}
