# ⚡ Orbit Ledger

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

> **Deterministic In-Memory Ledger Engine for High-Throughput Balance Systems**

Built on [LMAX Disruptor](https://www.baeldung.com/lmax-disruptor-concurrency) for lock-free, nanosecond-level performance.

---

## 📚 Documentation

**[→ View Full Documentation](https://fauzanhlmy.github.io/orbit-ledger/)**

---

## ✨ What It Does

```
You call:        Orbit.credit("ACC-001", 1000)
                 Orbit.debit("ACC-001", 300)
                 Orbit.credit("ACC-001", 500)

You receive (via onRelease):
  OrbitRelease {
      delta: +1200,
      runningBalance: 1200,
      events: [Event1, Event2, Event3]
  }
```

**Orbit Ledger does NOT touch your database.** It emits immutable balance deltas — you decide how and when to persist them.

---

## 🚀 Quick Start

```xml
<dependency>
    <groupId>io.github.fauzanhlmy</groupId>
    <artifactId>orbit-ledger</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
OrbitLedger orbit = OrbitLedger.builder()
    .onRelease(result -> {
        db.updateBalance(result.key(), result.runningBalance());
        db.insertEvents(result.events());
    })
    .build();

orbit.start();
orbit.credit("ACC-001", 100_000);
orbit.debit("ACC-001", 30_000);
```

---

## ⚡ Why Orbit?

| Metric | Traditional DB | Orbit Ledger |
|--------|---------------|--------------|
| **Throughput** | ~10K ops/sec | >1M ops/sec |
| **Latency (p99)** | 10-100ms | <1μs |
| **DB Calls (per 10K txn)** | 30,000 | 20 |
| **Lock Contention** | High | None |

---

## 📖 Learn More

- [Getting Started](https://fauzanhlmy.github.io/orbit-ledger/getting-started.html)
- [Inside Orbit](https://fauzanhlmy.github.io/orbit-ledger/inside-orbit.html) — Architecture & Benchmarks
- [Examples](https://fauzanhlmy.github.io/orbit-ledger/examples.html)
- [API Reference](https://fauzanhlmy.github.io/orbit-ledger/api-reference.html)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

---

Built with ❤️ by [Fauzan Hilmy Abyan](https://linkedin.com/in/fauzan-hilmy)
