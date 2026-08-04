# Technical Notes — MCPService

## Overview
`MCPService` is the real-time security threat analysis and enforcement engine of **SentientGate**.

---

## 1. System Components & Architecture

### Key Service Classes
* **`SecurityEventListeners`**: Listens to Kafka topic `security-events` in batch mode (up to 500 events per poll). Groups events by `uuid` and dispatches execution tasks to `analysisExecutor`.
* **`McpAnalysisService`**: Main orchestrator. Enforces pre-analysis short-circuits (`blacklist` check, `checked` window check, and `dedup` check), fetches user history via gRPC, evaluates non-AI rule strategies sequentially, and offloads AI evaluation to `aiExecutor`.
* **`EventHistoryService`**: Encapsulates gRPC client calls to `LOGGING-SERVICE`. Features a 30-second Redis cache (`mcp:history:{uuid}:{duration}`) storing Base64-encoded Protobuf payloads (`UserLogEventResponse`), equipped with Resilience4j circuit breaker fallback.
* **`EnforcementService`**: Serializes block metadata (`BlockRecord`) and writes it reactively to Redis at key `blacklist:{uuid}` with dynamic TTL.
* **`AIClient` & `AiServiceFeignClient`**: OpenFeign client targeting `ai-service`. Wraps requests in Resilience4j `@CircuitBreaker` (`ai-service-circuit-breaker`) with fail-open fallback (`SERVICE_UNAVAILABLE`).

---

## 2. Execution Pipeline

```
Incoming Batch (Kafka)
   │
   ▼
Group by UUID -> Submit Task to analysisExecutor Pool
   │
   ▼
[Short-Circuit 1] Is UUID on Redis Blacklist? (blacklist:{uuid})  ──────────────> [YES] Abort
   │ [NO]
   ▼
[Short-Circuit 2] Was UUID Checked Recently? (mcp:checked:{uuid}) ──────────────> [YES] Abort
   │ [NO]
   ▼
Mark Checked in Redis (mcp:checked:{uuid}, TTL 200s)
   │
   ▼
Fetch 10-Min History via EventHistoryService (Redis Cache / gRPC Fallback)
   │
   ▼
Loop Through Batch Alerts:
   ├── Check Dedup (mcp:dedup:{uuid}:{errorCode}, TTL 30s)
   └── If First Occurrence -> Evaluate Rule Strategies 1 to 5
          │
          ├── [Rule Matched] ──> Execute Block via EnforcementService ─────────> Abort Pipeline
          └── [No Rule Matched] ──> Continue
   │
   ▼
[No Rule Matched] Dispatch Task to aiExecutor Pool
   │
   ▼
Execute AiAnomalyStrategy -> Call AI Inference Service via Feign
   │
   └── [Anomaly & Confidence > 0.85] ──> Execute Block (TTL 6 Hours)
```

---

## 3. Threat Strategy Order Matrix

| Order | Strategy Class | Condition / Criteria | TTL | Severity | Reason Code |
|-------|----------------|----------------------|-----|----------|-------------|
| **1** | `PatternMatchStrategy` | Path contains SQLi, XSS, Path Traversal, Command Injection patterns | 1 Day | `CRITICAL` | `CRITICAL_INJECTION_ATTEMPT` |
| **2** | `SensitivePathStrategy` | Path starts with sensitive endpoints (`/wp-admin`, `/.env`, `/config.php`, etc.) | 7 Days | `CRITICAL` | `SENSITIVE_PATH_RECONNAISSANCE` |
| **3** | `RateLimitCoolDownStrategy` | Alert error code is `429` | 15 Minutes | `LOW` | `Aggressive polling detected. 15m cool-down.` |
| **4** | `HighErrorRateStrategy` | History > 5 requests and error rate (>400 status) > 70% | 2 Hours | `MEDIUM` | `HIGH_ERROR_RATE_SCANNER_DETECTED` |
| **5** | `BurstTrafficStrategy` | History >= 20 requests in < 5000ms duration | 30 Minutes | `LOW` | `BURST_TRAFFIC_DETECTED_BOT_SUSPECT` |
| **6** | `AiAnomalyStrategy` | AI Inference output `isAnomaly == true` and `confidenceScore > 0.85` | 6 Hours | `MEDIUM` | `AI_BEHAVIORAL_ANOMALY_DETECTED` |

---

## 4. Redis Key Registry

* **`blacklist:{uuid}`**: Stores `BlockRecord` JSON object (`reason`, `severity`, `blockedAt`, `expiresAt`). Checked by API Gateway edge for block enforcement.
* **`mcp:dedup:{uuid}:{errorCode}`**: 30-second TTL guard to deduplicate processing of identical alert error codes per UUID.
* **`mcp:checked:{uuid}`**: 200-second TTL guard to prevent repetitive checks for compliant UUIDs.
* **`mcp:history:{uuid}:{duration}`**: 30-second TTL cache storing Base64 Protobuf byte payloads of gRPC history responses.

---

## 5. Thread Pools (`AsyncConfig`)

* **`analysisExecutor`**: `corePoolSize=8`, `maxPoolSize=16`, `queueCapacity=256`, Thread prefix `mcp-analysis-`.
* **`aiExecutor`**: `corePoolSize=4`, `maxPoolSize=8`, `queueCapacity=128`, Thread prefix `mcp-ai-`.

---

## 6. Circuit Breaker Settings (`ai-service-circuit-breaker`)

* `slidingWindowSize`: 10
* `minimumNumberOfCalls`: 5
* `failureRateThreshold`: 50%
* `slowCallRateThreshold`: 50% (threshold: 2s)
* `waitDurationInOpenState`: 10 seconds
* `permittedNumberOfCallsInHalfOpenState`: 3
* **Fallback Behavior**: Fail-open (`isAnomaly = false`, `confidenceScore = 0.0`, `patternDetected = "SERVICE_UNAVAILABLE"`).
