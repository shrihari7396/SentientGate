# SentientGate — Infrastructure Services

This directory contains the Kubernetes manifests for all infrastructure services required by the SentientGate platform.

## Services Overview

| Service    | Image                           | K8s Service Name   | Port  | Protocol |
|------------|---------------------------------|--------------------|-------|----------|
| Kafka      | `confluentinc/cp-kafka:7.7.1`  | `kafka-service`    | 9092  | TCP      |
| PostgreSQL | `postgres:17`                  | `postgres`         | 5432  | TCP      |
| Redis      | `redis:7.4`                    | `redis`            | 6379  | TCP      |

> **Note:** Kafka runs in **KRaft mode** (no Zookeeper required). The controller and broker are co-located in a single node.

---

## Deployment Order

Infrastructure services must be deployed **before** any application services. Deploy in this order:

```bash
# 1. PostgreSQL (has PVC + Secret dependencies)
bash deploy/Infrastructure/postgres/deploy/deploy.sh

# 2. Redis
bash deploy/Infrastructure/redis/deploy/deploy.sh

# 3. Kafka (KRaft — self-contained, no Zookeeper)
bash deploy/Infrastructure/kafka/deploy/deploy.sh
```

Or deploy all at once:

```bash
for svc in postgres redis kafka; do
  bash deploy/Infrastructure/${svc}/deploy/deploy.sh
done
```

---

## Accessing Services from Application Pods

Application services running inside the same Kubernetes cluster can connect to infrastructure using the **K8s Service DNS names**.

### Kafka

| Property                            | Value                  |
|-------------------------------------|------------------------|
| Bootstrap Servers                   | `kafka-service:9092`   |
| Listener Protocol                   | `PLAINTEXT`            |

**Spring Boot config example:**
```yaml
spring:
  kafka:
    bootstrap-servers: kafka-service:9092
```

**Environment variable:**
```
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-service:9092
```

---

### PostgreSQL

| Property         | Value                                  |
|------------------|----------------------------------------|
| Host             | `postgres`                             |
| Port             | `5432`                                 |
| Database         | `mydb`                                 |
| Username         | `postgres` (from `postgres-secret`)    |
| Password         | `postgres` (from `postgres-secret`)    |
| JDBC URL         | `jdbc:postgresql://postgres:5432/mydb` |

**Spring Boot config example:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/mydb
    username: postgres
    password: postgres
```

**Environment variables:**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mydb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
```

---

### Redis

| Property | Value   |
|----------|---------|
| Host     | `redis` |
| Port     | `6379`  |

**Spring Boot config example:**
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
```

**Environment variable:**
```
SPRING_DATA_REDIS_HOST=redis
```

---

## Directory Structure

```
Infrastructure/
├── kafka/
│   └── deploy/
│       ├── config.yml        # ConfigMap — KRaft broker settings
│       ├── deployment.yml    # Deployment + Service
│       └── deploy.sh         # One-command deploy script
├── postgres/
│   └── deploy/
│       ├── config.yml        # ConfigMap — database name
│       ├── secret.yml        # Secret — credentials (base64)
│       ├── pvc.yml           # PersistentVolumeClaim — 1Gi storage
│       ├── deployment.yml    # Deployment + Service
│       └── deploy.sh         # One-command deploy script
└── redis/
    └── deploy/
        ├── deployment.yml    # Deployment + Service
        └── deploy.sh         # One-command deploy script
```

---

## Health Checks

All infrastructure deployments include readiness and liveness probes:

| Service    | Probe Type  | Method              | Initial Delay |
|------------|-------------|---------------------|---------------|
| Kafka      | TCP Socket  | Port `9092`         | 30s / 60s     |
| PostgreSQL | Exec        | `pg_isready -U postgres` | 10s / 30s |
| Redis      | Exec        | `redis-cli ping`    | 5s / 15s      |

---

## Teardown

To remove all infrastructure resources:

```bash
kubectl delete -f deploy/Infrastructure/kafka/deploy/deployment.yml
kubectl delete -f deploy/Infrastructure/kafka/deploy/config.yml
kubectl delete -f deploy/Infrastructure/redis/deploy/deployment.yml
kubectl delete -f deploy/Infrastructure/postgres/deploy/deployment.yml
kubectl delete -f deploy/Infrastructure/postgres/deploy/pvc.yml
kubectl delete -f deploy/Infrastructure/postgres/deploy/config.yml
kubectl delete -f deploy/Infrastructure/postgres/deploy/secret.yml
```

> **Warning:** Deleting the PostgreSQL PVC will permanently destroy all stored data.
