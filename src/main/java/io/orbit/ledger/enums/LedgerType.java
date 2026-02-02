package io.orbit.ledger.enums;

/**
 * Ledger event type.
 *
 * @since 1.0.0
 */
public enum LedgerType {
    /** Credit operation - increases balance. */
    CREDIT,

    /** Debit operation - decreases balance. */
    DEBIT,

    /** Force release operation. */
    RELEASE,

    /** Flush all pending events (release all). */
    RELEASE_ALL
}

