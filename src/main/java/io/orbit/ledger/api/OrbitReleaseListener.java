package io.orbit.ledger.api;

import io.orbit.ledger.model.OrbitRelease;

/**
 * Listener interface for commit/release events.
 */
@FunctionalInterface
public interface OrbitReleaseListener {
    void onRelease(OrbitRelease release);
}

