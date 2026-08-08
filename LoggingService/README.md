# SentientGate Logging Service

The **Logging Service** is a high-performance, event-driven microservice within the SentientGate ecosystem. It is responsible for ingesting, caching, persisting, and retrieving gateway decision logs. 

To ensure extreme low-latency reads for security threat analysis (via the `MCPService`), the service leverages a dual-write architecture backed by Kafka, Redis, and PostgreSQL, completely avoiding race conditions between fast ingestion and immediate data retrieval.

---

## 🏗️ Architecture & Data Flow

The service utilizes two independent Kafka consumer groups listening to the same `USER_LOGS` topic, along with a Strategy Pattern for intelligent data retrieval.

```mermaid
graph TD
    subgraph Ingestion Pipeline
        K[Kafka: USER_LOGS Topic] -->|Group: logging-redis-writer| RC[RedisLogConsumer]
        K -->|Group: logging-db-writer| DC[DatabaseLogConsumer]
        
        RC -->|Sub-millisecond writes| Redis[(Redis Cache)]
        DC -->|Batch Inserts| DB[(PostgreSQL)]
    end

    subgraph Retrieval Pipeline (gRPC)
        MCP[MCP Service] -->|getUserEvents| GRPC[UserLogEventGrpcService]
        GRPC --> Resolver{LogFetchStrategyResolver}
        
        Resolver -->|1. Try Fast Path| RedisStrategy[RedisFetchStrategy]
        RedisStrategy -.->|Cache Hit| Redis
        
        Resolver -->|2. Fallback on Miss| DBStrategy[DatabaseFetchStrategy]
        DBStrategy -.->|Query Database| DB
    end
```

### Key Architectural Decisions
1. **Race Condition Mitigation**: By utilizing separate consumer groups (`logging-redis-writer` and `logging-db-writer`), the service caches data to Redis almost instantly (~1ms), while the database batch inserts can take their time without blocking immediate gRPC reads.
2. **Strategy Pattern Fallback**: When fetching logs, the system always queries Redis first. If the cache expires (10-minute sliding TTL) or misses, it elegantly falls back to PostgreSQL, which acts as the durable source of truth.

---

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.5.x** (Web, Data JPA, Data Redis)
- **Spring Kafka** (Event-driven ingestion)
- **gRPC / Protobuf** (High-speed internal communication)
- **PostgreSQL** (Durable storage)
- **Redis** (In-memory caching)
- **Netflix Eureka** (Service Discovery)

---

## ⚙️ Configuration & Environment Variables

For production deployment, this service relies heavily on environment variables mapping to `application-prod.yml`.

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `SERVER_PORT` | HTTP port for REST and Actuator health checks | `8080` |
| `GRPC_SERVER_PORT` | Port for incoming gRPC requests | `9090` |
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL | `jdbc:postgresql://postgres:5432/mydb` |
| `SPRING_DATASOURCE_USERNAME` | Postgres Username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Postgres Password | `postgres` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka Brokers | `kafka-service:9092` |
| `SPRING_DATA_REDIS_HOST` | Redis Hostname | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis Port | `6379` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka Registry URL | `http://eureka-server:8761/eureka` |
| `SPRING_PROFILES_ACTIVE` | Must be set to `prod` for Kubernetes deployments | `prod` |

---

## 🚀 Getting Started Locally

### Prerequisites
You must have the SentientGate infrastructure running locally (Kafka on port 10000, Postgres on 5432, Redis on 6379, and Eureka on 8761).

### Running the Application
Use the Gradle wrapper to spin up the application:

```bash
./gradlew bootRun
```

### Running Tests
The service has a comprehensive unit test suite covering consumers, strategies, caching, and gRPC mapping. To run them:

```bash
./gradlew test
```
*Test reports will be generated in `build/reports/tests/test/index.html`.*

---

## 🐳 Docker & Kubernetes

The application is containerized using a multi-stage Docker build to ensure a lean production image.

**Build the Image:**
```bash
docker build -t sentientgate/logging-service:latest .
```

For Kubernetes deployments, please refer to the deployment documentation located at `k8s/LoggingService/deploy/README.md` for specific manifest instructions and secrets management.
