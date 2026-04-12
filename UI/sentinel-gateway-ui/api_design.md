# Sentinel Gateway UI - API Design

This document outlines the API endpoints required by the Frontend, including their expected request parameters, query parameters, and JSON response shapes. It is meant to guide the backend implementation to ensure compatibility with the frontend's data fetching hooks.

Base URL configured in frontend: `http://localhost:8079/api`
Global Headers: `Authorization: Bearer <token>` (if user is authenticated).

---

## 1. Pipeline Metrics & Events

### `GET /pipeline/stats`
Fetches the current status and statistics for each stage of the API Gateway pipeline.
- **Response**: `PipelineStage[]`
```json
[
  {
    "stage": "rate-limiter",
    "requestsToday": 15024,
    "status": "UP", // 'UP' | 'DOWN' | 'DEGRADED'
    "lastError": null
  }
]
```

### `GET /pipeline/events`
Fetches a live stream or latest list of pipeline events, showing pass/block decisions per request.
- **Response**: `PipelineEvent[]`
```json
[
  {
    "id": "evt-123",
    "timestamp": "2023-10-27T10:00:00Z",
    "uuid": "usr-456",
    "status": "PASSED", // 'PASSED' | 'BLOCKED'
    "stage": "jwt-auth"
  }
]
```

---

## 2. Registry & Service Discovery (Eureka)

### `GET /registry/services`
Fetches the high-level list of registered services.
- **Response**: `EurekaService[]`
```json
[
  {
    "id": "auth-service",
    "name": "Auth Service",
    "instanceCount": 3,
    "status": "UP" // 'UP' | 'DOWN' | 'STARTING'
  }
]
```

### `GET /registry/services/{serviceId}`
Fetches the individual instances for a given service.
- **Path Variable**: `serviceId` (e.g., `auth-service`)
- **Response**: `EurekaInstance[]`
```json
[
  {
    "host": "10.0.0.1",
    "port": 8081,
    "status": "UP",
    "homepage": "http://10.0.0.1:8081/",
    "lastHeartbeat": "2023-10-27T10:05:00Z"
  }
]
```

### `GET /registry/actuator/{serviceId}/health`
Fetches detailed actuator health metrics for a specific service.
- **Path Variable**: `serviceId`
- **Response**: Arbitrary Actuator Health JSON (e.g., standard Spring Boot Actuator format)
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" }
  }
}
```

---

## 3. Dashboard Analytics

### `GET /dashboard/metrics`
Fetches top-level KPIs for the traffic dashboard.
- **Response**: `DashboardMetrics`
```json
{
  "requestsPerMin": 1420,
  "blockedThreats": 15,
  "p99Latency": "45ms",
  "activeServices": 12
}
```

### `GET /dashboard/traffic`
Fetches time-series data for the traffic area chart.
- **Response**: `TrafficData[]`
```json
[
  {
    "timestamp": "2023-10-27T10:00:00Z",
    "coreFlow": 150,
    "threatVectors": 2
  }
]
```

### `GET /dashboard/threat-dist`
Fetches data for the threat distribution chart.
- **Response**: `ThreatDist[]`
```json
[
  {
    "type": "SQL Injection",
    "count": 45
  },
  {
    "type": "Rate Limit Exceeded",
    "count": 120
  }
]
```

### `GET /dashboard/recent-blocks`
Fetches a list of recently blocked actors.
- **Response**: `RecentBlock[]`
```json
[
  {
    "uuid": "usr-999",
    "reason": "Suspicious payload",
    "blockedAt": "2023-10-27T10:15:00Z",
    "ttlSeconds": 3600
  }
]
```

---

## 4. Threat Intelligence

### `GET /threat/stats`
Fetches aggregate threat stats (format flexible, generally matches the overview).
- **Response**:
```json
{
  "totalBlockedToday": 500,
  "highSeverityAnomalies": 12,
  "activeBlacklistedIp": 45
}
```

### `GET /threat/strategies`
Fetches list of active threat detection strategies and their status.
- **Response**: `Strategy[]`
```json
[
  {
    "id": "strat-sql-inj",
    "name": "SQLi Blocker",
    "description": "Blocks common SQL injection patterns",
    "enabled": true,
    "lastFired": "2023-10-27T09:00:00Z",
    "firesToday": 50
  }
]
```

### `POST /threat/strategies/{id}/toggle`
Toggles a detection strategy on or off. No body required.
- **Path Variable**: `id`
- **Response**: `200 OK` (Empty response body)

### `GET /threat/feed`
Fetches the live anomaly feed.
- **Response**: `AnomalyEvent[]`
```json
[
  {
    "uuid": "usr-123",
    "timestamp": "2023-10-27T10:20:00Z",
    "source": "AI_MODEL", // 'HEURISTIC' | 'AI_MODEL'
    "strategyFired": "strat-anomaly-1",
    "anomalyScore": 0.95,
    "decision": "BLACKLISTED", // 'BLACKLISTED' | 'ALLOWED' | 'MONITORING'
    "context": "Unusual header footprint",
    "reasoningText": "Spike in payload size with unknown user agent"
  }
]
```

### `GET /threat/blacklist`
Fetches currently blacklisted UUIDs.
- **Response**: `BlacklistEntry[]`
```json
[
  {
    "uuid": "usr-xyz",
    "reason": "Rate limited excessively",
    "blockedAt": "2023-10-27T10:00:00Z",
    "ttlSeconds": 86400
  }
]
```

### `DELETE /threat/blacklist/{uuid}`
Unblocks a specific identity/UUID.
- **Path Variable**: `uuid`
- **Response**: `200 OK` (Empty response body)

---

## 5. Logs

### `GET /logs`
Fetches audit logs with pagination and filtering. 
- **Query Params**:
  - `path` (string, optional)
  - `status` (string/number, optional)
  - `uuid` (string, optional)
  - `page` (number, default: 0)
  - `size` (number, default: 50)
- **Response**: Paginated Object containing `content` of type `LogEntry[]`
```json
{
  "content": [
    {
      "id": "log-1",
      "timestamp": "2023-10-27T10:25:00Z",
      "uuid": "usr-abc",
      "method": "POST",
      "endpoint": "/api/v1/payments",
      "status": 200,
      "latency": 45,
      "routeId": "payment-svc",
      "threatFlagged": false,
      "headers": { "User-Agent": "Mozilla..." },
      "kafkaEventId": "kfk-123",
      "threatDetails": null,
      "blacklistStatus": null
    }
  ],
  "totalPages": 10,
  "totalElements": 500
}
```

---

## 6. Access / Context

### `GET /users/{uuid}/context`
Fetches highly detailed security profiling context for a specific user identity to populate the Context Drawer.
- **Path Variable**: `uuid`
- **Response**:
```json
{
  "status": "BLACKLISTED", // 'ALLOWED' | 'MONITORING' | 'BLACKLISTED'
  "timeline": [
    { "time": "10:01", "requestCount": 5 },
    { "time": "10:02", "requestCount": 150 }
  ],
  "aiProfile": {
    "anomalyScores": [
      { "time": "10:01", "score": 0.1 },
      { "time": "10:02", "score": 0.85 }
    ],
    "lastAssessment": "Highly erratic request volume resembling enumeration attack."
  },
  "activeStrategies": ["rate-limiter", "behavior-anomaly"],
  "recentRequests": [
    {
      "timestamp": "2023-10-27T10:02:00Z",
      "method": "GET",
      "endpoint": "/api/users/1",
      "status": 403
    }
  ]
}
```

---

## 7. Infrastructure Health

### `GET /infra/kafka`
### `GET /infra/redis`
### `GET /infra/postgres`
Fetches status of core gateway backing services.
- **Response**: Flexible JSON, expected to contain status indicators.
```json
{
  "status": "UP",
  "details": {
    "uptime": "24h",
    "connections": 15
  }
}
```
