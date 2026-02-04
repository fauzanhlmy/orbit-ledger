package io.orbit.ledger.model;

import io.orbit.ledger.api.OrbitEvent;
import io.orbit.ledger.enums.LedgerType;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable ledger event representing a debit or credit operation.
 *
 * @param key          Account or entity key
 * @param sequence     Sequence number (monotonic per key)
 * @param type         Transaction type (CREDIT/DEBIT)
 * @param amount       Transaction amount (absolute value)
 * @param timestamp    Time of occurrence
 * @param balanceAfter Balance after this event (null if balance tracking
 *                     disabled)
 * @since 1.0.0
 */
public record LedgerEvent(
        String key,
        long sequence,
        LedgerType type,
        long amount,
        Instant timestamp,
        Long balanceAfter) implements OrbitEvent {

    /**
     * Canonical constructor with validation.
     */
    public LedgerEvent {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative: " + amount);
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative: " + sequence);
        }
    }

    /**
     * Backward-compatible constructor without balance tracking.
     */
    public LedgerEvent(String key, long sequence, LedgerType type, long amount, Instant timestamp) {
        this(key, sequence, type, amount, timestamp, null);
    }

    public static LedgerEvent of(String key, LedgerType type, long amount) {
        return new LedgerEvent(key, 0, type, amount, Instant.now(), null);
    }

    public LedgerEvent withSequence(long newSequence) {
        return new LedgerEvent(key, newSequence, type, amount, timestamp, balanceAfter);
    }

    public long signedAmount() {
        return type == LedgerType.CREDIT ? amount : -amount;
    }
}

