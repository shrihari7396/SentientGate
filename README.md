# SentientGate: AI-Powered Runtime Security for Cloud-Native Microservices

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
| Orchestration | Kubernetes (Minikube for local, any K8s cluster for production) |
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

## Kubernetes Deployment

SentientGate includes production-ready Kubernetes manifests under the `deploy/` directory. Each service has its own set of manifests organized as follows:

### Deploy Folder Structure

```text
deploy/
├── Infrastructure/
│   ├── kafka/deploy/        (config, deployment, service)
│   ├── postgres/deploy/     (config, secret, pvc, deployment, service)
│   ├── redis/deploy/        (deployment, service)
│   └── zookeeper/deploy/    (config, deployment, service)
├── AiService/deploy/        (config, deployment, service, hpa)
├── ApiGateWay/deploy/       (config, deployment, service, hpa)
├── EurekaServer/deploy/     (config, deployment, service)
├── LoggingService/deploy/   (config, secret, deployment, service, hpa)
├── McpService/deploy/       (config, deployment, service, hpa)
├── UI/deploy/               (deployment, service, hpa)
├── dummy/deploy/            (config, deployment, service, hpa)
└── deploy.sh                (automated full-stack deploy script)
```

### Manifest Types Per Service

| Manifest | Purpose |
|---|---|
| `config.yml` | ConfigMap with environment variables (URLs, ports, broker addresses) |
| `secret.yml` | Secret with base64-encoded credentials (database passwords) |
| `deployment.yml` | Deployment + Service definitions (container image, env injection, ports) |
| `hpa.yml` | HorizontalPodAutoscaler for CPU-based autoscaling |
| `pvc.yml` | PersistentVolumeClaim for stateful storage (PostgreSQL) |

### Deployment Order

The automated `deploy.sh` script deploys services in dependency order:

1. **PostgreSQL** — database storage
2. **Redis** — caching and TTL enforcement
3. **Zookeeper** — Kafka coordination
4. **Kafka** — event streaming (depends on Zookeeper)
5. **EurekaServer** — service discovery registry
6. **LoggingService** — log persistence and gRPC history (depends on Postgres, Kafka, Eureka)
7. **ApiGateway** — edge gateway (depends on Redis, Kafka, Eureka)
8. **MCPService** — security analysis brain (depends on Redis, Kafka, Eureka, LoggingService gRPC)
9. **AIService** — local LLM inference (depends on Eureka)
10. **DummyService** — test downstream service (depends on Eureka)
11. **SentinelUI** — monitoring dashboard

### Quick Start (Minikube)

Prerequisites:

- Minikube installed and running (`minikube start`)
- `kubectl` configured to use the Minikube context
- Docker images built and available (`./build_and_push_images.sh`)
- Ollama running on the host for AIService

Deploy all services:

```bash
chmod +x deploy/deploy.sh
./deploy/deploy.sh
```

Deploy a single service manually:

```bash
kubectl apply -f deploy/AiService/deploy/config.yml
kubectl apply -f deploy/AiService/deploy/deployment.yml
kubectl apply -f deploy/AiService/deploy/hpa.yml
```

Verify deployments:

```bash
kubectl get pods
kubectl get services
kubectl get hpa
```

Access services via Minikube:

```bash
minikube service api-gateway --url
minikube service sentinel-ui --url
```

### Service Exposure

| Service | Type | Cluster Port |
|---|---|---|
| `api-gateway` | LoadBalancer | `8079 → 8080` |
| `sentinel-ui` | LoadBalancer | `5173 → 80` |
| `eureka-server` | ClusterIP | `8761` |
| `ai-service` | ClusterIP | `8082 → 8080` |
| `logging-service` | ClusterIP | `8080 (HTTP)`, `9090 (gRPC)` |
| `mcp-server` | ClusterIP | `8080` |
| `kafka-service` | ClusterIP | `29092 (internal)`, `9092 (external)` |
| `postgres` | ClusterIP | `5432` |
| `redis` | ClusterIP | `6379` |
| `zookeeper` | ClusterIP | `2181` |

### Autoscaling

HPA is configured for application services with CPU-based scaling:

| Service | Min Replicas | Max Replicas | CPU Target |
|---|---|---|---|
| `ai-service` | 1 | 3 | 80% |
| `api-gateway` | 2 | 5 | 80% |
| `logging-service` | 1 | 3 | 80% |
| `mcp-server` | 2 | 5 | 80% |
| `sentinel-ui` | 1 | 3 | 80% |
| `dummy-service` | 1 | 3 | 80% |

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
├── deploy/
│   ├── Infrastructure/
│   │   ├── kafka/deploy/
│   │   ├── postgres/deploy/
│   │   ├── redis/deploy/
│   │   └── zookeeper/deploy/
│   ├── AiService/deploy/
│   ├── ApiGateWay/deploy/
│   ├── EurekaServer/deploy/
│   ├── LoggingService/deploy/
│   ├── McpService/deploy/
│   ├── UI/deploy/
│   ├── dummy/deploy/
│   └── deploy.sh
├── Architectures/
├── docker-compose.yml
├── build_and_push_images.sh
├── deploy.sh
├── run_tests.sh
└── README.md
```

## License

Apache 2.0. See `LICENSE`.
