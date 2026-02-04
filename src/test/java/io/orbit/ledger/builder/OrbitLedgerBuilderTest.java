package io.orbit.ledger.builder;

import io.orbit.ledger.api.OrbitLedger;
import io.orbit.ledger.enums.EvictionPolicy;
import io.orbit.ledger.enums.PerformanceMode;
import io.orbit.ledger.enums.ReleaseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OrbitLedgerBuilder.
 */
class OrbitLedgerBuilderTest {

    @Test
    @DisplayName("Build with defaults")
    void buildWithDefaults() {
        OrbitLedger engine = OrbitLedger.builder()
                .onRelease(ctx -> {
                })
                .build();

        assertNotNull(engine);
    }

    @Test
    @DisplayName("Build with custom buffer size")
    void buildWithCustomBufferSize() {
        OrbitLedger engine = OrbitLedger.builder()
                .bufferSize(2048)
                .onRelease(ctx -> {
                })
                .build();

        assertNotNull(engine);
    }

    @Test
    @DisplayName("Buffer size must be power of 2")
    void bufferSizeMustBePowerOf2() {
        OrbitLedger.Builder builder = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.bufferSize(1000));
    }

    @Test
    @DisplayName("Buffer size must be positive")
    void bufferSizeMustBePositive() {
        OrbitLedger.Builder builder1 = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder1.bufferSize(0));

        OrbitLedger.Builder builder2 = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder2.bufferSize(-1));
    }

    @Test
    @DisplayName("Thread count must be positive")
    void threadCountMustBePositive() {
        OrbitLedger.Builder builder1 = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder1.threadCount(0));

        OrbitLedger.Builder builder2 = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder2.threadCount(-1));
    }

    @Test
    @DisplayName("Commit threshold can be zero")
    void releaseThresholdZero() {
        OrbitLedger engine = OrbitLedger.builder()
                .releaseThreshold(0)
                .onRelease(ctx -> {
                })
                .build();

        assertNotNull(engine);
    }

    @Test
    @DisplayName("Commit threshold must be non-negative")
    void releaseThresholdNonNegative() {
        OrbitLedger.Builder builder = OrbitLedger.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.releaseThreshold(-1));
    }

    @Test
    @DisplayName("Commit type cannot be null")
    void ReleaseTypeNotNull() {
        OrbitLedger.Builder builder = OrbitLedger.builder();
        assertThrows(NullPointerException.class, () -> builder.releaseType(null));
    }

    @Test
    @DisplayName("All commit types accepted")
    void allReleaseTypes() {
        for (ReleaseType type : ReleaseType.values()) {
            OrbitLedger engine = OrbitLedger.builder()
                    .releaseType(type)
                    .onRelease(ctx -> {
                    })
                    .build();
            assertNotNull(engine);
        }
    }

    @Test
    @DisplayName("Build with custom thread count")
    void buildWithCustomThreadCount() {
        OrbitLedger engine = OrbitLedger.builder()
                .threadCount(4)
                .build();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("Build with custom commit threshold")
    void buildWithCustomreleaseThreshold() {
        OrbitLedger engine = OrbitLedger.builder()
                .releaseThreshold(50)
                .build();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("Eviction policy cannot be null")
    void evictionPolicyNotNull() {
        OrbitLedger.Builder builder = OrbitLedger.builder();
        assertThrows(NullPointerException.class, () -> builder.evictionPolicy(null));
    }

    @Test
    @DisplayName("Performance mode cannot be null")
    void performanceModeNotNull() {
        OrbitLedger.Builder builder = OrbitLedger.builder();
        assertThrows(NullPointerException.class, () -> builder.performanceMode(null));
    }

    @Test
    @DisplayName("Build with release interval")
    void buildWithReleaseInterval() {
        OrbitLedger engine = OrbitLedger.builder()
                .releaseType(ReleaseType.TIME)
                .releaseInterval(Duration.ofMillis(100))
                .onRelease(ctx -> {
                })
                .build();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("Build with balance loader")
    void buildWithBalanceLoader() {
        OrbitLedger engine = OrbitLedger.builder()
                .balanceLoader(key -> 1000L)
                .onRelease(ctx -> {
                })
                .build();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("Build with default balance")
    void buildWithDefaultBalance() {
        OrbitLedger engine = OrbitLedger.builder()
                .defaultBalance(5000L)
                .onRelease(ctx -> {
                })
                .build();
        assertNotNull(engine);
    }

    @Test
    @DisplayName("All performance modes accepted")
    void allPerformanceModes() {
        for (PerformanceMode mode : PerformanceMode.values()) {
            OrbitLedger engine = OrbitLedger.builder()
                    .performanceMode(mode)
                    .onRelease(ctx -> {
                    })
                    .build();
            assertNotNull(engine);
        }
    }

    @Test
    @DisplayName("All eviction policies accepted")
    void allEvictionPolicies() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            OrbitLedger engine = OrbitLedger.builder()
                    .evictionPolicy(policy)
                    .onRelease(ctx -> {
                    })
                    .build();
            assertNotNull(engine);
        }
    }
}
