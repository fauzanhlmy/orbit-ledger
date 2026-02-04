package io.orbit.ledger.handler;

import com.lmax.disruptor.EventHandler;
import io.orbit.ledger.api.BalanceLoader;
import io.orbit.ledger.api.OrbitReleaseListener;
import io.orbit.ledger.core.LedgerKeyState;
import io.orbit.ledger.core.LedgerRingEvent;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.model.OrbitRelease;
import io.orbit.ledger.model.LedgerEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes events from the RingBuffer.
 * <p>
 * Handles aggregation, state updates (balance), and commit logic.
 * Implements EventHandler (multicast) and filters by keyhash.
 * </p>
 */
public class LedgerWorkHandler implements EventHandler<LedgerRingEvent> {

    private final ConcurrentHashMap<String, LedgerKeyState> listKeyState;
    private final int releaseThreshold;
    private final ReleaseType releaseType;
    private final OrbitReleaseListener releaseListener;
    private final int workerId;
    private final int totalWorkers;

    // Balance management config (since 1.1.0)
    private final BalanceLoader balanceLoader;
    private final long defaultBalance;
    private final EvictionPolicy evictionPolicy;

    public LedgerWorkHandler(
            ConcurrentHashMap<String, LedgerKeyState> listKeyState,
            int releaseThreshold,
            ReleaseType releaseType,
            OrbitReleaseListener releaseListener,
            int workerId,
            int totalWorkers,
            BalanceLoader balanceLoader,
            long defaultBalance,
            EvictionPolicy evictionPolicy) {
        this.listKeyState = listKeyState;
        this.releaseThreshold = releaseThreshold;
        this.releaseType = releaseType;
        this.releaseListener = releaseListener;
        this.workerId = workerId;
        this.totalWorkers = totalWorkers;
        this.balanceLoader = balanceLoader;
        this.defaultBalance = defaultBalance;
        this.evictionPolicy = evictionPolicy;
    }

    private boolean shouldHandle(String key) {
        return Math.abs(key.hashCode() % totalWorkers) == workerId;
    }

    /**
     * OPTIMIZATION: Uses pre-computed hashCode from event (v1.2.0).
     */
    private boolean shouldHandleByHash(int keyHashCode) {
        return Math.abs(keyHashCode % totalWorkers) == workerId;
    }

    /**
     * Ensure key state is initialized with balance.
     * Called once per key on first access.
     */
    private void ensureInitialized(String key, LedgerKeyState state) {
        if (!state.isInitialized()) {
            long balance = balanceLoader != null
                    ? balanceLoader.load(key)
                    : defaultBalance;
            state.initialize(balance);
        }
    }

    @Override
    public void onEvent(LedgerRingEvent ledgerRingEvent, long sequence, boolean endOfBatch) {
        if (LedgerType.RELEASE_ALL == ledgerRingEvent.getType()) {
            this.releaseAll();
            return;
        }

        if (ledgerRingEvent.getKey() == null)
            return;
        // OPTIMIZATION: Use cached hashCode from event (v1.2.0)
        if (!this.shouldHandleByHash(ledgerRingEvent.getKeyHashCode())) {
            return;
        }

        String key = ledgerRingEvent.getKey();
        LedgerKeyState state = listKeyState.computeIfAbsent(key, k -> new LedgerKeyState());

        // Ensure balance is initialized
        ensureInitialized(key, state);

        if (LedgerType.RELEASE == ledgerRingEvent.getType()) {
            OrbitRelease result = this.doReleaseInstance(key, state);
            if (ledgerRingEvent.getResultFuture() != null) {
                ledgerRingEvent.getResultFuture().complete(result);
            }
            return;
        } else if (LedgerType.CREDIT == ledgerRingEvent.getType()) {
            state.credit(ledgerRingEvent.getAmount());
        } else if (LedgerType.DEBIT == ledgerRingEvent.getType()) {
            state.debit(ledgerRingEvent.getAmount());
        }

        // OPTIMIZATION: Store as primitives instead of creating LedgerEvent object
        // (v1.2.0)
        long balanceAfter = state.getCurrentBalance();
        state.addPendingEvent(
                state.nextSequence(),
                ledgerRingEvent.getType(),
                ledgerRingEvent.getAmount(),
                ledgerRingEvent.getTimestampMs(),
                balanceAfter);

        if ((ReleaseType.COUNT == releaseType || ReleaseType.HYBRID == releaseType)
                && state.getPendingCount() >= releaseThreshold) {
            this.doReleaseInstance(key, state);
        }
    }

    /**
     * Internal release logic.
     */
    private OrbitRelease doReleaseInstance(String key, LedgerKeyState state) {
        if (state.getPendingCount() == 0) {
            return null;
        }

        long delta = state.getPendingDelta();
        long eventCount = state.getPendingCount();
        // OPTIMIZATION: Creates LedgerEvent objects only at release time (v1.2.0)
        List<LedgerEvent> batchEvents = state.getAndClearPendingEvents(key);

        long startNs = System.nanoTime();
        state.resetPendingDelta();
        state.setLastCommittedSequence(state.getLastCommittedSequence() + eventCount);

        // Update committed balance
        state.updateCommittedBalance(delta);
        Long runningBalance = state.getCommittedBalance();

        OrbitRelease result = new OrbitRelease(
                key,
                eventCount,
                delta,
                System.nanoTime() - startNs,
                batchEvents,
                runningBalance);

        if (releaseListener != null) {
            releaseListener.onRelease(result);
        }

        // Apply eviction policy AFTER release callback completes
        if (evictionPolicy == EvictionPolicy.AFTER_RELEASE) {
            listKeyState.remove(key);
        }

        return result;
    }

    public void releaseAll() {
        for (Map.Entry<String, LedgerKeyState> entry : listKeyState.entrySet()) {
            String key = entry.getKey();
            if (this.shouldHandle(key)) {
                this.doReleaseInstance(key, entry.getValue());
            }
        }
    }
}
