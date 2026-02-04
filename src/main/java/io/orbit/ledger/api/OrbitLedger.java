package io.orbit.ledger.api;

import io.orbit.ledger.builder.OrbitLedgerBuilder;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.PerformanceMode;
import io.orbit.ledger.enums.ReleaseType;
import io.orbit.ledger.model.OrbitRelease;

import java.time.Duration;

/**
 * Public API for the Orbit Ledger Engine.
 * <p>
 * This interface defines the contract for interacting with the ledger,
 * including event ingestion and lifecycle management.
 * </p>
 *
 * @since 1.0.0
 */
public interface OrbitLedger {

    /**
     * Creates a new builder for configuring the engine.
     *
     * @return a new {@link Builder} instance
     */
    static Builder builder() {
        return new OrbitLedgerBuilder();
    }

    /**
     * Starts the engine and prepares it for processing.
     */
    void start();

    /**
     * Shuts down the engine and releases resources.
     */
    void shutdown();

    /**
     * Submits a CREDIT event to the ledger.
     *
     * @param key    the account/entity key
     * @param amount the amount to credit (must be positive)
     */
    void credit(String key, long amount);

    /**
     * Submits a DEBIT event to the ledger.
     *
     * @param key    the account/entity key
     * @param amount the amount to debit (must be positive)
     */
    void debit(String key, long amount);

    /**
     * Manually triggers a release for a specific key.
     *
     * @param key the key to release
     * @return the {@link OrbitRelease} containing the released data, or null if no
     *         pending events
     */
    OrbitRelease release(String key);

    /**
     * Manually triggers a release for ALL keys found in the system.
     */
    void releaseAll();

    /**
     * Builder interface for {@link OrbitLedger}.
     */
    interface Builder {
        Builder bufferSize(int bufferSize);

        Builder threadCount(int threadCount);

        Builder releaseThreshold(int threshold);

        Builder releaseType(ReleaseType type);

        /**
         * Set the interval for time-based auto-release.
         * <p>
         * Required for {@code ReleaseType.TIME} and {@code ReleaseType.HYBRID}.
         * All keys with pending events will be released at this interval.
         * </p>
         *
         * @param interval time between auto-releases
         * @return this builder
         * @since 1.2.0
         */
        Builder releaseInterval(Duration interval);

        Builder onRelease(OrbitReleaseListener listener);

        /**
         * Set balance loader for initializing key balances.
         * <p>
         * Called once per key on first event for that key.
         * If using {@code AFTER_RELEASE} eviction, called again after each release.
         * </p>
         *
         * @param loader function to load initial balance from external source
         * @return this builder
         * @since 1.1.0
         */
        Builder balanceLoader(BalanceLoader loader);

        /**
         * Set default balance when no balanceLoader is provided.
         * <p>
         * Default: 0
         * </p>
         *
         * @param balance default initial balance for new keys
         * @return this builder
         * @since 1.1.0
         */
        Builder defaultBalance(long balance);

        /**
         * Set eviction policy for memory lifecycle.
         * <p>
         * Default: {@code NONE} (never evict, current behavior)
         * </p>
         *
         * @param policy eviction policy to use
         * @return this builder
         * @since 1.1.0
         */
        Builder evictionPolicy(EvictionPolicy policy);

        /**
         * Set performance mode for throughput/latency tuning.
         * <p>
         * Controls the trade-off between CPU usage and performance.
         * Default: {@code BALANCED} (low CPU, ~400K-500K ops/sec)
         * </p>
         *
         * @param mode performance mode to use
         * @return this builder
         * @since 1.3.0
         * @see PerformanceMode
         */
        Builder performanceMode(PerformanceMode mode);

        OrbitLedger build();
    }
}
