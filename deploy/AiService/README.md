# AiService — Kubernetes Deployment

## Overview

The AiService provides AI-powered threat classification by proxying requests to a local Ollama LLM instance. It registers with Eureka for discovery by other microservices.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `ai-service`                         |
| External Port| `8082`                               |
| Internal Port| `8080`                               |
| Image        | `shrihari7396/ai-service:latest`     |

## Environment Variables

### From ConfigMap (`ai-service-config`)

| Key                                      | Value                                    | Description                              |
|------------------------------------------|------------------------------------------|------------------------------------------|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`   | `http://eureka-server:8761/eureka`       | Eureka service registry URL              |
| `OLLAMA_BASE_URL`                        | `http://host.minikube.internal:11434`    | Ollama LLM endpoint on host machine      |
| `SERVER_PORT`                            | `8080`                                   | Application HTTP port                    |

> **Note:** `OLLAMA_BASE_URL` points to the host machine's Ollama instance via `host.minikube.internal`. This requires Ollama to be running on the host at port `11434`.

## Infrastructure Dependencies

| Dependency | K8s Service     | Port | Used For              |
|------------|----------------|------|-----------------------|
| Eureka     | `eureka-server`| 8761 | Service registration  |
| Ollama     | Host machine   | 11434| LLM inference         |

## Application Usage

Other services can access the AiService via:

- **HTTP**: `http://ai-service:8082`
- **Eureka discovery**: `AI-SERVICE`

## Deploy

```bash
bash deploy/AiService/deploy/deploy.sh
```
