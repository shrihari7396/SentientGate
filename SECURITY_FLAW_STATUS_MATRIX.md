# SentientGate Security Flaw Status Matrix (31 Items)

Date: 2026-04-04  
Legend: `Fixed` = implemented, `Partial` = improved but incomplete, `Pending` = not implemented yet.

| ID | Flaw | Status | Evidence | Next Action |
|---|---|---|---|---|
| 1 | Private key committed | Fixed | `private_key.pem` deleted | Rotate key material in all deployed environments |
| 2 | Hardcoded HMAC/JWT secrets | Fixed | `ApiGateway/src/main/resources/application.yml` now uses env vars | Enforce secret manager in non-local env |
| 3 | CSRF disabled | Pending | `ApiGateway/src/main/java/edu/pict/apigateway/config/SecurityConfig.java` still disables CSRF | Implement explicit CSRF/session policy |
| 4 | `permitAll()` on all routes | Partial | `JwtExtractionFilter` now protects paths, but `SecurityConfig` still has `anyExchange().permitAll()` | Move auth enforcement into Spring Security chain |
| 5 | Kafka trusted packages `*` | Fixed | `MCPService` + `LogingService` `application.yml` now restricted | Add tests for deserializer hardening |
| 6 | Hardcoded DB creds | Partial | `LogingService` uses env vars; compose still weak defaults pattern | Remove weak defaults and enforce required secrets |
| 7 | UUID-only blacklist | Fixed | `BlacklistFilter` and `EnforcementService` now use UUID + IP keys | Add ops tooling for IP unblock workflows |
| 8 | No IP spoofing protection | Partial | `IpService` now validates trusted proxy context | Add configurable trusted proxy CIDR chain |
| 9 | Pattern match path-only | Partial | `PatternMatchStrategy` now checks broader fields | Add body/header inspection stage upstream |
| 10 | Prompt injection risk | Partial | `AnomalyDetectionService` sanitizes route sensitivity | Add strict prompt template + allowlist guards |
| 11 | gRPC plaintext | Pending | `MCPService application.yml` still `negotiation-type: plaintext` | Enable TLS/mTLS for gRPC |
| 12 | No inter-service auth | Pending | No service auth layer present | Add mTLS/API-token/JWT between services |
| 13 | `ddl-auto: update` risk | Partial | Now env-driven in `LogingService application.yml` | Set production default to `validate` + migrations |
| 14 | Missing `.gitignore` protections | Partial | `.gitignore` significantly hardened | Finalize tracked-file policy for sensitive configs |
| 15 | Redis fire-and-forget block writes | Partial | JSON serialization + error callback improved | Add reliable write/ack strategy and retries/bulkhead |
| 16 | `assert` in production path | Fixed | Removed in `SentientGateFilter` flow | Add static analysis rule to block future assertions |
| 17 | AI path mismatch Feign vs controller | Fixed | `AiServiceFeignClient` now calls `/ai-service/anomaly/analyze` | Add integration test for MCP->AI call |
| 18 | `findFirst()` first-match only | Pending | Strategy arbitration still first-match | Introduce severity-based strategy arbitration |
| 19 | Burst strategy evasion | Pending | Existing threshold logic unchanged | Redesign burst detection with sliding windows |
| 20 | No request body size limits | Pending | No global request-size guards configured | Add gateway and service request-size limits |
| 21 | Restrictive CORS | Pending | CORS still hardcoded localhost in `SecurityConfig` | Make CORS env/profile driven |
| 22 | No Kafka DLQ | Pending | No DLQ/retry topic wiring | Add error handler + DLQ topic |
| 23 | Unsanitized DB text storage | Pending | Raw text fields remain | Sanitize/encode before render + constrain fields |
| 24 | Missing healthchecks in compose | Pending | Most services lack healthchecks | Add healthchecks for redis/kafka/services |
| 25 | Log rotation missing | Pending | No rotation policy configured | Add rolling appender/container log limits |
| 26 | `stratagies` typo package | Pending | Path remains unchanged | Rename package to `strategies` safely |
| 27 | `LogingService` typo name | Pending | Module name/path still typo | Rename module and references |
| 28 | Placeholder `SECURITY.md` | Pending | File still template-level | Write real disclosure/support policy |
| 29 | `show-sql: true` in LoggingService | Pending | Still enabled in `LogingService application.yml` | Disable in production profile |
| 30 | No compose network isolation | Pending | Default shared network only | Add segmented docker networks |
| 31 | No graceful shutdown config | Pending | No explicit graceful shutdown tuning | Enable graceful shutdown + timeout settings |

## Totals
- Fixed: 6
- Partial: 8
- Pending: 17

