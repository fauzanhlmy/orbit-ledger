package io.orbit.ledger.api;

/**
 * Functional interface for loading initial balance of a key.
 * <p>
 * Called once per key on first access (first credit/debit event).
 * Thread-safe within key partition - each key is handled by a single worker.
 * </p>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * OrbitLedger ledger = OrbitLedger.builder()
 *         .balanceLoader(key -> database.getBalance(key))
 *         .evictionPolicy(EvictionPolicy.AFTER_COMMIT)
 *         .build();
 * }</pre>
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface BalanceLoader {

    /**
     * Load initial balance for a key.
     * <p>
     * This method is called:
     * <ul>
     * <li>Once per key on first event for that key</li>
     * <li>Again after eviction (if using {@code AFTER_COMMIT} policy)</li>
     * </ul>
     * </p>
     *
     * @param key the account/entity key
     * @return initial balance for the key, or 0 if not found
     */
    long load(String key);
}

