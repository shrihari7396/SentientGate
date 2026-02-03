
---

# 🚦 SentientGate – Reactive API Gateway

**SentientGate** is a **reactive, high-performance API Gateway** built using **Spring Cloud Gateway (WebFlux)** to protect backend microservices from abusive traffic before it reaches business logic.

The gateway focuses purely on **fast, deterministic enforcement** and remains **stateless, non-blocking, and highly scalable**.

---

## 🎯 Why SentientGate

Backend services should not spend CPU, threads, and database connections handling:

* Traffic floods
* Brute-force attempts
* Bot-driven API abuse
* Known malicious sources

**SentientGate enforces security at the edge**, where decisions are cheaper and faster.

---

## 🧠 Design Principles

* **Reactive by Default**
  Built on Netty + Reactor to handle high concurrency without thread exhaustion.

* **Deterministic Enforcement**
  Fixed rules only (rate limits, blacklists, token validity).
  No heuristics or ML inside the gateway.

* **Fail-Safe Behavior**
  Gateway availability is never compromised by Redis or Kafka failures.

* **Separation of Concerns**
  The gateway enforces rules; it does not contain business or intelligence logic.

---

## 🏗️ Gateway Architecture

```
Client
  ↓
SentientGate
  ├─ Client Identity Resolution
  ├─ IP Blacklisting (Redis)
  ├─ Rate Limiting (Redis + Lua)
  ├─ JWT Validation (Route-specific)
  ├─ Decision Logging (Async)
  ↓
Backend Services
```

---

## 🧩 Filter Chain (Execution Order)

### 🌍 Global Filters (Pre-Routing)

1. **ClientIdentityFilter**
   Resolves the real client IP from headers and connection metadata.

2. **BlacklistFilter**
   Blocks requests from known malicious IPs using Redis.

3. **RateLimitFilter**
   Enforces per-IP, per-route quotas using Redis and atomic Lua scripts.

---

### 🎯 Route-Specific Filter

4. **JWTValidationFilter**
   Validates JWT signature, expiry, and structure for protected routes only.

---

### 🌍 Global Filter (Post-Routing)

5. **DecisionLoggingFilter**
   Records enforcement decisions asynchronously without blocking traffic.

---

## 🔐 Threats Handled

* Traffic flooding & retry storms
* Distributed brute-force attempts
* Bot-driven API abuse
* Reconnaissance & endpoint scanning
* Known malicious IP sources
* Invalid or forged JWTs

---

## ⚙️ Technology Stack

* **Java 21**
* **Spring Boot 3**
* **Spring Cloud Gateway (WebFlux)**
* **Project Reactor**
* **Netty**
* **Redis (Reactive + Lua)**
* **JWT (OAuth2 Resource Server)**

---

## 🚫 Out of Scope (Intentional)

SentientGate does **not**:

* Perform authorization (roles/permissions)
* Inspect request bodies
* Execute ML or anomaly detection
* Access databases
* Perform blocking operations

These concerns are handled outside the gateway.

---

## 🧠 Reactive Execution Model

* Requests are processed as **reactive pipelines**, not thread-bound tasks.
* Event-loop threads execute callbacks and are never blocked.
* Request state is stored in:

    * `ServerWebExchange`
    * Reactive operator closures
* End-to-end latency is measured at pipeline termination.

---

## 🛡️ Failure Behavior

| Component         | Behavior                    |
| ----------------- | --------------------------- |
| Redis unavailable | Fail-open (traffic allowed) |
| JWT invalid       | Request rejected            |
| Gateway overload  | Backend protected           |

---

## 🚀 Running Locally

```bash
# Start Redis
redis-server

# Run the gateway
./mvnw spring-boot:run
```

---

## 🧠 One-Line Summary

> **SentientGate is a reactive API gateway that enforces deterministic security controls at the edge, protecting backend services while remaining fast, stateless, and highly scalable.**

---
