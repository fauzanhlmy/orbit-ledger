#!/bin/sh
source ../../../java-17-env.sh

# ═══════════════════════════════════════════════════════════════════════════
# FULL STRESS TESTS (with balance validation)
# ═══════════════════════════════════════════════════════════════════════════

# Run ALL stress tests: 1M, 2M, 3M × COUNT, TIME, HYBRID (9 tests total)
mvn test -Dtest="OrbitLedgerStressTest#stressTest_COUNT+stressTest_TIME+stressTest_HYBRID"

# Run specific release type only:
# mvn test -Dtest="OrbitLedgerStressTest#stressTest_COUNT"
# mvn test -Dtest="OrbitLedgerStressTest#stressTest_TIME"
# mvn test -Dtest="OrbitLedgerStressTest#stressTest_HYBRID"

# ═══════════════════════════════════════════════════════════════════════════
# PURE THROUGHPUT TESTS (delta-only, NO balance validation)
# ═══════════════════════════════════════════════════════════════════════════

# Run ALL pure throughput tests (9 tests total)
# mvn test -Dtest="PureThroughputTest#pureThroughput_COUNT+pureThroughput_TIME+pureThroughput_HYBRID"

# Run specific pure throughput tests:
# mvn test -Dtest="PureThroughputTest#pureThroughput_COUNT"
# mvn test -Dtest="PureThroughputTest#pureThroughput_TIME"
# mvn test -Dtest="PureThroughputTest#pureThroughput_HYBRID"

# ═══════════════════════════════════════════════════════════════════════════
# ULTRA-MINIMAL TESTS (absolute minimum callback work - raw Disruptor speed)
# ═══════════════════════════════════════════════════════════════════════════

# Run ALL ultra-minimal tests (9 tests total)
mvn test -Dtest="PureThroughputTest#ultraMinimal_COUNT+ultraMinimal_TIME+ultraMinimal_HYBRID"

# Run specific ultra-minimal tests:
# mvn test -Dtest="PureThroughputTest#ultraMinimal_COUNT"
# mvn test -Dtest="PureThroughputTest#ultraMinimal_TIME"
# mvn test -Dtest="PureThroughputTest#ultraMinimal_HYBRID"

# ═══════════════════════════════════════════════════════════════════════════
# CSV GENERATION (for external testing)
# ═══════════════════════════════════════════════════════════════════════════

# Generate CSV files only:
# mvn test -Dtest="OrbitLedgerStressTest#generateCsvFile_1M+generateCsvFile_2M+generateCsvFile_3M"