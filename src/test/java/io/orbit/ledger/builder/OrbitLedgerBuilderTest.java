package io.orbit.ledger.builder;

import io.orbit.ledger.api.OrbitLedger;
import io.orbit.ledger.enums.ReleaseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

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
}

