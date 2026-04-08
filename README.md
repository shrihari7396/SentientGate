# SentientGate

AI-Driven Security Mesh for Modern Microservices

SentientGate is a distributed security platform that sits in front of microservices, observes traffic in real time, detects suspicious behavior, and takes temporary enforcement actions before attacks spread.

## The Story

Most API security setups fail in one of two ways:

1. They are static and rule-only, so they miss evolving attacks.
2. They are powerful but expensive, slow, and hard to run privately.

SentientGate is built to solve that gap. It combines fast gateway enforcement with event-driven analysis, historical context, and local AI inference to make security decisions that are both fast and adaptive.

Instead of blocking forever, it applies TTL-based temporary blocks, learns from behavior, and keeps services available under load.

## What SentientGate Is

SentientGate is a microservice security fabric with these core capabilities:

- Real-time request filtering at the gateway edge
- Event-driven threat analysis with Kafka
- Behavioral history analysis via gRPC
- Layered detection with strategy-based scoring
- Dynamic temporary blocking via Redis TTL
- Optional local LLM anomaly checks using Ollama
- Operational visibility through a React dashboard

## How the System Works

Request journey:

1. A client request enters `ApiGateway`.
2. Gateway filters validate request context and check Redis blacklist state.
3. Security events are published to Kafka for asynchronous analysis.
4. `MCPService` consumes events and fetches recent user/IP behavior from `LogingService` through gRPC.
5. MCP applies layered strategies:
   - `PatternMatchStrategy` for signature-like payload threats
   - `BurstTrafficStrategy` for abusive traffic patterns
   - `AiAnomalyStrategy` for behavioral anomalies
6. If risk crosses threshold, MCP writes a TTL block record to Redis.
7. Next malicious requests are denied quickly at gateway level.
8. Logs and decision outcomes are visible to operators in the UI.

This keeps the hot request path fast while moving deeper intelligence to async services.

## Industry Impact

SentientGate targets practical impact in production environments:

- Financial services: reduces fraud and bot abuse blast radius with fast temporary bans
- E-commerce: protects checkout and login surfaces during traffic spikes and bot storms
- SaaS platforms: provides centralized protection for many internal services behind one gateway
- Regulated industries: enables privacy-first AI analysis using local models (no external LLM dependency)
- Platform engineering teams: improves resilience by decoupling detection, storage, and enforcement

Business-level outcomes:

- Lower incident response time
- Fewer successful automated attacks
- Better uptime during abusive traffic windows
- Stronger auditability of security decisions

## Architecture

High-level diagram:

![SentientGate Architecture](Architectures/Sentigate_Architectural_Diagram.png)

Sequence diagram:

![SentientGate Sequence](Architectures/Sentigate_Sequence_Diagram.png)

## Services

| Service | Purpose | Default Port |
|---|---|---|
| `ApiGateway` | Entry point, filtering, rate limiting, Redis enforcement | `8079` |
| `MCPService` | Security brain, strategy analysis, enforcement decisions | `9991` |
| `AIService` | Local LLM-based anomaly analysis via Ollama | `8082` |
| `LogingService` | Log persistence, gRPC behavior history, dashboard data | `8010` |
| `EurekaServer` | Service discovery registry | `8761` |
| `DummyService` | Protected downstream test service | `8090` |
| `sentinel-ui` | Monitoring dashboard | `5173` |

## Technology Used

| Layer | Tech |
|---|---|
| Language/Runtime | Java 21 |
| Frameworks | Spring Boot, Spring Cloud Gateway, Spring WebFlux |
| Messaging | Apache Kafka |
| Caching/Enforcement | Redis |
| Persistence | PostgreSQL |
| Service Discovery | Netflix Eureka |
| Inter-service RPC | gRPC |
| AI Inference | Ollama (`gemma3:latest` configured in `AIService`) |
| Frontend | React, Vite, Tailwind CSS |
| Containerization | Docker, Docker Compose |
| Build Tools | Maven and Gradle |

## Why This Design Matters

- Detection is decoupled from enforcement, so analysis can evolve without slowing the gateway.
- AI is local and optional, so teams keep data control and reduce vendor/API dependency.
- TTL blocks reduce false-positive damage compared with permanent bans.
- Strategy pattern allows easy extension for new threat heuristics.
- Event-driven architecture supports high-throughput and horizontal growth.

## Quick Start (Docker Compose)

Prerequisites:

- Docker and Docker Compose
- Ollama running locally (for AIService), default endpoint: `http://localhost:11434`
- Optional model pull: `ollama pull gemma3:latest`

Start all services:

```bash
docker compose up -d
```

Stop all services:

```bash
docker compose down
```

## Local Development

Each service is independently buildable:

- Maven services: `ApiGateway`, `AIService`
- Gradle services: `MCPService`, `LogingService`, `EurekaServer`, `DummyService`

Typical local order:

1. Start infrastructure: PostgreSQL, Redis, Kafka, Eureka
2. Start `LogingService` and `MCPService`
3. Start `AIService`
4. Start `DummyService`
5. Start `ApiGateway`
6. Start UI from `UI/sentinel-gateway-ui`

## Testing

Run multi-service tests:

```bash
./run_tests.sh
```

Run gateway tests separately:

```bash
cd ApiGateway
./mvnw test
```

## Project Structure

```text
SentientGate/
├── ApiGateway/
├── MCPService/
├── AIService/
├── LogingService/
├── EurekaServer/
├── DummyService/
├── UI/sentinel-gateway-ui/
├── Architectures/
├── docker-compose.yml
├── run_tests.sh
└── README.md
```

## Additional Documents

- `CURRENT_FLAWS_AND_VULNERABILITIES.md`
- `ARCHITECTURAL_DESIGN_FLAWS.md`
- `ARCHITECTURAL_SOLUTIONS.md`
- `IMPROVEMENT_AND_HARDENING_GUIDE.md`
- `FUTURE_README.md`
- `SECURITY.md`

## License

Apache 2.0. See `LICENSE`.
