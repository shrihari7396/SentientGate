# ApiGateway — Kubernetes Deployment

## Overview

The ApiGateway is the single entry point for all external HTTP requests. It performs rate limiting (via Redis), routes traffic to downstream microservices through Eureka service discovery, and publishes request audit events to Kafka.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `api-gateway`                        |
| External Port| `8079` (LoadBalancer)                |
| Internal Port| `8080`                               |
| Replicas     | `2`                                  |
| Image        | `shrihari7396/api-gateway:latest`    |

## Environment Variables

### From ConfigMap (`api-gateway-config`)

| Key                                      | Value                                    | Description                          |
|------------------------------------------|------------------------------------------|--------------------------------------|
| `SPRING_DATA_REDIS_HOST`                 | `redis`                                  | Redis host for rate limiting         |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`   | `http://eureka-server:8761/eureka`       | Eureka service registry URL          |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`         | `kafka-service:9092`                     | Kafka broker address                 |
| `SERVER_PORT`                            | `8080`                                   | Application HTTP port                |

## Infrastructure Dependencies

| Dependency | K8s Service     | Port | Used For              |
|------------|----------------|------|-----------------------|
| Redis      | `redis`        | 6379 | Rate limiting / cache |
| Kafka      | `kafka-service`| 9092 | Audit event publishing|
| Eureka     | `eureka-server`| 8761 | Service discovery     |

## Application Usage

External clients access the gateway via:

- **Within cluster**: `http://api-gateway:8079`
- **External (minikube)**: `minikube service api-gateway --url`

## Deploy

```bash
bash deploy/ApiGateWay/deploy/deploy.sh
```
