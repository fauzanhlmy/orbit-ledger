package io.orbit.ledger.api;

import io.orbit.ledger.enums.LedgerType;

import java.time.Instant;

/**
 * Represents a processed ledger event.
 *
 * @since 1.0.0
 */
public interface OrbitEvent {
    String key();

    long sequence();

    LedgerType type();

    long amount();

    Instant timestamp();
}

