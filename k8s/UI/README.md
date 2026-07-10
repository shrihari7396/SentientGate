# Sentinel UI — Kubernetes Deployment

## Overview

The Sentinel UI is the frontend dashboard for the SentientGate platform. It is a static web application served by Nginx and communicates with backend services exclusively through the API Gateway.

## K8s Service

| Property     | Value                                |
|--------------|--------------------------------------|
| Service Name | `sentinel-ui`                        |
| External Port| `5173` (LoadBalancer)                |
| Internal Port| `80` (Nginx)                         |
| Image        | `shrihari7396/sentinel-ui:latest`    |

## Environment Variables

None — The UI is a static frontend with no runtime environment configuration. API endpoints are configured at build time.

## Infrastructure Dependencies

| Dependency  | K8s Service   | Port | Used For             |
|-------------|--------------|------|----------------------|
| API Gateway | `api-gateway`| 8079 | All backend API calls|

## Application Usage

Access the UI via:

- **External (minikube)**: `minikube service sentinel-ui --url`
- **Port-forward**: `kubectl port-forward svc/sentinel-ui 5173:5173`

## Deploy

```bash
bash deploy/UI/deploy/deploy.sh
```
