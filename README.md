<div align="center">

# 🛡️ SentientGate

### AI-Driven Distributed Security Mesh for Microservices

[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-event--driven-231F20?style=for-the-badge&logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-reactive-DC382D?style=for-the-badge&logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-containerized-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

**SentientGate** is an intelligent, high-concurrency security infrastructure that protects microservice ecosystems from sophisticated bot attacks, injection attempts, and behavioral anomalies — using **real-time AI-driven threat analysis** powered by a local LLM (Ollama).

Unlike static firewalls, SentientGate's **Sentient MCP (Master Control Program)** analyzes user behavioral history using a layered Strategy pattern — from rule-based pattern matching to AI anomaly detection — all without any external API dependency.

[Architecture](#-system-architecture) · [Services](#-microservices) · [Tech Stack](#-technology-stack) · [Quick Start](#-quick-start) · [Docker Setup](#-docker-deployment) · [Folder Structure](#-project-structure)

</div>

---

## 🏗️ System Architecture

### High-Level Architecture

![SentientGate Architecture](Architectures/Sentigate_Architectural_Diagram.png)

The architecture follows a **distributed, event-driven microservice model** where detection, analysis, and enforcement are completely decoupled for maximum scalability and resilience.

### Request Lifecycle (Sequence Flow)

![SentientGate Sequence](Architectures/Sentigate_Sequence_Diagram.png)

A suspicious request flows through the following pipeline:

```
Incoming Request
      │
      ▼
 ApiGateway ──► Kafka Event Bus ──► MCPService
      │                                  │
      │                            ┌─────┴──────────┐
      │                        LoggingService   AIService
      │                        (via gRPC)    (Ollama LLM)
      │                                  │
      └──────── Redis Blacklist ◄─────────┘
               (TTL Enforcement)
```

---

## 🔬 Microservices

### 🔹 ApiGateway
> `ApiGateway/` · Spring Boot · Maven · Port `8079`

The **entry point** of the entire system.

- **Visitor Identity Signing** — generates and validates fingerprinted visitor tokens
- **Sub-millisecond Blacklist Enforcement** — checks Reactive Redis for blocked IPs/tokens instantly
- **Threat Event Publishing** — publishes `SecurityAlertEvent` to Kafka for async analysis
- **Reactive Architecture** — built on **Spring WebFlux** for non-blocking, high-throughput processing
- **Global Filters** — authentication, request validation, rate limiting applied as gateway filters

---

### 🔹 MCPService (Sentient Engine)
> `MCPService/` · Spring Boot · Gradle · The "Brain"

The **core decision-making engine** of SentientGate.

- Consumes `SecurityAlertEvent` from **Kafka**
- Fetches 10-minute behavioral history from LoggingService via **gRPC**
- Applies a **layered, Strategy-based threat analysis pipeline**:

| Strategy | Type | Detects |
|---|---|---|
| `PatternMatchStrategy` | Rule-Based | SQL Injection, XSS, Path Traversal |
| `BurstTrafficStrategy` | Heuristic | Burst traffic, high error rate, bot probing |
| `AiAnomalyStrategy` | AI-Driven | Behavioral entropy, non-human activity patterns |

- Applies **TTL-based dynamic blocking** via Redis for temporary bans (prevents false positives)

---

### 🔹 AIService
> `AIService/` · Spring Boot · Maven · Reactive

The **AI inference microservice**.

- Interfaces with a **local Ollama LLM** (`llama3` model)
- Performs deep behavioral anomaly detection on request patterns
- Called only for **high-complexity, edge-case threats** to avoid latency impact on the gateway
- Fully **reactive** using Spring WebFlux

---

### 🔹 LoggingService
> `LogingService/` · Spring Boot · Gradle · gRPC Server

The **memory and data layer** of the system.

- Records all gateway interaction logs to **PostgreSQL**
- Exposes **gRPC endpoints** for MCPService to fetch behavioral history
- Consumes gateway decision events from **Kafka**
- Powers entropy and pattern-based behavioral scoring
- Provides REST APIs for the **monitoring dashboard**

---

### 🔹 EurekaServer
> `EurekaServer/` · Spring Cloud · Gradle · Port `8761`

Netflix Eureka **service discovery** server.

- All microservices register here dynamically
- Enables **load-balanced, service-name-based** inter-service routing (used with gRPC and Feign)

---

### 🔹 DummyService
> `DummyService/` · Spring Boot · Gradle

A **mock protected backend service** that demonstrates the security mesh in action.

- Simulates a real downstream microservice behind the gateway
- Used for **end-to-end testing** of detection, blocking, and request forwarding behavior

---

### 🔹 UI — Sentinel Gateway Dashboard
> `UI/sentinel-gateway-ui/` · React · Vite · Tailwind CSS · Port `5173`

A real-time **security monitoring dashboard**.

- Live request logs and threat detection feed
- Gateway statistics and charts (`/dashboard`)
- Visualizes blocked IPs, threat reason breakdown, and traffic patterns
- Communicates with backend via the ApiGateway

---

## ⚡ Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Backend** | Java 21, Spring Boot 3.x | Core service runtime |
| **Reactive** | Spring WebFlux, Project Reactor | Non-blocking, high-throughput I/O |
| **Service Discovery** | Spring Cloud Netflix Eureka | Dynamic service registration |
| **Messaging** | Apache Kafka | Async, decoupled threat event pipeline |
| **Communication** | gRPC | Low-latency inter-service calls (MCP ↔ Logging) |
| **HTTP Client** | OpenFeign | REST-based service-to-service calls |
| **Cache / Blacklist** | Redis (Reactive) | Sub-millisecond TTL-based block enforcement |
| **Database** | PostgreSQL | Persistent request log storage |
| **AI Inference** | Ollama (`llama3`) | Local, privacy-first LLM anomaly detection |
| **Frontend** | React, Vite, Tailwind CSS | Security monitoring dashboard |
| **Containerization** | Docker, Docker Compose | Full-stack orchestration |
| **Build Tools** | Maven, Gradle | Per-service build management |

---

## 🔐 Security Design Principles

- **TTL-Based Blocking** — temporary bans prevent permanent false positives; auto-expire via Redis
- **Kafka Buffering** — security events are buffered, preventing system collapse under spike load
- **Decoupled AI** — AI inference happens asynchronously; it never adds latency to the gateway response
- **Strategy Pattern** — threat detection rules are modular and independently extensible
- **Stateless Services** — every service is designed for **horizontal scalability** with no shared in-memory state
- **Local AI** — Ollama runs fully on-premise — zero external API dependency, total data privacy

---

## 📁 Project Structure

```
SentientGate/
├── ApiGateway/                  # Spring Boot (Maven) — Entry point, filters, blacklist
│   └── src/main/java/
│       └── filters/             # Global filters: Auth, RateLimit, RequestValidation
│       └── services/            # Redis blacklist service, Kafka publisher
│
├── MCPService/                  # Spring Boot (Gradle) — Sentient threat analysis engine
│   └── src/main/java/
│       └── strategy/            # PatternMatch, BurstTraffic, AiAnomaly strategies
│       └── kafka/               # Security alert consumer
│       └── grpc/                # gRPC client to LoggingService
│
├── AIService/                   # Spring Boot (Maven) — Ollama LLM integration
│   └── src/main/java/
│       └── service/             # OllamaService — behavioral anomaly analysis
│
├── LogingService/               # Spring Boot (Gradle) — gRPC server, log persistence
│   └── src/main/java/
│       └── kafka/               # Gateway decision consumer
│       └── grpc/                # gRPC server — behavioral history provider
│       └── repository/          # PostgreSQL interaction log repository
│
├── EurekaServer/                # Spring Cloud Eureka — Service registry
│
├── DummyService/                # Spring Boot (Gradle) — Mock protected service
│
├── UI/
│   └── sentinel-gateway-ui/     # React + Vite + Tailwind — Monitoring dashboard
│       └── src/
│           ├── components/      # Dashboard, charts, log viewer components
│           └── pages/           # Route-based page views
│
├── Architectures/               # Architecture and sequence diagrams
├── docker-compose.yml           # Full-stack Docker Compose orchestration
├── push_images.sh               # Docker Hub image publish script
├── run_tests.sh                 # Automated test runner script
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.8+ |
| Gradle | 8+ |
| Docker & Docker Compose | Latest |
| Ollama | Latest |
| Node.js | 18+ (for UI dev only) |

---

## 🐳 Docker Deployment (Recommended)

The **fastest way** to run the full stack. All service images are pre-built and published to Docker Hub.

### 1. Pull and Start Ollama (required for AI inference)

```bash
# Install Ollama: https://ollama.com
ollama serve
ollama pull llama3
```

### 2. Start All Services with Docker Compose

```bash
git clone https://github.com/shrihari7396/SentientGate.git
cd SentientGate

docker compose up -d
```

Docker Compose will automatically start:

| Container | Image | Port |
|---|---|---|
| `sentient-postgres` | `postgres:16.2` | `5433` |
| `sentient-redis` | `redis:7.2.4` | `6379` |
| `sentient-kafka` | `confluentinc/cp-kafka:7.5.0` | `9092` |
| `sentient-zookeeper` | `confluentinc/cp-zookeeper:7.5.0` | — |
| `eureka-server` | `shrihari7396/eureka-server:latest` | `8761` |
| `api-gateway` | `shrihari7396/api-gateway:latest` | `8079` |
| `logging-service` | `shrihari7396/logging-service:latest` | — |
| `mcp-server` | `shrihari7396/mcp-server:latest` | — |
| `ai-service` | `shrihari7396/ai-service:latest` | — |
| `dummy-service` | `shrihari7396/dummy-service:latest` | — |
| `sentinel-ui` | `shrihari7396/sentinel-ui:latest` | `5173` |

### 3. Access the Dashboard

```
http://localhost:5173       → Sentinel Gateway Dashboard (UI)
http://localhost:8761       → Eureka Service Registry
http://localhost:8079       → API Gateway (entry point)
```

---

## 🛠️ Local Development Setup

### Step 1 — Start Infrastructure

```bash
# Start Kafka, Zookeeper, Redis, and PostgreSQL via Docker
docker compose up -d postgres redis zookeeper kafka
```

### Step 2 — Start Ollama

```bash
ollama serve
ollama pull llama3
```

### Step 3 — Start Services (Order Matters)

```bash
# 1. Service Registry
cd EurekaServer && ./gradlew bootRun

# 2. Data + Memory layer
cd LogingService && ./gradlew bootRun

# 3. AI inference
cd AIService && mvn spring-boot:run

# 4. Sentient brain
cd MCPService && ./gradlew bootRun

# 5. Entry point
cd ApiGateway && mvn spring-boot:run

# 6. Protected mock service
cd DummyService && ./gradlew bootRun

# 7. Dashboard UI
cd UI/sentinel-gateway-ui && npm install && npm run dev
```

### Step 4 — Run Tests

```bash
# Run all service tests
bash run_tests.sh
```

---

## 🧪 How It Works — Attack Scenario

1. A bot starts sending **rapid, repeated requests** to the ApiGateway
2. The gateway detects an anomaly and publishes a `SecurityAlertEvent` to **Kafka** (non-blocking)
3. **MCPService** consumes the event and fetches the visitor's **last 10 minutes of history** from LoggingService via gRPC
4. The **Strategy Engine** evaluates:
   - **Pattern match** → checks for SQL injection, XSS payloads
   - **Burst traffic heuristic** → detects abnormal request frequency
   - **AI anomaly** → asks Ollama LLM to evaluate behavioral entropy
5. If any strategy triggers, the visitor IP/token is **written to Redis** with a configurable TTL
6. **Future requests from this visitor** are blocked at the gateway level in **sub-millisecond time** via Redis lookup
7. The entire event is available in the **Sentinel Dashboard**

---

## 🏆 Key Highlights

- ⚡ Sub-millisecond blacklist enforcement using Reactive Redis
- 🧠 Fully local AI inference (Ollama) — no external API, no cost, full privacy
- 🔄 100% async, event-driven threat pipeline via Apache Kafka
- 🧩 Strategy Pattern–based modular detection engine (easily extendable)
- 🌐 Horizontally scalable — all services are stateless and service-discovery–enabled
- 🐳 One-command full-stack deployment via Docker Compose
- 📊 Real-time security monitoring dashboard

---

## 👨‍💻 Author

**Shrihari Kulkarni**
Computer Engineering Student

Specializing in:
- Distributed Systems & Microservice Architecture
- Computer Networking (OSI / TCP-IP)
- AI-Integrated Security Systems
- High-Concurrency & Reactive Programming

---

## 📜 License

This project is licensed under the **Apache License 2.0**.

You are free to use, modify, and distribute this software in accordance with the terms of the license.

See the [LICENSE](LICENSE) file for full details.
