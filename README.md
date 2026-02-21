# 🛡️ SentientGate: AI-Driven Distributed Security Mesh

**SentientGate** is an intelligent, high-concurrency security infrastructure designed to protect microservices from sophisticated bot attacks, injection attempts, and behavioral anomalies.

Unlike traditional firewalls that rely on static rules, SentientGate uses a **"Sentient" MCP (Master Control Program)** to analyze user intent in real-time using behavioral history and local LLM inference.

---

# 🏗️ Core Services Architecture

The system is composed of multiple specialized services, each responsible for a critical part of the security lifecycle:

## 🔹 ApiGateway
- Entry point of the system
- Handles Visitor Identity signing
- Performs high-speed **Blacklist Enforcement**
- Uses **Reactive Redis** for sub-millisecond checks

## 🔹 MCPService (Sentient Service)
- The brain of the system
- Consumes security alerts from **Kafka**
- Fetches 10-minute behavioral history via **gRPC**
- Executes a **Strategy-based analysis engine**

## 🔹 AIService
- Reactive Spring Boot service
- Interfaces with local **Ollama LLM**
- Performs deep behavioral anomaly detection

## 🔹 LoggingService
- The memory of the system
- Records interaction logs
- Provides historical data to MCP via **gRPC**

## 🔹 EurekaServer
- Service discovery for dynamic microservice communication

## 🔹 DummyService
- Protected target service used for demonstrating the security mesh

---

# 🚀 Key Features & Innovations

## 🧠 Sentient Analysis Engine

Implemented using the **Strategy Design Pattern**, the MCP applies a layered defense:

1. **Rule-Based Detection**
   - SQL Injection
   - XSS
   - Path Traversal
   - (`PatternMatchStrategy`)

2. **Heuristic Detection**
   - Burst traffic analysis
   - High error-rate scanning
   - (`BurstTrafficStrategy`)

3. **AI-Driven Detection**
   - Behavioral entropy analysis
   - Local LLM inference using **Ollama**
   - Identifies non-human activity patterns
   - (`AiAnomalyStrategy`)

---

# ⚡ High-Performance Tech Stack

## Event-Driven Security
- **Apache Kafka**
- Non-blocking threat reporting
- Decoupled detection pipeline

## Reactive Programming
- **Spring WebFlux**
- **Reactive Redis**
- Backpressure-enabled architecture

## Internal Communication
- **gRPC** for low-latency service-to-service communication
- **OpenFeign** for REST-based interactions

## Local AI Inference
- **Ollama**
- Privacy-focused
- Cost-free local LLM processing

---

# 🔄 System Flow

1. **Gateway Detection**
   - Suspicious request triggers a `SecurityAlertEvent`
   - Event is sent to **Kafka**

2. **MCP Consumption**
   - `MCPService` consumes the alert
   - Requests last 10 minutes of logs from `LoggingService` via **gRPC**

3. **Intelligence Evaluation**
   - Logs pass through the Strategy Chain
   - Complex cases are forwarded to `AIService`

4. **Threat Confirmation**
   - If malicious behavior is detected:
     - A `BlockRecord` with TTL is written to **Redis**

5. **Instant Enforcement**
   - `BlacklistFilter` in Gateway blocks future requests instantly

---

# 🛠️ Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 3.x |
| Cloud | Spring Cloud, Eureka |
| Messaging | Apache Kafka |
| Communication | gRPC, OpenFeign |
| Databases | Redis (Reactive), MySQL, MongoDB |
| AI | Ollama (Local LLM) |
| Infrastructure | Docker |

---

# 🧪 Installation & Setup

## 1️⃣ Prerequisites

- Java 21
- Maven
- Docker
- Apache Kafka
- Redis
- Ollama

## 2️⃣ Start Ollama

```bash
ollama serve
ollama pull llama3
```

## 3️⃣ Build the Project

```bash
mvn clean install
```

## 4️⃣ Start Services (Order Matters)

1. EurekaServer  
2. LoggingService  
3. AIService  
4. MCPService  
5. ApiGateway  
6. DummyService  

---

# 🏆 Highlights

- Fully asynchronous event-driven security mesh
- Local AI inference (no external API dependency)
- Sub-millisecond blacklist checks
- Strategy pattern–based modular detection engine
- Horizontally scalable microservice design

---

# 👨‍💻 Author

**Shrihari Kulkarni**  
Computer Engineering Student  
Specializing in:
- Computer Networking (OSI / TCP-IP)
- Distributed Systems
- AI-Integrated Security Architectures
- High-Concurrency Microservices Design

---

# 📜 License

This project is for educational and research purposes.
