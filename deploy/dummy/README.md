# Dummy Service — Kubernetes Deployment

## Overview

The Dummy Service is a test/placeholder microservice used for validating the deployment pipeline, Eureka registration, and API Gateway routing without any real business logic.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `dummy-service`                      |
| Port         | `8080`                               |
| Image        | `shrihari7396/dummy-service:latest`  |

## Environment Variables

### From ConfigMap (`dummy-service-config`)

| Key                                      | Value                                    | Description                          |
|------------------------------------------|------------------------------------------|--------------------------------------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`   | `http://eureka-server:8761/eureka`       | Eureka service registry URL          |
| `SERVER_PORT`                            | `8080`                                   | Application HTTP port                |

## Infrastructure Dependencies

| Dependency | K8s Service     | Port | Used For             |
|------------|----------------|------|----------------------|
| Eureka     | `eureka-server`| 8761 | Service registration |

## Application Usage

Other services can access the Dummy Service via:

- **HTTP**: `http://dummy-service:8080`
- **Eureka discovery**: `DUMMY-SERVICE`

## Deploy

```bash
bash deploy/dummy/deploy/deploy.sh
```
