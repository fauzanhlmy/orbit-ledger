package io.orbit.ledger.enums;

/**
 * Performance mode for the Orbit Ledger engine.
 * <p>
 * Controls the trade-off between CPU usage and throughput/latency.
 * Higher performance modes consume more CPU but provide better throughput.
 * </p>
 *
 * @since 1.3.0
 */
public enum PerformanceMode {

    /**
     * STANDARD - Default mode with low CPU usage.
     * <p>
     * Suitable for most production workloads. Uses thread parking
     * when waiting for new events, minimizing CPU consumption.
     * </p>
     * <ul>
     * <li>Throughput: ~400K-500K ops/sec</li>
     * <li>CPU Usage: Low</li>
     * <li>Recommended for: General production use</li>
     * </ul>
     */
    STANDARD,

    /**
     * MAXIMUM - Maximum throughput with moderate CPU usage.
     * <p>
     * Uses thread yielding instead of parking, trading some CPU
     * for better throughput and lower latency.
     * </p>
     * <ul>
     * <li>Throughput: ~1M-2M ops/sec</li>
     * <li>CPU Usage: Medium</li>
     * <li>Recommended for: High-volume batch processing</li>
     * </ul>
     */
    MAXIMUM
}
