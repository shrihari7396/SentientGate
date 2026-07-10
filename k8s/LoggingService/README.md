# LoggingService — Kubernetes Deployment

## Overview

The LoggingService is responsible for persisting API request/response audit logs. It consumes Kafka events and stores records in PostgreSQL. It also exposes a gRPC endpoint for synchronous log queries from the MCPService.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `logging-service`                    |
| HTTP Port    | `8080`                               |
| gRPC Port    | `9090`                               |
| Image        | `shrihari7396/logging-service:latest`|

## Environment Variables

### From ConfigMap (`logging-config`)

| Key                                      | Value                                    | Description                          |
|------------------------------------------|------------------------------------------|--------------------------------------|
| `SPRING_DATASOURCE_URL`                  | `jdbc:postgresql://postgres:5432/mydb`   | JDBC connection to PostgreSQL        |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`         | `kafka-service:9092`                     | Kafka broker address                 |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`   | `http://eureka-server:8761/eureka`       | Eureka service registry URL          |
| `SERVER_PORT`                            | `8080`                                   | Application HTTP port                |

### From Secret (`logging-secret`)

| Key                          | Decoded Value | Description           |
|------------------------------|---------------|-----------------------|
| `SPRING_DATASOURCE_USERNAME` | `postgres`    | PostgreSQL username   |
| `SPRING_DATASOURCE_PASSWORD` | `postgres`    | PostgreSQL password   |

## Infrastructure Dependencies

| Dependency | K8s Service  | Port | Used For              |
|------------|-------------|------|-----------------------|
| PostgreSQL | `postgres`  | 5432 | Audit log storage     |
| Kafka      | `kafka-service` | 9092 | Event consumption |
| Eureka     | `eureka-server` | 8761 | Service registration |

## Application Usage

Other services can access the LoggingService via:

- **HTTP**: `http://logging-service:8080`
- **gRPC**: `logging-service:9090`
- **Eureka discovery**: `discovery:///LOGGING-SERVICE`

## Deploy

```bash
bash deploy/LoggingService/deploy/deploy.sh
```
