package io.orbit.ledger.engine;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.orbit.ledger.api.BalanceLoader;
import io.orbit.ledger.api.OrbitLedger;
import io.orbit.ledger.api.OrbitReleaseListener;
import io.orbit.ledger.core.LedgerRingEvent;
import io.orbit.ledger.core.LedgerRingEventFactory;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.LedgerType;
import io.orbit.ledger.handler.LedgerFlushHandler;
import io.orbit.ledger.handler.LedgerWorkHandler;
import io.orbit.ledger.model.OrbitRelease;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * LMAX Disruptor-based implementation of OrbitLedgerEngine.
 * <p>
 * Uses multicast (handleEventsWith) where all workers see all events,
 * effectively sharding by filtering in the handler based on key hash.
 * This guarantees strict ordering per-key without race conditions.
 * </p>
 */
public class OrbitDisruptor implements OrbitLedger {

    private final Disruptor<LedgerRingEvent> disruptor;
    private final List<LedgerWorkHandler> handlers = new ArrayList<>();
    private final ReleaseType releaseType;
    private final Duration releaseInterval;
    private ScheduledExecutorService scheduler;

    public OrbitDisruptor(
            int bufferSize,
            int threadCount,
            int releaseThreshold,
            ReleaseType releaseType,
            OrbitReleaseListener releaseListener,
            BalanceLoader balanceLoader,
            long defaultBalance,
            EvictionPolicy evictionPolicy,
            Duration releaseInterval) {

        this.releaseType = releaseType;
        this.releaseInterval = releaseInterval;

        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        this.disruptor = new Disruptor<>(
                new LedgerRingEventFactory(),
                bufferSize,
                threadFactory,
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        LedgerWorkHandler[] workHandlers = new LedgerWorkHandler[threadCount];
        for (int i = 0; i < threadCount; i++) {
            workHandlers[i] = new LedgerWorkHandler(
                    new ConcurrentHashMap<>(),
                    releaseThreshold,
                    releaseType,
                    releaseListener,
                    i,
                    threadCount,
                    balanceLoader,
                    defaultBalance,
                    evictionPolicy);
            handlers.add(workHandlers[i]);
        }
        disruptor.handleEventsWith(workHandlers).then(new LedgerFlushHandler());
    }

    @Override
    public void start() {
        disruptor.start();

        // Start time-based scheduler for TIME and HYBRID modes
        if ((releaseType == ReleaseType.TIME || releaseType == ReleaseType.HYBRID)
                && releaseInterval != null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Orbit-release-scheduler");
                t.setDaemon(true);
                return t;
            });

            long intervalMs = releaseInterval.toMillis();
            scheduler.scheduleAtFixedRate(
                    this::releaseAll,
                    intervalMs,
                    intervalMs,
                    TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void shutdown() {
        // Stop scheduler first
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Flush all pending events before shutdown
        releaseAll();

        // Then shutdown disruptor
        disruptor.shutdown();
    }

    @Override
    public void credit(String key, long amount) {
        publishEvent(key, amount, LedgerType.CREDIT);
    }

    @Override
    public void debit(String key, long amount) {
        publishEvent(key, amount, LedgerType.DEBIT);
    }

    private void publishEvent(String key, long amount, LedgerType type) {
        disruptor.publishEvent((event, sequence) -> event.set(key, type, amount));
    }

    @Override
    public OrbitRelease release(String key) {
        CompletableFuture<OrbitRelease> future = new CompletableFuture<>();
        disruptor.publishEvent((event, sequence) -> event.setCommit(key, future));

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for release", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error waiting for release", e);
        }
    }

    @Override
    public void releaseAll() {
        disruptor.publishEvent((event, sequence) -> event.setCommitFlush());
    }
}

