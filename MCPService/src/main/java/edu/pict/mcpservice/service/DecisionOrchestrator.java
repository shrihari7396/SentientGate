package edu.pict.mcpservice.service;

import edu.pict.mcpservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DecisionOrchestrator {

    private final PolicyClient policyClient;
    private final AIClient aiClient;
    private final DecisionCacheService cacheService;

    public void process(GatewayEvent event) {

        // 1. Build PolicyContext
        PolicyContext context = new PolicyContext(
                event.getStatusCode() >= 400 ? 1 : 0,
                1,
                0,
                event.getRoute(),
                "HIGH",
                true
        );

        // 2. Call Policy Service
        PolicyResult policy = policyClient.evaluate(context);

        // 3. Call AI Service
        AnomalyDetectionResponse ai = aiClient.analyze(
                AnomalyDetectionRequest.builder()
                        .failureRate(policy.getRiskScore() / 100.0)
                        .requestsPerMinute(1)
                        .jwtReuseCount(0)
                        .routeSensitivity("HIGH")
                        .build()
        );

        // 4. Decision logic (LOCK THIS)
        Decision decision;

        if (policy.isHardBlock()) {
            decision = buildDecision(DecisionType.BLOCK, policy, ai, 600);
        } else if (policy.getRiskScore() > 70 && ai.getConfidence() > 0.8) {
            decision = buildDecision(DecisionType.BLOCK, policy, ai, 300);
        } else if (policy.getRiskScore() > 40) {
            decision = buildDecision(DecisionType.THROTTLE, policy, ai, 120);
        } else {
            decision = buildDecision(DecisionType.ALLOW, policy, ai, 60);
        }

        // 5. Store in Redis
        cacheService.cacheByIp(event.getIp(), decision);

        log.info("Decision [{}] cached for IP={}", decision.getType(), event.getIp());
    }

    private Decision buildDecision(
            DecisionType type,
            PolicyResult policy,
            AnomalyDetectionResponse ai,
            long ttlSeconds) {

        return Decision.builder()
                .type(type)
                .riskScore(policy.getRiskScore())
                .aiConfidence(ai.getConfidence())
                .reason(String.join(", ", policy.getReasons()))
                .ttlSeconds(ttlSeconds)
                .build();
    }
}