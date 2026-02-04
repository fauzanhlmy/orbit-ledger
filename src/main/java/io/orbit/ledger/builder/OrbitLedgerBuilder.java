package io.orbit.ledger.builder;

import io.orbit.ledger.api.BalanceLoader;
import io.orbit.ledger.api.OrbitLedger;
import io.orbit.ledger.api.OrbitReleaseListener;
import io.orbit.ledger.engine.OrbitDisruptor;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.PerformanceMode;
import io.orbit.ledger.enums.ReleaseType;

import java.time.Duration;
import java.util.Objects;

/**
 * Builder for constructing {@link OrbitLedger} instances.
 *
 * @since 1.0.0
 */
public final class OrbitLedgerBuilder implements OrbitLedger.Builder {

    private int bufferSize = 1024;
    private int threadCount = 1;
    private int releaseThreshold = 1000;
    private ReleaseType releaseType = ReleaseType.COUNT;
    private OrbitReleaseListener releaseListener;

    // Balance management config (since 1.1.0)
    private BalanceLoader balanceLoader;
    private long defaultBalance = 0;
    private EvictionPolicy evictionPolicy = EvictionPolicy.NONE;

    // Time-based release config (since 1.2.0)
    private Duration releaseInterval;

    // Performance mode config (since 1.3.0)
    private PerformanceMode performanceMode = PerformanceMode.STANDARD;

    @Override
    public OrbitLedger.Builder bufferSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("bufferSize must be positive: " + size);
        }
        if ((size & (size - 1)) != 0) {
            throw new IllegalArgumentException("bufferSize must be a power of 2: " + size);
        }
        this.bufferSize = size;
        return this;
    }

    @Override
    public OrbitLedger.Builder threadCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("threadCount must be positive: " + count);
        }
        this.threadCount = count;
        return this;
    }

    @Override
    public OrbitLedger.Builder releaseThreshold(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("releaseThreshold must be non-negative: " + threshold);
        }
        this.releaseThreshold = threshold;
        return this;
    }

    @Override
    public OrbitLedger.Builder releaseType(ReleaseType type) {
        this.releaseType = Objects.requireNonNull(type, "releaseType must not be null");
        return this;
    }

    @Override
    public OrbitLedger.Builder onRelease(OrbitReleaseListener listener) {
        this.releaseListener = listener;
        return this;
    }

    @Override
    public OrbitLedger.Builder releaseInterval(Duration interval) {
        this.releaseInterval = interval;
        return this;
    }

    @Override
    public OrbitLedger.Builder balanceLoader(BalanceLoader loader) {
        this.balanceLoader = loader;
        return this;
    }

    @Override
    public OrbitLedger.Builder defaultBalance(long balance) {
        this.defaultBalance = balance;
        return this;
    }

    @Override
    public OrbitLedger.Builder evictionPolicy(EvictionPolicy policy) {
        this.evictionPolicy = Objects.requireNonNull(policy, "evictionPolicy must not be null");
        return this;
    }

    @Override
    public OrbitLedger.Builder performanceMode(PerformanceMode mode) {
        this.performanceMode = Objects.requireNonNull(mode, "performanceMode must not be null");
        return this;
    }

    @Override
    public OrbitLedger build() {
        return new OrbitDisruptor(
                bufferSize,
                threadCount,
                releaseThreshold,
                releaseType,
                releaseListener,
                balanceLoader,
                defaultBalance,
                evictionPolicy,
                releaseInterval,
                performanceMode);
    }
}
