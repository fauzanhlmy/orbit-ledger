package io.orbit.ledger.enums;

/**
 * Release strategy type - determines when transactions are released from Orbit.
 * <p>
 * Choose based on your workload pattern:
 * <ul>
 * <li>{@link #COUNT} - High-volume keys only. Low-volume keys require manual
 * release.</li>
 * <li>{@link #TIME} - Guaranteed periodic release. Best for mixed/low-volume
 * workloads.</li>
 * <li>{@link #HYBRID} - Best of both. Fast release for high-volume, guaranteed
 * for low-volume.</li>
 * </ul>
 * </p>
 *
 * @since 1.0.0
 */
public enum ReleaseType {
    /**
     * Auto-release when event count reaches threshold.
     * <p>
     * <b>Warning:</b> Keys that never reach threshold will stay in memory
     * until explicit {@code release(key)} or {@code releaseAll()} is called.
     * </p>
     * <p>
     * Requires: {@code releaseThreshold}
     * </p>
     */
    COUNT,

    /**
     * Auto-release at fixed time intervals.
     * <p>
     * All keys with pending events are released periodically.
     * Best for workloads where latency to persistence matters more than batching.
     * </p>
     * <p>
     * Requires: {@code releaseInterval}
     * </p>
     */
    TIME,

    /**
     * Auto-release on EITHER count threshold OR time interval (whichever comes
     * first).
     * <p>
     * Best of both worlds:
     * <ul>
     * <li>High-volume keys release immediately when reaching threshold</li>
     * <li>Low-volume keys guaranteed to release within the interval</li>
     * </ul>
     * </p>
     * <p>
     * Requires: {@code releaseThreshold} AND {@code releaseInterval}
     * </p>
     */
    HYBRID
}

