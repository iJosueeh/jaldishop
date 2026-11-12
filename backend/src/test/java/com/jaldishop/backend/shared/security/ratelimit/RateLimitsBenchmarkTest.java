package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class RateLimitsBenchmarkTest {

    @Test
    void runBenchmark() throws Exception {
        System.out.println("Running rate limits micro-benchmark (in-memory Java ops/sec)...");
        var env = new MockEnvironment();
        var limits = new RateLimits(env);

        int threads = 16;
        int operationsPerThread = 20_000;
        int activeKeySpace = 100; // 80% repeat traffic on active keys
        int burstKeySpace = 20;   // 20% traffic that exceeds limit (burst)

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Multi-threaded JIT compilation warmup
        for (int i = 0; i < 20_000; i++) {
            try {
                limits.check("warmup", "key-" + (i % activeKeySpace), 50_000, Duration.ofMinutes(1));
            } catch (Exception ignored) {}
        }

        var admitted = new AtomicInteger();
        var rejected = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < operationsPerThread; i++) {
                        // 80% traffic on active keys (limit 50,000), 20% traffic on burst keys (limit 50)
                        boolean isActive = (i % 5) != 0;
                        String key;
                        int limit;
                        if (isActive) {
                            key = "active-" + (threadId % activeKeySpace) + "-" + (i % 10);
                            limit = 50_000;
                        } else {
                            key = "burst-" + (i % burstKeySpace);
                            limit = 50;
                        }

                        try {
                            limits.check("bench", key, limit, Duration.ofMinutes(1));
                            admitted.incrementAndGet();
                        } catch (RateLimitExceeded e) {
                            rejected.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        long start = System.nanoTime();
        startLatch.countDown();
        doneLatch.await();
        long elapsedNanos = System.nanoTime() - start;
        executor.shutdown();

        long totalOps = (long) threads * operationsPerThread;
        double elapsedMs = elapsedNanos / 1_000_000.0;
        double opsPerSec = (totalOps / (double) elapsedNanos) * 1_000_000_000.0;

        System.out.printf("BENCHMARK RESULT:%n" +
                "  Total operations : %d%n" +
                "  Admitted ops     : %d (%.1f%%)%n" +
                "  Rejected ops     : %d (%.1f%%)%n" +
                "  Elapsed time     : %.2f ms%n" +
                "  Throughput       : %.0f in-memory ops/sec (approx %.2f µs/op)%n" +
                "  [NOTE: Measures JVM check method throughput; does not represent full HTTP network capacity]%n",
                totalOps, admitted.get(), (admitted.get() * 100.0 / totalOps),
                rejected.get(), (rejected.get() * 100.0 / totalOps),
                elapsedMs, opsPerSec, (elapsedNanos / (double) totalOps / 1000.0));
    }
}
