package com.qaportfolio.sampleapp;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal Prometheus exposition (counter + latency histogram per route) so Prometheus/Grafana can chart the
 * app during load tests without extra libraries.
 */
final class Metrics {

    private static final double[] BUCKETS = {0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5};

    private static final class Series {
        final LongAdder count = new LongAdder();
        final LongAdder sumNanos = new LongAdder();
        final LongAdder[] buckets = new LongAdder[BUCKETS.length];

        Series() {
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new LongAdder();
            }
        }
    }

    private final Map<String, LongAdder> requests = new ConcurrentHashMap<>();
    private final Map<String, Series> latency = new ConcurrentHashMap<>();

    void record(String method, String route, int status, long nanos) {
        if ("/metrics".equals(route)) {
            return;
        }
        String r = route.isEmpty() ? "unmatched" : route;
        requests.computeIfAbsent(labels(method, r) + ",status=\"" + status + "\"", k -> new LongAdder()).increment();
        Series s = latency.computeIfAbsent(labels(method, r), k -> new Series());
        s.count.increment();
        s.sumNanos.add(nanos);
        double seconds = nanos / 1e9;
        for (int i = 0; i < BUCKETS.length; i++) {
            if (seconds <= BUCKETS[i]) {
                s.buckets[i].increment();
            }
        }
    }

    String prometheus() {
        StringBuilder sb = new StringBuilder();
        sb.append("# HELP demostore_http_requests_total HTTP requests by route and status\n");
        sb.append("# TYPE demostore_http_requests_total counter\n");
        new TreeMap<>(requests).forEach((k, v) ->
                sb.append("demostore_http_requests_total{").append(k).append("} ").append(v.sum()).append('\n'));
        sb.append("# HELP demostore_http_request_duration_seconds Request latency\n");
        sb.append("# TYPE demostore_http_request_duration_seconds histogram\n");
        new TreeMap<>(latency).forEach((k, s) -> {
            for (int i = 0; i < BUCKETS.length; i++) {
                sb.append("demostore_http_request_duration_seconds_bucket{").append(k).append(",le=\"")
                        .append(BUCKETS[i]).append("\"} ").append(s.buckets[i].sum()).append('\n');
            }
            sb.append("demostore_http_request_duration_seconds_bucket{").append(k).append(",le=\"+Inf\"} ")
                    .append(s.count.sum()).append('\n');
            sb.append("demostore_http_request_duration_seconds_sum{").append(k).append("} ")
                    .append(s.sumNanos.sum() / 1e9).append('\n');
            sb.append("demostore_http_request_duration_seconds_count{").append(k).append("} ")
                    .append(s.count.sum()).append('\n');
        });
        Runtime rt = Runtime.getRuntime();
        sb.append("# TYPE demostore_jvm_memory_used_bytes gauge\n");
        sb.append("demostore_jvm_memory_used_bytes ").append(rt.totalMemory() - rt.freeMemory()).append('\n');
        return sb.toString();
    }

    private static String labels(String method, String route) {
        return "method=\"" + method + "\",route=\"" + route + "\"";
    }
}
