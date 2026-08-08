# MCPService Deployment Guide

This document outlines the required configuration, environment variables, and external dependencies needed to run `MCPService` in a production Kubernetes environment.

## 1. External Dependencies

`MCPService` relies on the following infrastructure components. Ensure these are deployed and accessible from the pod:

*   **Apache Kafka**: To ingest the `security-events` topic.
*   **Redis**: To manage distributed state for rate limiting, deduplication, and blacklists.
*   **Eureka Service Registry**: For dynamic service discovery.
*   **LOGGING-SERVICE (gRPC)**: To fetch historical user logs for contextual threat analysis.
*   **AI-SERVICE**: For behavioral anomaly detection via Feign HTTP client.

## 2. Environment Variables

The `application-prod.yml` enforces strict variable binding to prevent accidental fallbacks to `localhost`. The following environment variables **must** be provided in your Kubernetes deployment manifest:

| Environment Variable | Description | Example Value | Required? |
| :--- | :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Activates the production profile. This is already baked into the Dockerfile. | `prod` | Yes |
| `SERVER_PORT` | The HTTP port the container binds to. Defaulted in Dockerfile to 8080. | `8080` | Yes |
| `SPRING_DATA_REDIS_HOST` | The DNS name or IP of the Redis master/cluster. | `redis-cluster.default.svc.cluster.local` | **Yes** |
| `SPRING_DATA_REDIS_PORT` | The Redis connection port. | `6379` | **Yes** |
| `SPRING_DATA_REDIS_PASSWORD` | The Redis authentication password. | *(Map from a Kubernetes Secret)* | No (if auth is disabled) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | The Kafka broker endpoints. | `kafka-broker.default.svc.cluster.local:9092` | **Yes** |
| `SPRING_KAFKA_CONSUMER_GROUP_ID` | The Kafka consumer group. Defaults to `sentientgate-mcp-service`. | `sentientgate-mcp-service` | No |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | The full Eureka registration URL. | `http://eureka-server.default.svc.cluster.local:8761/eureka` | **Yes** |
| `HOSTNAME` | The host identity for Eureka registration. Kubernetes maps this automatically. | Automatically injected by K8s | Auto |

## 3. Kubernetes Deployment Recommendations

When writing your `deployment.yaml` and `service.yaml`, consider the following best practices:

### 3.1. Secrets and ConfigMaps

*   Store credentials (like `SPRING_DATA_REDIS_PASSWORD`) in a Kubernetes `Secret` and inject them as environment variables.
*   Store connection URIs (Kafka, Redis hosts, Eureka) in a `ConfigMap`.

### 3.2. Probes (Health Checks)

The production configuration enables Actuator health probes. You should configure Kubernetes to use these endpoints to manage pod lifecycles:

*   **Liveness Probe**: `GET /mcp-service/actuator/health/liveness` (Port: 8080)
*   **Readiness Probe**: `GET /mcp-service/actuator/health/readiness` (Port: 8080)

### 3.3. Security Context

The updated Dockerfile runs the application as a non-root user (`appuser`). To enforce this at the cluster level, define a `securityContext` in your Pod specification:

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000 # Map to appuser ID
```

### 3.4. Resource Limits

`MCPService` utilizes dedicated `ThreadPoolTaskExecutors` for I/O and CPU-bound tasks. Allocate sufficient resources to prevent CPU throttling during high Kafka event ingestion or AI inference spikes:

```yaml
resources:
  requests:
    cpu: "500m"
    memory: "512Mi"
  limits:
    cpu: "1000m"
    memory: "1Gi"
```
