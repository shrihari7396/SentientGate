# SentientGate Remediation Plan + SOLID Audit

Date: 2026-04-04
Scope: Security flaws, architectural flaws, and SOLID-driven refactor plan.

## 1. Execution Plan (Phased)

### Phase 0: Immediate Containment (Day 0)
- Rotate and remove leaked key material:
  - `/private_key.pem`
  - hardcoded secrets in `ApiGateway/src/main/resources/application.yml`
- Move secrets to environment variables for all services.
- Strengthen `.gitignore` for `*.pem`, `.env*`, `target/`, `build/`, `.idea/`, `application*.yml.bak`.
- Remove tracked secret backups like `ApiGateway/src/main/resources/application.yml.bak`.

Definition of Done:
- No secrets in tracked source.
- App starts with env-based secrets in local/dev profiles.

### Phase 1: Gateway Security Baseline (Day 1-2)
- Replace `permitAll()` with endpoint-level authorization policy.
- Rework CSRF policy:
  - enable for cookie/session browser flows, or
  - explicitly disable only for fully stateless token APIs with compensating controls.
- Integrate trusted proxy/IP extraction via `IpService` inside `SentientGateFilter`.
- Replace UUID-only blacklist checks with UUID + IP strategy.
- Remove production `assert` checks and replace with explicit validation/guard clauses.

Definition of Done:
- Protected internal routes require auth.
- IP extraction is deterministic and proxy-safe.
- Blacklist cannot be bypassed by only rotating UUID cookie.

### Phase 2: MCP Pipeline Performance + Safety (Day 2-4)
- Add pre-check: skip analysis when already blocked in Redis.
- Add dedup window per UUID/signature for repeated alerts.
- Add short TTL cache for recent history fetches.
- Split strategy execution:
  - fast deterministic strategies on consumer thread
  - slow AI strategy on async executor.
- Add timeout + circuit breaker for gRPC and AI calls.
- Restrict Kafka `trusted.packages` from `"*"` to explicit packages.

Definition of Done:
- Under burst traffic, consumer lag remains bounded.
- AI unavailability does not stall Kafka consumers.

### Phase 3: Service Integration Correctness (Day 4-5)
- Fix AI Feign client name/path mismatch.
- Implement gRPC server in LoggingService for history retrieval.
- Add gRPC deadlines and fallback paths in MCP.

Definition of Done:
- History-dependent strategies function correctly in integration tests.
- MCP handles LoggingService outage without thread starvation.

### Phase 4: Logging/Data Hardening (Day 5-6)
- Replace `parallelStream()` with `stream()` in ingestion mapping.
- Move `ddl-auto` to safer production profile (`validate`/migration-driven).
- Add DB indexes for dashboard query paths.
- Add short-lived cache for dashboard aggregates.
- Store block metadata as JSON, not `record.toString()`.

Definition of Done:
- Dashboard load no longer spikes DB repeatedly.
- Block records are machine-readable and auditable.

### Phase 5: Resilience Baseline (Day 6-7)
- Add profile-level HA guidance for Redis/Kafka/Postgres/service replicas.
- Keep dev-simple profile; add prod-safe profile defaults.

Definition of Done:
- Clear failover and scaling baseline exists for non-local environments.

### Phase 6: Verification + Documentation (Day 7)
- Add/expand unit + integration tests for all fixed flaw classes.
- Create fix tracker with:
  - flaw ID
  - changed files
  - tests
  - verification commands
  - residual risks.

Definition of Done:
- Each flaw has traceable fix evidence.

---

## 2. SOLID Violation Audit (Current Code)

## S — Single Responsibility Principle (SRP)

Break 1:
- File: `ApiGateway/src/main/java/edu/pict/apigateway/filters/global/SentientGateFilter.java`
- Problem:
  - same class extracts request metadata, decides alert severity, builds two event models, and publishes to Kafka.
- Fix:
  - split into:
    - `RequestContextExtractor`
    - `LogEventFactory`
    - `SecurityAlertFactory`
    - `SecurityEventPublisher`

Break 2:
- File: `MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java`
- Problem:
  - orchestrates history fetch, transforms data models, executes strategies, and triggers enforcement.
- Fix:
  - split orchestration:
    - `HistoryProvider`
    - `StrategyEvaluator`
    - `ThreatDecisionEngine`
    - `EnforcementCoordinator`

Break 3:
- File: `LogingService/src/main/java/edu/pict/loggingservice/service/DashboardStatsService.java`
- Problem:
  - handles stats retrieval, error fallback policy, throughput calculation, and p99 logic in one class.
- Fix:
  - separate into repository-facing query service + metrics calculation utility + caching facade.

## O — Open/Closed Principle (OCP)

Break 1:
- File: `MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java`
- Problem:
  - transformation from gRPC model to internal model is inlined in service flow; adding alternate history sources forces edits in same class.
- Fix:
  - introduce mapper interface and pluggable history source adapters.

Break 2:
- File: `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/PatternMatchStrategy.java`
- Problem:
  - static embedded malicious patterns mean updates require code changes/redeploy.
- Fix:
  - externalize signatures to config/rules provider and keep strategy closed for modification.

## L — Liskov Substitution Principle (LSP)

Break 1 (behavioral contract risk):
- File: `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/AiAnomalyStrategy.java`
- Problem:
  - strategy call can block for long periods while other strategies are fast/pure. This violates expected substitutability of `ThreatStrategy` from a caller timing perspective.
- Fix:
  - split strategy contracts:
    - `SynchronousThreatStrategy`
    - `AsynchronousThreatStrategy`
  - evaluate each in appropriate execution pipeline.

## I — Interface Segregation Principle (ISP)

Break 1:
- File: `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/ThreatStrategy.java` (used by all strategies)
- Problem:
  - single contract forces both fast deterministic and slow remote-dependent strategies into same interface behavior.
- Fix:
  - segregate into narrower interfaces:
    - `RuleBasedThreatStrategy`
    - `AiThreatStrategy`
    - optional marker for history requirement.

## D — Dependency Inversion Principle (DIP)

Break 1:
- File: `MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java`
- Problem:
  - depends directly on concrete `EventHistoryService` and `EnforcementService`.
- Fix:
  - depend on abstractions:
    - `HistoryProvider`
    - `BlockEnforcer`.

Break 2:
- File: `MCPService/src/main/java/edu/pict/mcpservice/service/AIClient.java`
- Problem:
  - concrete dependence on Feign transport details leaks into domain service behavior.
- Fix:
  - introduce `AnomalyScoringPort` interface; Feign/WebClient become infrastructure adapters.

Break 3:
- File: `ApiGateway/src/main/java/edu/pict/apigateway/config/SecurityConfig.java`
- Problem:
  - security policy is hardcoded directly in config class (`anyExchange().permitAll()`), making higher-level policy dependent on low-level implementation.
- Fix:
  - model policy in dedicated `AuthorizationPolicy` abstraction and apply through config.

---

## 3. SOLID-Aligned Target Module Structure

ApiGateway:
- `security.policy` (authorization + CSRF decisions)
- `request.context` (visitor/IP extraction and validation)
- `events.factory` (log/alert event composition)
- `events.publisher` (Kafka transport only)

MCPService:
- `analysis.core` (decision engine)
- `analysis.strategy.sync` (fast local rules)
- `analysis.strategy.async` (AI/remote rules)
- `analysis.history` (grpc/cache providers)
- `analysis.enforcement` (blocking port + redis adapter)

LoggingService:
- `ingestion` (Kafka to entity mapping)
- `history.api` (gRPC/REST adapters)
- `dashboard.query` (aggregates + cache facade)

---

## 4. Priority Mapping (Flaw -> SOLID-Driven Fix)

1. Secret leaks and hardcoded config -> DIP (externalized secure config) + SRP (separate secret loading responsibility).
2. `permitAll` and CSRF disable -> DIP (policy abstraction) + SRP (security policy isolated).
3. UUID-only blacklist and IP handling gaps -> SRP (context extraction), OCP (extensible block keys).
4. MCP redundant analysis + blocked-user reprocessing -> SRP (pipeline steps), ISP (strategy classes by behavior), LSP (sync/async split).
5. Missing gRPC server + client mismatch -> DIP (ports/adapters), SRP (transport adapter separation).
6. Dashboard heavy DB load -> SRP (cache facade), OCP (cache strategy extension).
7. `parallelStream` misuse and `record.toString()` metadata storage -> SRP + DIP (serialization abstraction).

---

## 5. Delivery Order for Implementation

1. Phase 0 + Phase 1 (security-critical).
2. Phase 2 + Phase 3 (pipeline correctness and stability).
3. Phase 4 (data/query efficiency and observability quality).
4. Phase 5 + Phase 6 (resilience baseline and proof documentation).

This order minimizes immediate breach risk first, then removes systemic runtime bottlenecks.
