package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.stratagies.blocking.AiAnomalyStrategy;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class McpAnalysisService {

    private static final String DEDUP_PREFIX = "mcp:dedup:";
    private static final String CHECKED_PREFIX = "mcp:checked:";
    private static final Duration DEDUP_WINDOW = Duration.ofSeconds(30);
    private static final Duration CHECKED_WINDOW = Duration.ofSeconds(200);

    private final EventHistoryService eventHistoryService;
    private final EnforcementService enforcementService;
    private final List<ThreatStrategy> strategies;
    private final StringRedisTemplate stringRedisTemplate;
    private final Executor aiExecutor;

    public McpAnalysisService(
            EventHistoryService eventHistoryService,
            EnforcementService enforcementService,
            List<ThreatStrategy> strategies,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("aiExecutor") Executor aiExecutor) {
        this.eventHistoryService = eventHistoryService;
        this.enforcementService = enforcementService;
        this.strategies = strategies;
        this.stringRedisTemplate = stringRedisTemplate;
        this.aiExecutor = aiExecutor;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Analyzes a batch of security alerts for a single UUID.
     *
     * <p>Flow:
     * <ol>
     *   <li>Skip if MCP recently checked this UUID</li>
     *   <li>Skip if UUID is already blocked</li>
     *   <li>Mark UUID as checked</li>
     *   <li>Fetch user history once via gRPC</li>
     *   <li>Run rule strategies per alert — first block stops everything</li>
     *   <li>If no rule matched — run AI analysis asynchronously</li>
     * </ol>
     */
    public void analyze(String uuid, List<SecurityAlertEvent> alerts) {

        if (isAlreadyBlocked(uuid)) return;
        if (wasRecentlyChecked(uuid)) return;

        markAsChecked(uuid);

        log.info("Analyzing {} alerts for UUID: {}", alerts.size(), uuid);

        List<LogEvent> history = fetchHistory(uuid);

        if (runRuleStrategies(uuid, alerts, history)) return;

        log.info("No synchronous malicious patterns found for UUID: {}", uuid);

        runAiAnalysisAsync(uuid, alerts, history);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GUARD CHECKS
    // ═══════════════════════════════════════════════════════════════════════

    /** Returns true if this UUID was already processed by MCP within the checked window. */
    private boolean wasRecentlyChecked(String uuid) {
        String checkedKey = CHECKED_PREFIX + uuid;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(checkedKey))) {
            log.debug("UUID {} was recently checked by MCP, skipping", uuid);
            return true;
        }
        return false;
    }

    /** Returns true if this UUID is currently on the Redis blacklist. */
    private boolean isAlreadyBlocked(String uuid) {
        if (Boolean.TRUE.equals(enforcementService.isBlocked(uuid))) {
            log.info("UUID {} already blocked, skipping analysis", uuid);
            return true;
        }
        return false;
    }

    /** Marks this UUID as checked so subsequent batches within the window are skipped. */
    private void markAsChecked(String uuid) {
        String checkedKey = CHECKED_PREFIX + uuid;
        stringRedisTemplate
                .opsForValue()
                .set(checkedKey, String.valueOf(System.currentTimeMillis()), CHECKED_WINDOW);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HISTORY
    // ═══════════════════════════════════════════════════════════════════════

    /** Fetches the last 10 minutes of user request history via gRPC and maps to LogEvent. */
    private List<LogEvent> fetchHistory(String uuid) {
        List<UserLogEvent> grpcList =
                eventHistoryService.getAllEventsInDuration(uuid, 10);

        return grpcList.stream()
                .map(
                        grpcEvent ->
                                LogEvent.builder()
                                        .uuid(grpcEvent.getUuid())
                                        .path(grpcEvent.getPath())
                                        .method(grpcEvent.getMethod())
                                        .latencyMs(grpcEvent.getLatencyMs())
                                        .queryParams(grpcEvent.getQueryParams())
                                        .clientIp(grpcEvent.getClientIp())
                                        .statusCode(grpcEvent.getStatusCode())
                                        .requestSize(grpcEvent.getRequestSize())
                                        .timestamp(grpcEvent.getTimestamp())
                                        .userAgent(grpcEvent.getUserAgent())
                                        .build())
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DEDUP
    // ═══════════════════════════════════════════════════════════════════════

    /** Returns true if this exact uuid+errorCode combination was NOT seen recently (first time). */
    private boolean isFirstOccurrence(String uuid, int errorCode) {
        String dedupKey = DEDUP_PREFIX + uuid + ":" + errorCode;
        Boolean isFirstSeen =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                dedupKey,
                                String.valueOf(System.currentTimeMillis()),
                                DEDUP_WINDOW);

        if (!Boolean.TRUE.equals(isFirstSeen)) {
            log.debug("Dedup: skipping duplicate event for {}", dedupKey);
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RULE STRATEGIES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Runs all non-AI strategies against each alert. First match blocks the user and returns true.
     *
     * @return true if a block was executed (caller should stop), false if no rule matched.
     */
    private boolean runRuleStrategies(
            String uuid, List<SecurityAlertEvent> alerts, List<LogEvent> history) {

        for (SecurityAlertEvent alert : alerts) {

            if (!isFirstOccurrence(uuid, alert.getErrorCode())) {
                continue;
            }

            Optional<ThreatStrategy> matched = findMatchingRuleStrategy(alert, history);

            if (matched.isPresent()) {
                ThreatStrategy strategy = matched.get();
                log.warn(
                        "Threat Detected! Strategy: {} | Reason: {}",
                        strategy.getClass().getSimpleName(),
                        strategy.getReason());

                enforcementService.blockUser(uuid, strategy);
                return true;
            }
        }
        return false;
    }

    /** Finds the first non-AI strategy that matches the given alert + history. */
    private Optional<ThreatStrategy> findMatchingRuleStrategy(
            SecurityAlertEvent alert, List<LogEvent> history) {

        return strategies.stream()
                .filter(s -> !(s instanceof AiAnomalyStrategy))
                .filter(s -> s.isAvailable(alert, history))
                .findFirst();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AI ANALYSIS
    // ═══════════════════════════════════════════════════════════════════════

    /** Submits AI anomaly detection to the dedicated AI executor. Uses the last alert as input. */
    private void runAiAnalysisAsync(
            String uuid, List<SecurityAlertEvent> alerts, List<LogEvent> history) {

        SecurityAlertEvent representativeAlert = alerts.get(alerts.size() - 1);

        strategies.stream()
                .filter(s -> s instanceof AiAnomalyStrategy)
                .findFirst()
                .ifPresent(
                        aiStrategy ->
                                CompletableFuture.runAsync(
                                        () -> executeAiStrategy(uuid, representativeAlert,
                                                history, aiStrategy),
                                        aiExecutor));
    }

    /** Runs the AI strategy and blocks the user if anomaly confidence exceeds threshold. */
    private void executeAiStrategy(
            String uuid,
            SecurityAlertEvent alert,
            List<LogEvent> history,
            ThreatStrategy aiStrategy) {
        try {
            if (aiStrategy.isAvailable(alert, history)) {
                log.warn(
                        "[AI] Threat Detected! Strategy: {}",
                        aiStrategy.getClass().getSimpleName());

                enforcementService.blockUser(uuid, aiStrategy);
            }
        } catch (Exception e) {
            log.error("AI Analysis failed asynchronously", e);
        }
    }
}
