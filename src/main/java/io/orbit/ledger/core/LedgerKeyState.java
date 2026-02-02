package io.orbit.ledger.core;

import io.orbit.ledger.model.LedgerEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the state for a single key (account/entity).
 * <p>
 * TRACKED BY SINGLE THREAD (Worker). NO LOCKS. NO ATOMICS.
 * </p>
 *
 * @since 1.0.0
 */
public final class LedgerKeyState {

    private long currentSequence = 0;
    private long pendingDelta = 0;
    private long lastCommittedSequence = 0;
    private final List<LedgerEvent> pendingEvents = new ArrayList<>();

    // Balance tracking (optional feature, since 1.1.0)
    private boolean initialized = false;
    private long committedBalance = 0; // Balance after last commit

    public long nextSequence() {
        return ++currentSequence;
    }

    public void credit(long amount) {
        pendingDelta += amount;
    }

    public void debit(long amount) {
        pendingDelta -= amount;
    }

    public void addPendingEvent(LedgerEvent event) {
        pendingEvents.add(event);
    }

    public long getPendingDelta() {
        return pendingDelta;
    }

    public void resetPendingDelta() {
        pendingDelta = 0;
    }

    public int getPendingCount() {
        return pendingEvents.size();
    }

    public boolean isEmpty() {
        return pendingEvents.isEmpty();
    }

    public List<LedgerEvent> getAndClearPendingEvents() {
        List<LedgerEvent> copy = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    public void setLastCommittedSequence(long seq) {
        this.lastCommittedSequence = seq;
    }

    public long getLastCommittedSequence() {
        return lastCommittedSequence;
    }

    // Balance tracking methods (since 1.1.0)

    /**
     * Initialize balance for this key.
     * Called once per key on first access.
     *
     * @param balance initial balance from external source
     */
    public void initialize(long balance) {
        this.committedBalance = balance;
        this.initialized = true;
    }

    /**
     * Check if this key has been initialized with a balance.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the committed balance (balance after last commit).
     *
     * @return committed balance
     */
    public long getCommittedBalance() {
        return committedBalance;
    }

    /**
     * Update committed balance after a commit operation.
     *
     * @param delta the delta to add to committed balance
     */
    public void updateCommittedBalance(long delta) {
        this.committedBalance += delta;
    }

    /**
     * Get current balance including pending delta.
     *
     * @return current balance (committed + pending)
     */
    public long getCurrentBalance() {
        return committedBalance + pendingDelta;
    }
}

