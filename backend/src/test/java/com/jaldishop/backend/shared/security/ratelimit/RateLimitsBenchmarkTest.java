package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class RateLimitsBenchmarkTest {

    @Test
    void runBenchmark() throws Exception {
        System.out.println("Running rate limits micro-benchmark...");
        var env = new MockEnvironment();
        var limits = new RateLimits(env);

        int threads = 16;
        int operationsPerThread = 20_000;
        int keySpace = 100; // 100 distinct keys to simulate concurrent distinct traffic

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Warmup
        for (int i = 0; i < 5_000; i++) {
            try {
                limits.check("bench", "key-" + (i % keySpace), 100_000, Duration.ofMinutes(1));
            } catch (Exception ignored) {}
        }

        CountDownLatch latch = new CountDownLatch(threads);
        long start = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread-" + (threadId % keySpace) + "-" + (i % 10);
                        try {
                            limits.check("bench", key, 100_000, Duration.ofMinutes(1));
                        } catch (RateLimitExceeded ignored) {}
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long elapsedNanos = System.nanoTime() - start;
        executor.shutdown();

        long totalOps = (long) threads * operationsPerThread;
        double elapsedMs = elapsedNanos / 1_000_000.0;
        double opsPerSec = (totalOps / (double) elapsedNanos) * 1_000_000_000.0;

        System.out.printf("BENCHMARK RESULT: Total ops: %d, Elapsed: %.2f ms, Throughput: %.0f ops/sec%n",
                totalOps, elapsedMs, opsPerSec);
    }
}
