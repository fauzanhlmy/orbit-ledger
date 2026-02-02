package io.orbit.ledger.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents transactions released from Orbit - the result of a commit
 * operation
 * containing the aggregated delta and the source events.
 *
 * <p>
 * When transactions are processed in Orbit Ledger, they accumulate in memory
 * until a commit is triggered. At that point, they are "released" from Orbit
 * and this record is emitted for persistence.
 * </p>
 *
 * @param key            The account/entity key
 * @param eventCount     Number of events included in this release
 * @param delta          Net balance change
 * @param durationNs     Duration of the commit operation in nanoseconds
 * @param events         List of events included in this release
 * @param runningBalance Balance after this release (null if balance tracking
 *                       disabled)
 * @since 1.0.0
 */
public record OrbitRelease(
        String key,
        long eventCount,
        long delta,
        long durationNs,
        List<LedgerEvent> events,
        Long runningBalance) {

    /**
     * Canonical constructor with validation.
     */
    public OrbitRelease {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(events, "events must not be null");
        events = Collections.unmodifiableList(events);
    }

    /**
     * Backward-compatible constructor without balance tracking.
     */
    public OrbitRelease(String key, long eventCount, long delta, long durationNs, List<LedgerEvent> events) {
        this(key, eventCount, delta, durationNs, events, null);
    }
}

