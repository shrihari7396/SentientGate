# Kubernetes & Observability Setup Guide

This document explains the recent configuration changes made to the SentientGate microservices and Kubernetes manifests to ensure stability, proper routing, and robust observability.

## 1. Spring Boot Actuator & Prometheus Integration

To enable health monitoring and metric scraping in Kubernetes, all microservices have been configured with Spring Boot Actuator and Micrometer Prometheus.

### What Changed:
* **Dependencies**: Added `io.micrometer:micrometer-registry-prometheus` to all `pom.xml` and `build.gradle` files. Without this dependency, the `/actuator/prometheus` endpoint is not exposed, even if actuator is included.
* **Configuration**: Updated `application.yml` and `application-prod.yml` across services to expose the necessary endpoints:
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,prometheus
    endpoint:
      health:
        show-details: always
        probes:
          enabled: true
  ```
### Why it Matters:
* `probes: enabled: true` activates the specific `/actuator/health/liveness` and `/actuator/health/readiness` groups, which are heavily optimized for Kubernetes.
* The `prometheus` endpoint allows a Prometheus server deployed in your cluster to scrape JVM metrics, HTTP request latency, and custom metrics out-of-the-box.

## 2. Kubernetes Liveness & Readiness Probes (Crash Prevention)

Kubernetes relies on probes to know if a pod is ready to receive traffic (Readiness) and if a pod needs to be restarted because it's deadlocked (Liveness).

### What Changed:
* Added missing probes to `api-gateway`, `ai-service`, and `mcp-server` manifests.
* **Crucial Fix for CrashLoopBackOff**: Corrected the probe paths to account for Spring Boot `context-path` settings. 
  * The Logging Service is configured with `server.servlet.context-path: /logging-service`.
  * If Kubernetes checks `/actuator/health`, it gets an HTTP 404. Kubernetes assumes the app is broken and continually restarts the pod.
  * The probe paths in the manifests were updated to reflect their correct base paths:
    * Logging Service: `/logging-service/actuator/health`
    * MCP Service: `/mcp-service/actuator/health`
    * AI Service: `/ai-service/actuator/health`
    * API Gateway: `/actuator/health` (No base path)

## 3. JVM Memory Requests (OOMKilled Prevention)

Java applications, especially those using Spring WebFlux and Kafka (like your API Gateway and Logging Service), require a decent amount of memory to start the JVM and initialize their contexts.

### What Changed:
* Increased the `resources.requests.memory` from `256Mi` to `512Mi` in the Kubernetes deployments.
### Why it Matters:
* With only 256MB allocated, the pods were highly susceptible to `OOMKilled` (Out Of Memory) errors during startup. Reserving 512MB ensures the JVM has enough heap space to boot up gracefully.

## 4. API Gateway Ingress Routing

The Nginx Ingress Controller routes external traffic into the cluster.

### What Changed:
* Removed the `nginx.ingress.kubernetes.io/rewrite-target: /$1` annotation from `api-gateway-ingress.yml`.
* Changed the ingress path matching to just `/` (Prefix).
### Why it Matters:
* The `rewrite-target` annotation was stripping away the URL path (e.g., stripping `/api` from `/api/users/`). However, the Spring Cloud Gateway relies on the *full* path to route requests to the correct backend microservice (using predicates like `Path=/api/users/**`). By removing the rewrite, the Ingress acts as a pure passthrough, allowing the Gateway to handle all routing logic natively.

## 5. Kubernetes Native gRPC Networking

The MCP Service communicates with the Logging Service via gRPC. 

### What Changed:
* Updated `GRPC_CLIENT_LOGGING_SERVICE_ADDRESS` from `discovery:///LOGGING-SERVICE` to `dns:///logging-service:9090` in `mcp-server-manifest.yml`.
### Why it Matters:
* While `discovery:///` works perfectly with Eureka, relying on Eureka for internal service-to-service communication inside Kubernetes is often redundant and adds a single point of failure. Using standard Kubernetes DNS (`dns:///`) leverages the cluster's native CoreDNS, providing a more robust, decoupled network link between your gRPC client and server.
