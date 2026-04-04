# SentientGate Fix Implementation Tracker

Date: 2026-04-04
Status: Phase 0 + significant Phase 1/2/3/4 changes implemented.

## Implemented Changes

1. Secret and repository hardening
- Removed leaked key: `private_key.pem`
- Removed backup config with secrets: `ApiGateway/src/main/resources/application.yml.bak`
- Externalized secrets:
  - `ApiGateway/src/main/resources/application.yml`
  - `LogingService/src/main/resources/application.yml`
  - `docker-compose.yml`
- Hardened ignore rules in `.gitignore` (`*.pem`, `.env*`, build and IDE folders)

2. Gateway design-pattern refactor and security controls
- Added policy pattern:
  - `ApiGateway/src/main/java/edu/pict/apigateway/security/RouteAccessPolicy.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/security/StaticRouteAccessPolicy.java`
- Added factory/extractor pattern for event creation:
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContext.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/RequestContextExtractor.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultRequestContextExtractor.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/GatewayEventFactory.java`
  - `ApiGateway/src/main/java/edu/pict/apigateway/events/DefaultGatewayEventFactory.java`
- Updated filters:
  - `JwtExtractionFilter`: JWT required on protected routes
  - `SentientGateFilter`: removed `assert`, uses extractor+factory
  - `BlacklistFilter`: checks UUID and IP blacklist keys (legacy compatible)
- Improved proxy/IP handling:
  - `ApiGateway/src/main/java/edu/pict/apigateway/service/IpService.java`
- Updated blacklist management controller for namespaced UUID keys:
  - `ApiGateway/src/main/java/edu/pict/apigateway/controller/ManagementController.java`

3. MCP pipeline architecture and resilience improvements
- Added dependency inversion ports:
  - `MCPService/src/main/java/edu/pict/mcpservice/ports/HistoryProvider.java`
  - `MCPService/src/main/java/edu/pict/mcpservice/ports/BlockEnforcer.java`
  - `MCPService/src/main/java/edu/pict/mcpservice/ports/AnomalyScoringPort.java`
- Added async strategy segregation:
  - `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/AsyncThreatStrategy.java`
  - `MCPService/src/main/java/edu/pict/mcpservice/service/AsyncThreatEvaluator.java`
  - `MCPService/src/main/java/edu/pict/mcpservice/config/AsyncConfig.java`
- Updated analysis pipeline:
  - `MCPService/src/main/java/edu/pict/mcpservice/service/McpAnalysisService.java`
  - Added pre-block check, dedup window, sync/async strategy split
- Added Caffeine history cache + gRPC deadline:
  - `MCPService/src/main/java/edu/pict/mcpservice/service/EventHistoryService.java`
  - `MCPService/build.gradle`
- Improved enforcement serialization and key strategy:
  - `MCPService/src/main/java/edu/pict/mcpservice/service/EnforcementService.java`
  - Stores JSON metadata and writes UUID/IP blacklist keys
- Improved pattern matching coverage:
  - `MCPService/src/main/java/edu/pict/mcpservice/stratagies/blocking/PatternMatchStrategy.java`
- Batch listener behavior aligned:
  - `MCPService/src/main/java/edu/pict/mcpservice/kafkaListeners/SecurityEventListners.java`
- Feign endpoint correction and timeout config:
  - `MCPService/src/main/java/edu/pict/mcpservice/clients/AiServiceFeignClient.java`
  - `MCPService/src/main/resources/application.yml`

4. Logging service correctness and performance
- Added gRPC server implementation:
  - `LogingService/src/main/proto/user_log_event.proto`
  - `LogingService/src/main/java/edu/pict/loggingservice/grpc/UserLogEventGrpcService.java`
  - `LogingService/src/main/java/edu/pict/loggingservice/repository/GatewayLogRepository.java`
  - `LogingService/src/main/resources/application.yml`
  - `LogingService/build.gradle`
- Replaced `parallelStream()` with `stream()`:
  - `LogingService/src/main/java/edu/pict/loggingservice/service/KafkaBatchService.java`
- Added DB indexes on hot query paths:
  - `LogingService/src/main/java/edu/pict/loggingservice/entity/GatewayLogEntity.java`
- Hardened Kafka trusted packages and DDL config:
  - `LogingService/src/main/resources/application.yml`

5. AI request-contract compatibility and prompt-injection hardening
- Added history-compatible event DTO:
  - `AIService/src/main/java/edu/pict/dtos/BehaviorLogEvent.java`
- Extended request DTO for MCP payload shape:
  - `AIService/src/main/java/edu/pict/dtos/AnomalyDetectionRequest.java`
- Added feature derivation and route-sensitivity sanitization:
  - `AIService/src/main/java/edu/pict/service/AnomalyDetectionService.java`
- Added response aliasing support in MCP consumer:
  - `MCPService/src/main/java/edu/pict/mcpservice/model/AnomalyDetectionResponse.java`

## Verification Notes

Automated test runs in this execution environment were blocked by runtime/toolchain mismatch:
- Maven builds require Java 21 `--release`, but runner JDK is older.
- Gradle plugin resolution requires Java 17+, runner JDK is Java 11.

Commands attempted:
- `ApiGateway: ./mvnw -Dmaven.repo.local=/tmp/.m2-api test`
- `AIService: ./mvnw -Dmaven.repo.local=/tmp/.m2-ai test`
- `MCPService: GRADLE_USER_HOME=/tmp/.gradle-mcp ./gradlew test`
- `LogingService: GRADLE_USER_HOME=/tmp/.gradle-log ./gradlew test`

## Remaining Planned Items

- Integrate Spring Security authorization chain with JWT-authenticated principal (beyond header-level enforcement).
- Add circuit breaker policy (`Resilience4j`) around gRPC + AI calls.
- Add dashboard cache layer for aggregate queries.
- Add environment profile split for strict production defaults (`ddl-auto=validate`, TLS, service auth).
