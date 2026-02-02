package io.orbit.ledger.enums;

/**
 * Policy for managing memory lifecycle of key states.
 * <p>
 * Controls when {@link io.orbit.ledger.core.LedgerKeyState} entries
 * are removed from memory after processing.
 * </p>
 *
 * @since 1.1.0
 */
public enum EvictionPolicy {

    /**
     * Never evict key states from memory (default).
     * <p>
     * Key states remain in memory indefinitely. Best for:
     * <ul>
     * <li>Small, fixed set of accounts</li>
     * <li>Short-lived applications</li>
     * <li>Maximum performance (no reload overhead)</li>
     * </ul>
     * </p>
     */
    NONE,

    /**
     * Remove key state after each release.
     * <p>
     * State is cleared after {@code onRelease} callback. Best for:
     * <ul>
     * <li>Large number of accounts</li>
     * <li>Stateless processing patterns</li>
     * <li>Memory-constrained environments</li>
     * </ul>
     * Requires {@code balanceLoader} to reload balance on next access.
     * </p>
     */
    AFTER_RELEASE
}

