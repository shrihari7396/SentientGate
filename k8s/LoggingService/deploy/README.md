# Logging Service Kubernetes Deployment Guide

This directory contains the Kubernetes manifests required to deploy the **LoggingService** to a production cluster. 

## Infrastructure Dependencies

Before deploying the Logging Service, ensure the following core infrastructure components are running and accessible within your Kubernetes cluster:
1. **PostgreSQL** (`postgres:5432`) - Used for durable, long-term log storage.
2. **Redis** (`redis:6379`) - Used for sub-millisecond caching of recent logs.
3. **Kafka** (`kafka-service:9092`) - The event stream from which logs are consumed.
4. **Eureka Server** (`eureka-server:8761`) - For service discovery.

## Manifest Overview

- `deployment.yml`: The main Deployment controlling the replica pods. Maps environment variables to the container.
- `config.yml`: The ConfigMap defining non-sensitive environment variables (e.g., URLs, ports).
- `secret.yml`: The Secret defining sensitive credentials (e.g., database passwords).
- `hpa.yml`: The Horizontal Pod Autoscaler for dynamic scaling based on CPU/Memory usage.

## Environment Variables

The `application-prod.yml` relies on the following environment variables. Ensure these are correctly mapped in your `config.yml` and `secret.yml`.

| Variable Name | Description | Default / Example Value |
| --- | --- | --- |
| `SERVER_PORT` | HTTP port for REST and Actuator health checks | `8080` |
| `GRPC_SERVER_PORT` | Port for incoming gRPC communication (e.g. from MCPService) | `9090` |
| `SPRING_DATASOURCE_URL` | JDBC connection string for PostgreSQL | `jdbc:postgresql://postgres:5432/mydb` |
| `SPRING_DATASOURCE_USERNAME`| Database username (Inject via Secret) | `postgres` |
| `SPRING_DATASOURCE_PASSWORD`| Database password (Inject via Secret) | `postgres` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers connection string | `kafka-service:9092` |
| `SPRING_DATA_REDIS_HOST` | Redis host | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis port | `6379` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL for the Eureka registry | `http://eureka-server:8761/eureka` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile. Must be set to `prod` | `prod` |

## Access Patterns

The LoggingService is exposed internally via a Kubernetes Service on two primary ports:
1. **Port 8080 (HTTP)**: Used primarily for Kubernetes Readiness/Liveness probes targeting `/actuator/health`, and occasionally for any REST aggregation controllers.
2. **Port 9090 (gRPC)**: The high-performance channel used by the `MCPService` to fetch historical logs.

## Deployment Instructions

To deploy the service, apply the manifests in the following order:

```bash
# 1. Apply Configuration and Secrets
kubectl apply -f config.yml
kubectl apply -f secret.yml

# 2. Deploy the Service
kubectl apply -f deployment.yml

# 3. Apply Autoscaling (Optional)
kubectl apply -f hpa.yml
```

Verify that the pods start successfully and pass their health checks:
```bash
kubectl get pods -l app=logging-service -w
```
