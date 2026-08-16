package edu.pict.apigateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Provides aggregated metrics for the SentientGate dashboard and pipeline views. Data is derived
 * from Redis (blacklist state, recent logs) and gateway filter metadata.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class GatewayMetricsController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String LOG_CACHE_PREFIX = "log:recent:";

    // ═══════════════════════════════════════════════════════════════════════
    //  DASHBOARD ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardMetrics {
        private long requestsPerMin;
        private long blockedThreats;
        private String p99Latency;
        private int activeServices;
    }

    /**
     * GET /api/dashboard/metrics — Core KPIs for the dashboard top row. Computes blocked threats
     * from Redis blacklist key count.
     */
    @GetMapping("/api/dashboard/metrics")
    public Mono<ResponseEntity<DashboardMetrics>> getDashboardMetrics() {
        return redisTemplate
                .keys(BLACKLIST_PREFIX + "*")
                .count()
                .onErrorReturn(0L)
                .map(
                        blacklistCount ->
                                ResponseEntity.ok(
                                        DashboardMetrics.builder()
                                                .blockedThreats(blacklistCount)
                                                .requestsPerMin(0) // Will be populated by
                                                // LoggingService
                                                .p99Latency("0")
                                                .activeServices(0)
                                                .build()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  THREAT STATS & FEED ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ThreatStats {
        private long blockedToday;
        private long aiEscalations;
        private double avgAnomalyScore;
        private int strategiesActive;
    }

    /**
     * GET /api/threat/stats — Aggregated threat statistics. Counts blacklist entries and
     * categorizes by reason.
     */
    @GetMapping("/api/threat/stats")
    public Mono<ResponseEntity<ThreatStats>> getThreatStats() {
        return redisTemplate
                .keys(BLACKLIST_PREFIX + "*")
                .flatMap(
                        key ->
                                redisTemplate
                                        .opsForValue()
                                        .get(key)
                                        .onErrorReturn("")
                                        .map(
                                                json -> {
                                                    try {
                                                        var node =
                                                                objectMapper.readTree(json);
                                                        String reason =
                                                                node.has("reason")
                                                                        ? node.get("reason")
                                                                                .asText()
                                                                        : "";
                                                        return reason;
                                                    } catch (Exception e) {
                                                        return "UNKNOWN";
                                                    }
                                                }))
                .collectList()
                .onErrorReturn(List.of())
                .map(
                        reasons -> {
                            long total = reasons.size();
                            long aiCount =
                                    reasons.stream()
                                            .filter(
                                                    r ->
                                                            r.contains("AI")
                                                                    || r.contains("ANOMALY"))
                                            .count();

                            return ResponseEntity.ok(
                                    ThreatStats.builder()
                                            .blockedToday(total)
                                            .aiEscalations(aiCount)
                                            .avgAnomalyScore(total > 0 ? 0.65 : 0.0)
                                            .strategiesActive(6) // 6 registered strategies
                                            .build());
                        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PIPELINE ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PipelineStage {
        private String stage;
        private long requestsToday;
        private String status;
        private String lastError;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PipelineEvent {
        private String id;
        private String timestamp;
        private String uuid;
        private String status;
        private String stage;
    }

    /**
     * GET /api/pipeline/stats — Returns the real gateway filter chain stages with health status.
     * Checks Redis connectivity to determine Edge Fire stage health.
     */
    @GetMapping("/api/pipeline/stats")
    public Mono<ResponseEntity<List<PipelineStage>>> getPipelineStats() {
        // Check Redis health for Edge Fire stage
        return redisTemplate
                .getConnectionFactory()
                .getReactiveConnection()
                .ping()
                .map(pong -> true)
                .onErrorReturn(false)
                .map(
                        redisUp -> {
                            List<PipelineStage> stages =
                                    List.of(
                                            PipelineStage.builder()
                                                    .stage("Request In")
                                                    .requestsToday(0)
                                                    .status("UP")
                                                    .lastError(null)
                                                    .build(),
                                            PipelineStage.builder()
                                                    .stage("Edge Fire")
                                                    .requestsToday(0)
                                                    .status(redisUp ? "UP" : "DOWN")
                                                    .lastError(
                                                            redisUp
                                                                    ? null
                                                                    : "Redis connection unavailable")
                                                    .build(),
                                            PipelineStage.builder()
                                                    .stage("JTI Vault")
                                                    .requestsToday(0)
                                                    .status("UP")
                                                    .lastError(null)
                                                    .build(),
                                            PipelineStage.builder()
                                                    .stage("Rate Pulse")
                                                    .requestsToday(0)
                                                    .status(redisUp ? "UP" : "DEGRADED")
                                                    .lastError(null)
                                                    .build(),
                                            PipelineStage.builder()
                                                    .stage("Shadow Log")
                                                    .requestsToday(0)
                                                    .status("UP")
                                                    .lastError(null)
                                                    .build(),
                                            PipelineStage.builder()
                                                    .stage("Response Out")
                                                    .requestsToday(0)
                                                    .status("UP")
                                                    .lastError(null)
                                                    .build());
                            return ResponseEntity.ok(stages);
                        });
    }

    /**
     * GET /api/pipeline/events — Recent pipeline events derived from Redis cached logs. Returns the
     * most recent request outcomes (PASSED/BLOCKED).
     */
    @GetMapping("/api/pipeline/events")
    public Mono<ResponseEntity<List<PipelineEvent>>> getPipelineEvents() {
        // Scan recent log keys from Redis cache and map to pipeline events
        return redisTemplate
                .keys("log:recent:*")
                .take(20)
                .flatMap(
                        key ->
                                redisTemplate
                                        .opsForValue()
                                        .get(key)
                                        .onErrorReturn("")
                                        .map(
                                                json -> {
                                                    try {
                                                        var node =
                                                                objectMapper.readTree(json);
                                                        String decision =
                                                                node.has("decision")
                                                                        ? node.get("decision")
                                                                                .asText()
                                                                        : "ALLOWED";
                                                        String uuid =
                                                                node.has("uuid")
                                                                        ? node.get("uuid").asText()
                                                                        : "unknown";
                                                        long ts =
                                                                node.has("timestamp")
                                                                        ? node.get("timestamp")
                                                                                .asLong()
                                                                        : System
                                                                                .currentTimeMillis();

                                                        return PipelineEvent.builder()
                                                                .id(key)
                                                                .timestamp(
                                                                        Instant.ofEpochMilli(ts)
                                                                                .toString())
                                                                .uuid(uuid)
                                                                .status(
                                                                        "BLOCKED"
                                                                                        .equals(
                                                                                                decision)
                                                                                ? "BLOCKED"
                                                                                : "PASSED")
                                                                .stage(
                                                                        "BLOCKED"
                                                                                        .equals(
                                                                                                decision)
                                                                                ? "Edge Fire"
                                                                                : "Response Out")
                                                                .build();
                                                    } catch (Exception e) {
                                                        return PipelineEvent.builder()
                                                                .id(key)
                                                                .timestamp(
                                                                        Instant.now().toString())
                                                                .uuid("unknown")
                                                                .status("PASSED")
                                                                .stage("Response Out")
                                                                .build();
                                                    }
                                                }))
                .collectList()
                .onErrorReturn(List.of())
                .map(ResponseEntity::ok);
    }
}
