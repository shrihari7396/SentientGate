# EurekaServer — Kubernetes Deployment

## Overview

The EurekaServer is the Netflix Eureka service registry that enables service discovery across all SentientGate microservices. All application services register with Eureka on startup.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `eureka-server`                      |
| Port         | `8761`                               |
| Image        | `shrihari7396/eureka-server:latest`  |

## Environment Variables

### From ConfigMap (`eureka-server-config`)

| Key                        | Value           | Description                              |
|----------------------------|-----------------|------------------------------------------|
| `SPRING_PROFILES_ACTIVE`   | `docker`        | Activates the Docker/K8s Spring profile  |
| `EUREKA_INSTANCE_HOSTNAME` | `eureka-server` | Hostname matching the K8s service name   |

## Infrastructure Dependencies

None — EurekaServer is a standalone service with no infrastructure dependencies. It should be deployed **first** before any other application service.

## Application Usage

All microservices register with Eureka using:

```
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka
```

The Eureka dashboard is accessible at:

- **Within cluster**: `http://eureka-server:8761`
- **Port-forward**: `kubectl port-forward svc/eureka-server 8761:8761`

## Deploy

```bash
bash deploy/EurekaServer/deploy/deploy.sh
```
