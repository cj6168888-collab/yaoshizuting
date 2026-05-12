package com.yaoshizuting.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestRunner {
    public static void main(String[] args) throws Exception {
        final String healthUrl = "http://localhost:8090/api/health";
        final String baseUrl = "http://localhost:8090/api";
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        int durationSec = 15;
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        AtomicLong success = new AtomicLong(0);
        AtomicLong failures = new AtomicLong(0);

        // Health load
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    long end = System.currentTimeMillis() + durationSec * 1000;
                    while (System.currentTimeMillis() < end) {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(healthUrl))
                                .GET()
                                .build();
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200) success.incrementAndGet(); else failures.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // SignCode load (no auth)
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    long end = System.currentTimeMillis() + durationSec * 1000;
                    while (System.currentTimeMillis() < end) {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/auth/sendCode/13900000099"))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200) success.incrementAndGet(); else failures.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start
        startLatch.countDown();
        doneLatch.await();

        long total = success.get() + failures.get();
        System.out.println("LoadTest Summary: total=" + total + ", success=" + success.get() + ", failures=" + failures.get());
        if (total > 0) {
            double rate = (double) success.get() / total * 100.0;
            System.out.printf("Success rate: %.2f%%\n", rate);
        }
    }
}
