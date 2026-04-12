# Sentinel Gateway UI - Detailed API Specifications

This document outlines all API endpoints required by the React Frontend. It acts as the definitive contract between the backend microservices and the frontend client.

For every endpoint, this document identifies the responsible **Microservice**, **HTTP Method, Path**, **Path/Query Parameters**, and exact **Request / Response Schemas** including field types.

---

## 🏗 Backend Service Allocation Mapping

- **ApiGateway**: Pipeline metrics, traffic stats, rate-limiting rules, infra health.
- **EurekaServer**: Service discovery, registry status, proxying Spring Boot Actuator health checks.
- **LoggingService**: Distributed audit logging, log querying/pagination.
- **AIService**: Threat intelligence, anomaly scoring, blacklisting, Context Evaluation.

---

## 🚀 1. Real-Time Monitoring (WebSocket Architecture Proposal)

*To transition from the current HTTP Long-Polling architecture to true real-time streaming, the backend should implement a WebSocket Broker.*

**Connection Endpoint:** `ws://localhost:8079/ws-stream` or `ws://localhost:8079/api/stomp`

| Topic Path | Publishing Service | Payload Schema | Replaces REST API |
|------------|--------------------|----------------|-------------------|
| `/topic/pipeline/events` | **ApiGateway** | `PipelineEvent` | `GET /pipeline/events` |
| `/topic/threat/feed` | **AIService** | `AnomalyEvent` | `GET /threat/feed` |
| `/topic/dashboard/traffic` | **ApiGateway** | `TrafficData` | `GET /dashboard/traffic` |

---

## 📊 2. REST API Specifications

### Base Setup
- **Base URL:** `http://localhost:8079/api`
- **Global Headers:**
  - `Authorization: Bearer <JWT_TOKEN>` (for all secured endpoints)
  - `Content-Type: application/json`

---

### A. Pipeline & Metrics (ApiGateway)

#### a1. Get Pipeline Stage Statistics
- **Endpoint:** `GET /pipeline/stats`
- **Description:** Returns the operational status and processed request count for the underlying gateway pipeline filters (e.g., Auth Filter, Rate Limiter, AI Inspector).
- **Request Body:** None
- **Response Schema:** Array of `PipelineStage`

**`PipelineStage` Object:**
| Field | Type | Description |
|---|---|---|
| `stage` | `string` | Unique identifier/name of the pipeline stage (e.g., "rate-limiter"). |
| `requestsToday` | `integer` | Count of requests processed by this stage today. |
| `status` | `enum` | Operational status: `'UP'`, `'DOWN'`, `'DEGRADED'`. |
| `lastError` | `string` \| `null` | Most recent error message, if any. |

```json
[
  { "stage": "jwt-auth", "requestsToday": 45000, "status": "UP", "lastError": null }
]
```

#### a2. Get Recent Pipeline Events
- **Endpoint:** `GET /pipeline/events`
- **Description:** Returns recent individual request routing outcomes.
- **Request Body:** None
- **Response Schema:** Array of `PipelineEvent`

**`PipelineEvent` Object:**
| Field | Type | Description |
|---|---|---|
| `id` | `string` | Unique event ID. |
| `timestamp` | `string` (ISO-8601) | When the event occurred. |
| `uuid` | `string` | The UUID of the requesting entity (Actor). |
| `status` | `enum` | Final decision: `'PASSED'` or `'BLOCKED'`. |
| `stage` | `string` | The specific pipeline stage where the status was determined. |

---

### B. Registry & Service Discovery (EurekaServer)

#### b1. List Registered Services
- **Endpoint:** `GET /registry/services`
- **Description:** Returns an aggregation of all registered microservices.
- **Response Schema:** Array of `EurekaService`

**`EurekaService` Object:**
| Field | Type | Description |
|---|---|---|
| `id` | `string` | Spring Application Name / Eureka ID (e.g., "AUTH-SERVICE"). |
| `name` | `string` | Human-readable name. |
| `instanceCount` | `integer` | Number of live instances registered. |
| `status` | `enum` | General status: `'UP'`, `'DOWN'`, `'STARTING'`. |

#### b2. Get Service Instances
- **Endpoint:** `GET /registry/services/{serviceId}`
- **Path Parameter `serviceId`:** `string` (The Eureka Service ID).
- **Response Schema:** Array of `EurekaInstance`

**`EurekaInstance` Object:**
| Field | Type | Description |
|---|---|---|
| `host` | `string` | The IP address or hostname of the instance. |
| `port` | `integer` | The port the instance is listening on. |
| `status` | `string` | Instance status (`UP`, `OUT_OF_SERVICE`). |
| `homepage` | `string` | The instance's health or homepage URL. |
| `lastHeartbeat` | `string` (ISO-8601) | Last heartbeat timestamp. |

#### b3. Get Service Actuator Health
- **Endpoint:** `GET /registry/actuator/{serviceId}/health`
- **Description:** Proxies the standard Spring Boot `/actuator/health` endpoint for a specific service.
- **Path Parameter `serviceId`:** `string`.
- **Response Schema:** Unstructured JSON, matching standard Spring Boot Actuator format.

---

### C. Dashboard Analytics (ApiGateway)

#### c1. Get Core KPIs
- **Endpoint:** `GET /dashboard/metrics`
- **Response Schema:** `DashboardMetrics`

**`DashboardMetrics` Object:**
| Field | Type | Description |
|---|---|---|
| `requestsPerMin` | `integer` | Current TPM (Transactions per minute). |
| `blockedThreats` | `integer` | Threats blocked in the last 24h. |
| `p99Latency` | `string` | 99th percentile latency (e.g., "45ms"). |
| `activeServices` | `integer` | Total number of healthy upstream services. |

#### c2. Get Traffic Time-Series Data
- **Endpoint:** `GET /dashboard/traffic`
- **Response Schema:** Array of `TrafficData`

**`TrafficData` Object:**
| Field | Type | Description |
|---|---|---|
| `timestamp` | `string` (ISO-8601) | Timestamp of the data point. |
| `coreFlow` | `integer` | Count of legitimate requests. |
| `threatVectors` | `integer` | Count of malicious/blocked requests. |

#### c3. Get Threat Distribution
- **Endpoint:** `GET /dashboard/threat-dist`
- **Response Schema:** Array of `ThreatDist`

**`ThreatDist` Object:**
| Field | Type | Description |
|---|---|---|
| `type` | `string` | The threat category (e.g., "SQL Injection"). |
| `count` | `integer` | Number of occurrences. |

#### c4. Get Recent Blocks
- **Endpoint:** `GET /dashboard/recent-blocks`
- **Response Schema:** Array of `RecentBlock`

**`RecentBlock` Object:**
| Field | Type | Description |
|---|---|---|
| `uuid` | `string` | The blocked identity/IP/UUID. |
| `reason` | `string` | Why the block occurred. |
| `blockedAt` | `string` (ISO-8601) | Timestamp of block generation. |
| `ttlSeconds` | `integer` | Time to live for the block penalty. |

---

### D. Threat Intelligence & AI (AIService)

#### d1. Get Threat Statistics Overview
- **Endpoint:** `GET /threat/stats`
- **Response Schema:** Arbitrary JSON mapping to overview highlights (e.g., `{ totalBlockedToday: 500, highSeverityAnomalies: 12 }`).

#### d2. Get Active Strategies
- **Endpoint:** `GET /threat/strategies`
- **Response Schema:** Array of `Strategy`

**`Strategy` Object:**
| Field | Type | Description |
|---|---|---|
| `id` | `string` | Strategy identifier. |
| `name` | `string` | Strategy display name. |
| `description` | `string` | Explanation of what the strategy detects. |
| `enabled` | `boolean` | Flag indicating if this strategy is actively enforced. |
| `lastFired` | `string` (ISO-8601) | Timestamp of the last time this strategy triggered a block. |
| `firesToday` | `integer` | Number of times fired today. |

#### d3. Toggle Strategy State
- **Endpoint:** `POST /threat/strategies/{id}/toggle`
- **Path Parameter `id`:** `string` (Strategy identifier).
- **Request Body:** None
- **Response Schema:** Empty body (Status `200 OK`)

#### d4. Get Live Threat Feed
- **Endpoint:** `GET /threat/feed`
- **Response Schema:** Array of `AnomalyEvent`

**`AnomalyEvent` Object:**
| Field | Type | Description |
|---|---|---|
| `uuid` | `string` | Target Identity. |
| `timestamp` | `string` (ISO-8601) | Time of detection. |
| `source` | `enum` | `'HEURISTIC'` or `'AI_MODEL'`. |
| `strategyFired` | `string` (Optional) | ID of the specific strategy if heuristic. |
| `anomalyScore` | `float` | AI confidence score (0.0 to 1.0) indicating maliciousness. |
| `decision` | `enum` | Target outcome: `'BLACKLISTED'`, `'ALLOWED'`, `'MONITORING'`. |
| `context` | `string` (Optional) | Short context regarding the incident. |
| `reasoningText`| `string` (Optional) | Detailed AI reasoning/explanation. |

#### d5. Read Blacklist
- **Endpoint:** `GET /threat/blacklist`
- **Response Schema:** Array of `BlacklistEntry` (Identical schema to `RecentBlock`).

#### d6. Remove UUID from Blacklist
- **Endpoint:** `DELETE /threat/blacklist/{uuid}`
- **Path Parameter `uuid`:** `string`.
- **Request Body:** None
- **Response Schema:** Empty body (Status `200 OK`)

---

### E. Logs & Audit Trail (LoggingService)

#### e1. Query Logs
- **Endpoint:** `GET /logs`
- **Query Parameters:**
  - `path` (string) - Filters logs by request path substring.
  - `status` (string/number) - Filters logs by HTTP response status.
  - `uuid` (string) - Filters logs by requesting user UUID.
  - `page` (number) - **Default 0**. For Pagination.
  - `size` (number) - **Default 50**. Elements per page.
- **Response Schema:** Paginated Spring Boot response containing `LogEntry` objects.

**Paginated Wrapper:**
| Field | Type | Description |
|---|---|---|
| `content` | `Array<LogEntry>` | The log entries for the current page. |
| `totalPages` | `integer` | Total number of pages available. |
| `totalElements` | `integer` | Total total items matching the query. |

**`LogEntry` Object:**
| Field | Type | Description |
|---|---|---|
| `id` | `string` | Unique log ID (often from Elastic/MongoDB). |
| `timestamp` | `string` (ISO-8601) | Exact time of the request trace. |
| `uuid` | `string` | Identified Actor. |
| `method` | `string` | HTTP Method (e.g. `GET`, `POST`). |
| `endpoint` | `string` | Request Path (e.g. `/api/v1/users`). |
| `status` | `integer` | Backend HTTP Status code returned to the client. |
| `latency` | `integer` | Milliseconds taken. |
| `routeId` | `string` | The downstream Route mapped to the target microservice. |
| `threatFlagged`| `boolean` | Indicates if an anomaly/threat process caught this. |
| `headers` | `object` (Optional)| Key-Value map of captured HTTP headers. |
| `kafkaEventId` | `string` (Optional)| Tracing ID within Kafka topic. |
| `threatDetails`| `object` (Optional)| Nested AI model explanation if flagged. |

---

### F. User Security Context (AIService & LoggingService)

#### f1. Get Complete Context for UUID
- **Endpoint:** `GET /users/{uuid}/context`
- **Path Parameter `uuid`:** `string`.
- **Description:** An aggregated endpoint providing a complete contextual breakdown of a user's behavior. Displayed in the User Context Drawer component.
- **Response Schema:** `UserContext`

**`UserContext` Object:**
| Field | Type | Description |
|---|---|---|
| `status` | `enum` | `'ALLOWED'`, `'MONITORING'`, `'BLACKLISTED'`. |
| `timeline` | `Array<TimelinePoint>`| Time-series representation of the user's velocity. |
| `aiProfile` | `AIProfile` | Deep profiling metrics generated by the AI inspector. |
| `activeStrategies` | `Array<string>`| Identifiers of any strategies currently flagging this user. |
| `recentRequests` | `Array<RequestTrace>`| A sample of their most recent traffic. |

**`TimelinePoint` Object:**
| Field | Type | Description |
|---|---|---|
| `time` | `string` (HH:mm) | The hour:minute grouping for the data point. |
| `requestCount` | `integer` | Number of requests made in this bucket. |

**`AIProfile` Object:**
| Field | Type | Description |
|---|---|---|
| `anomalyScores` | `Array<{ time: string, score: float }>` | Data points plotting the history of their AI risk score. |
| `lastAssessment` | `string` | An automated text sentence detailing the AI's current conclusion about the actor. |

**`RequestTrace` Object:**
| Field | Type | Description |
|---|---|---|
| `timestamp` | `string` | When the request occurred. |
| `method` | `string` | HTTP Method. |
| `endpoint` | `string` | HTTP Target Path. |
| `status` | `integer` | Resulting HTTP Status Code. |

---

### G. Infrastructure Health (ApiGateway)

#### g1. Get Service Status
- **Endpoints:**
  - `GET /infra/kafka`
  - `GET /infra/redis`
  - `GET /infra/postgres`
- **Response Schema:** Expected to return JSON indicating UP time and details.
```json
{
  "status": "UP",
  "details": {
    "uptime": "24h 13m",
    "connections": 54
  }
}
```
