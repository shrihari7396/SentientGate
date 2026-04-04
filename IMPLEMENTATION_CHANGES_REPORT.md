# SentientGate Implementation Changes Report

Date: 2026-04-04
Prepared for: Current remediation sprint

## 1. Security and Secret Management

### Changes made
- Removed exposed private key:
  - `private_key.pem` (deleted)
- Removed backup config containing sensitive values:
  - `ApiGateway/src/main/resources/application.yml.bak` (deleted)
- Externalized secrets from source code:
  - `ApiGateway/src/main/resources/application.yml`
  - `LogingService/src/main/resources/application.yml`
  - `docker-compose.yml`
- Hardened repository ignore rules:
  - `.gitignore` updated with `*.pem`, `.env*`, build outputs, IDE folders, `application.yml.bak`.

### Impact
- Prevents secret leakage through Git.
- Moves runtime secrets to environment variables.

---

## 2. ApiGateway Refactor (SOLID + Patterns)

### New design components
- Policy Pattern:
  - `ApiGateway/src/main/java/edu/pict/apigateway/security/RouteAccessPolicy.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/security/StaticRouteAccessPolicy.java`
- Factory + Extractor Pattern:
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContext.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContextExtractor.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultRequestContextExtractor.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/GatewayEventFactory.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultGatewayEventFactory.java`

### Existing components updated
- `ApiGateway/src/main/java/edu/pict/apigateway/filters/global/JwtExtractionFilter.java`
  - Enforces JWT for protected routes using `RouteAccessPolicy`.
- `ApiGateway/src/main/java/edu/pict/apigateway/filters/global/SentientGateFilter.java`
  - Removed `assert`-based validation.
  - Delegates event creation to extractor/factory components.
- `ApiGateway/src/main/java/edu/pict/apigateway/filters/global/BlacklistFilter.java`
  - Added UUID + IP blacklist checks.
  - Kept legacy key compatibility.
- `ApiGateway/src/main/java/edu/pict/apigateway/service/IpService.java`
  - Improved trusted-proxy handling for `X-Forwarded-For` and `X-Real-IP`.
- `ApiGateway/src/main/java/edu/pict/apigateway/controller/ManagementController.java`
  - Moved to namespaced blacklist key strategy.

### Impact
- Better separation of responsibilities (SRP).
- Extensible route authorization policy (OCP).
- Stronger enforcement against UUID-only bypass.

---

## 3. MCPService Refactor (Pipeline, Ports, Async)

### New abstractions (DIP)
- `MCPService/src/main/java/edu/pict/mcpservice/ports/HistoryProvider.java`
- `MCPService/src/main/java/edu/pict/mcpservice/ports/BlockEnforcer.java`
- `MCPService/src/main/java/edu/pict/mcpservice/ports/AnomalyScoringPort.java`

### Async strategy support
- `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/AsyncThreatStrategy.java`
- `MCPService/src/main/java/edu/pict/mcpservice/service/AsyncThreatEvaluator.java`
- `MCPService/src/main/java/edu/pict/mcpservice/config/AsyncConfig.java`

### Core flow updates
- `MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java`
  - Added blocked-user pre-check.
  - Added dedup window.
  - Split sync and async strategy execution.
- `MCPService/src/main/java/edu/pict/mcpservice/service/EventHistoryService.java`
  - Added Caffeine cache with TTL.
  - Added gRPC deadline timeout.
- `MCPService/src/main/java/edu/pict/mcpservice/service/EnforcementService.java`
  - Stores JSON block metadata (not `toString()`).
  - Writes UUID/IP blacklist keys with legacy compatibility.
- `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/PatternMatchStrategy.java`
  - Extended detection inputs beyond only attempted path.
- `MCPService/src/main/java/edu/pict/mcpservice/kafkaListeners/SecurityEventListners.java`
  - Batch grouping and representative-event handling.

### AI integration fixes
- `MCPService/src/main/java/edu/pict/mcpservice/clients/AiServiceFeignClient.java`
  - Corrected service name and endpoint path.
- `MCPService/src/main/java/edu/pict/mcpservice/model/AnomalyDetectionResponse.java`
  - Added JSON aliases for MCP/AI response compatibility.
- `MCPService/src/main/resources/application.yml`
  - Restricted `trusted.packages`.
  - Added Feign timeout config.
- `MCPService/build.gradle`
  - Added Caffeine dependency.

### Impact
- Reduces redundant analysis and blocking overhead.
- Prevents consumer thread starvation from AI latency.
- Improves reliability and architecture boundaries.

---

## 4. LogingService Improvements

### gRPC history server implemented
- Added proto and generated-code build setup:
  - `LogingService/src/main/proto/user_log_event.proto`
  - `LogingService/build.gradle`
- Added server implementation:
  - `LogingService/src/main/java/edu/pict/loggingservice/grpc/UserLogEventGrpcService.java`
- Added repository support:
  - `LogingService/src/main/java/edu/pict/loggingservice/repository/GatewayLogRepository.java`
- Added gRPC/eureka metadata config:
  - `LogingService/src/main/resources/application.yml`

### Performance and data updates
- `LogingService/src/main/java/edu/pict/loggingservice/service/KafkaBatchService.java`
  - Replaced `parallelStream()` with `stream()`.
- `LogingService/src/main/java/edu/pict/loggingservice/entity/GatewayLogEntity.java`
  - Added DB indexes for hot query patterns.
- `LogingService/src/main/resources/application.yml`
  - Restricted Kafka trusted packages.
  - Externalized DB credentials and DDL strategy.

### Impact
- Enables history-dependent MCP strategies to work.
- Improves query and ingestion behavior under load.

---

## 5. AIService Contract and Prompt-Safety Updates

### Changes made
- Added MCP-compatible history DTO:
  - `AIService/src/main/java/edu/pict/dtos/BehaviorLogEvent.java`
- Extended request DTO compatibility:
  - `AIService/src/main/java/edu/pict/dtos/AnomalyDetectionRequest.java`
- Added feature derivation and input sanitization:
  - `AIService/src/main/java/edu/pict/service/AnomalyDetectionService.java`

### Impact
- Resolves request-shape mismatch between MCP and AI service.
- Reduces prompt manipulation risk by sanitizing route sensitivity input.

---

## 6. Validation Status

### Build/test execution attempt
- Commands were executed for all services.
- Full validation was blocked by local runner toolchain mismatch:
  - Maven/Gradle builds require Java 17/21.
  - Current execution runner provides Java 11.

### What this means
- Code changes are implemented and organized.
- Final compile/test confirmation should be run in your local JDK17/21 setup.

---

## 7. Files Added (new)

- `SYSTEM_REMEDIATION_PLAN_SOLID.md`
- `FIX_IMPLEMENTATION_TRACKER.md`
- `IMPLEMENTATION_CHANGES_REPORT.md`
- `ApiGateway/src/main/java/edu/pict/apigateway/security/RouteAccessPolicy.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/security/StaticRouteAccessPolicy.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContext.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContextExtractor.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultRequestContextExtractor.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/events/GatewayEventFactory.java`
- `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultGatewayEventFactory.java`
- `MCPService/src/main/java/edu/pict/mcpservice/ports/HistoryProvider.java`
- `MCPService/src/main/java/edu/pict/mcpservice/ports/BlockEnforcer.java`
- `MCPService/src/main/java/edu/pict/mcpservice/ports/AnomalyScoringPort.java`
- `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/AsyncThreatStrategy.java`
- `MCPService/src/main/java/edu/pict/mcpservice/config/AsyncConfig.java`
- `MCPService/src/main/java/edu/pict/mcpservice/service/AsyncThreatEvaluator.java`
- `LogingService/src/main/proto/user_log_event.proto`
- `LogingService/src/main/java/edu/pict/loggingservice/grpc/UserLogEventGrpcService.java`
- `AIService/src/main/java/edu/pict/dtos/BehaviorLogEvent.java`

