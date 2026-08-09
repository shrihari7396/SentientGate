# ApiGateway

The **ApiGateway** service acts as the main entry point and reverse proxy for the SentientGate microservices architecture. It is built using **Spring Cloud Gateway (WebFlux)**.

## Core Responsibilities
1. **Request Routing:** Maps incoming traffic to downstream services (e.g. `USER-SERVICE`, `LOGGING-SERVICE`, `MCP-SERVER`, `AI-SERVICE`) via Eureka Server service discovery.
2. **Rate Limiting:** Implements a Redis-backed request rate limiter for API protection.
3. **Security & Threat Prevention:**
   - Evaluates incoming requests against a Redis threat blacklist.
   - Handles global JWT validation and blocks revoked JWT tokens.
4. **Resilience:** Implements fail-open logic for Redis operations, allowing the gateway to gracefully degrade and permit requests if the Redis cache is temporarily down.

## Configuration Profiles
The service provides multiple Spring configuration profiles:
- **`application.yml`**: The default configuration (usually for local development). Expects services like Redis and Eureka on `localhost`.
- **`application-prod.yml`**: The production configuration. It externalizes connection URLs and secrets to Environment Variables to be injected securely via Kubernetes manifests.

## Key Environment Variables (Production)
When running with the `prod` profile (`SPRING_PROFILES_ACTIVE=prod`), the following environment variables are expected:

| Variable | Description | Default (if omitted) |
|----------|-------------|----------------------|
| `REDIS_HOST` | Hostname for the Redis server. | `redis` |
| `REDIS_PORT` | Port for the Redis server. | `6379` |
| `REDIS_PASSWORD` | Password for Redis (if applicable). | (empty) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker endpoints. | `kafka:9092` |
| `EUREKA_URL` | Eureka Service Discovery URL. | `http://eureka-server:8761/eureka` |
| `JWT_SECRET_KEY` | **(Required)** Secret for signing/validating JWTs. | None |
| `SENTINEL_SECRET_KEY` | **(Required)** Internal security key. | None |

## Getting Started

### Local Development
To run the service locally using Maven:
```bash
./mvnw spring-boot:run
```
Make sure you have Redis running on `localhost:6379` and Eureka Server on `localhost:8761`.

### Running Tests
To execute the unit tests (which include comprehensive WebFlux and Mockito tests covering edge cases and fail-open logic):
```bash
./mvnw clean test
```
