# McpService — Kubernetes Deployment

## Overview

The McpService (MCP Server) is the core threat analysis engine. It consumes Kafka events, performs rule-based and AI-driven threat detection, caches analysis state in Redis, and queries the LoggingService via gRPC for historical request data.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `mcp-server`                         |
| HTTP Port    | `8080`                               |
| Replicas     | `2`                                  |
| Image        | `shrihari7396/mcp-server:latest`     |

## Environment Variables

### From ConfigMap (`mcp-server-config`)

| Key                                      | Value                                    | Description                              |
|------------------------------------------|------------------------------------------|------------------------------------------|
| `SPRING_DATA_REDIS_HOST`                 | `redis`                                  | Redis host for caching / dedup guard     |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`         | `kafka-service:9092`                     | Kafka broker address                     |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`   | `http://eureka-server:8761/eureka`       | Eureka service registry URL              |
| `GRPC_CLIENT_LOGGING_SERVICE_ADDRESS`    | `discovery:///LOGGING-SERVICE`           | gRPC target for LoggingService via Eureka|
| `SERVER_PORT`                            | `8080`                                   | Application HTTP port                    |

## Infrastructure Dependencies

| Dependency      | K8s Service     | Port | Used For                      |
|-----------------|----------------|------|-------------------------------|
| Redis           | `redis`        | 6379 | Analysis cache / dedup guard  |
| Kafka           | `kafka-service`| 9092 | Event consumption             |
| Eureka          | `eureka-server`| 8761 | Service discovery             |
| LoggingService  | `logging-service`| 9090 | gRPC history queries        |

## Application Usage

Other services can access the McpService via:

- **HTTP**: `http://mcp-server:8080`
- **Eureka discovery**: `MCP-SERVER`

## Deploy

```bash
bash deploy/McpService/deploy/deploy.sh
```
