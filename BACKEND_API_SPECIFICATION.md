# SentientGate Integrated Backend API Specifications

This document defines the complete API specification, request/response structures, query parameters, gRPC schemas, and Kafka topics for all microservices in the SentientGate security ecosystem.

---

## 🗺️ System Topology & Ports
When running in containerized mode (e.g. Kubernetes or Docker Compose), all microservices internally bind Tomcat/Netty to port `8080`. External host mapping defaults to:

| Service | Protocol | Host/Proxy Port | Container Port | Base Path / Context |
| :--- | :--- | :--- | :--- | :--- |
| **ApiGateway** | HTTP (WebFlux) | `8079` | `8080` | `/` |
| **LoggingService** | HTTP (Tomcat) | `8010` | `8080` | `/logging-service` |
| **LoggingService** | gRPC | `9090` | `9090` | `-` |
| **MCPService** | HTTP (Tomcat) | `9991` | `8080` | `/mcp-service` |
| **AIService** | HTTP (Tomcat) | `8082` | `8080` | `/ai-service` |
| **DummyService** | HTTP (Tomcat) | `8090` | `8080` | `/dummy` |

---

## 🛡️ 1. ApiGateway (Local & Threat Endpoints)
The `ApiGateway` handles local requests for blacklisting and unblocking visitors, storing security blocks directly in Redis.

### 1.1 Read Threat Blacklist
* **Endpoint:** `GET /api/threat/blacklist` (Also resolves under alias `/api/mgmt/blacklist`)
* **Description:** Retrieves the list of currently blacklisted visitor UUIDs, along with the block reason, creation timestamp, and remaining TTL (time-to-live) in seconds.
* **Request Headers:**
  * `Authorization: Bearer <JWT_TOKEN>` (If authentication is enabled)
* **Request Parameters:** None
* **Response (JSON Array):** `200 OK`
  ```json
  [
    {
      "uuid": "u-user123-abc456",
      "reason": "AI_BEHAVIORAL_ANOMALY",
      "blockedAt": "2026-06-20T08:30:15Z",
      "ttlSeconds": 3542
    },
    {
      "uuid": "u-attacker-99",
      "reason": "MANUAL_BLOCK",
      "blockedAt": "2026-06-20T09:12:45Z",
      "ttlSeconds": 240
    }
  ]
  ```

### 1.2 Manually Block Visitor
* **Endpoint:** `POST /api/threat/blacklist/{uuid}` (Also resolves under alias `/api/mgmt/blacklist/{uuid}`)
* **Description:** Manually blocks a visitor UUID by serializing a default `BlockRecord` and writing it to Redis with a 1-hour expiration TTL.
* **Path Parameters:**
  * `uuid` (String, Required) - The unique visitor ID to block.
* **Request Body:** None
* **Response:** `200 OK` (Empty response body)

### 1.3 Manually Unblock Visitor (Delete Blacklist)
* **Endpoint:** `DELETE /api/threat/blacklist/{uuid}` (Also resolves under alias `/api/mgmt/blacklist/{uuid}`)
* **Description:** Unblocks a visitor by removing their blacklist key from the Redis database.
* **Path Parameters:**
  * `uuid` (String, Required) - The unique visitor ID to unblock.
* **Request Body:** None
* **Response:** `200 OK` (Empty response body)

---

## 📊 2. LoggingService
The `LoggingService` aggregates gateway traffic, stores logs in PostgreSQL, and serves paginated query interfaces matching the frontend schema.

### 2.1 Get Paginated Traffic Logs
* **Endpoint:** `GET /logging-service/api/logs` (Also resolves under alias `/logging-service/api/logs/raw`)
* **Description:** Retrieves a paginated list of traffic log events with optional request path and status code filters.
* **Query Parameters:**
  * `page` (Integer, Optional, Default: `0`) - Zero-indexed page number.
  * `size` (Integer, Optional, Default: `20`) - Size of the page chunk.
  * `sortBy` (String, Optional, Default: `"occurredAt"`) - Field name to sort by.
  * `direction` (String, Optional, Default: `"DESC"`) - Sorting order (`ASC` or `DESC`).
  * `path` (String, Optional) - Filter logs by request path.
  * `statusCode` (Integer, Optional) - Filter logs by response HTTP status code.
* **Response (JSON Page Object):** `200 OK`
  ```json
  {
    "content": [
      {
        "id": "log-7b1a2c3d-e4f5",
        "uuid": "u-user456",
        "endpoint": "/api/v1/auth/login",
        "method": "POST",
        "status": 200,
        "latency": 150,
        "routeId": "auth-service",
        "threatFlagged": false,
        "timestamp": "2026-06-20T09:15:00Z"
      },
      {
        "id": "log-9x8y7z6w-a1b2",
        "uuid": "u-attacker-99",
        "endpoint": "/api/v1/admin/secrets",
        "method": "GET",
        "status": 403,
        "latency": 45,
        "routeId": "admin-service",
        "threatFlagged": true,
        "timestamp": "2026-06-20T09:16:12Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalPages": 1,
    "totalElements": 2,
    "last": true,
    "size": 20,
    "number": 0,
    "first": true,
    "numberOfElements": 2,
    "empty": false
  }
  ```

### 2.2 gRPC Protocol: `UserLogEventService` (Port `9090`)
Used internally by `MCPService` to query historical logs of visitors triggered by security alerts.

* **RPC Method:** `GetUserEvents`
* **Request Message (`UserLogEventsRequest`):**
  ```protobuf
  message UserLogEventsRequest {
    string uuid = 1;      // Visitor ID to search
    int32 duration = 2;   // Lookback window size in minutes
  }
  ```
* **Response Message (`UserLogEventResponse`):**
  ```protobuf
  message UserLogEventResponse {
    repeated UserLogEvent user_log_events = 1;
  }

  message UserLogEvent {
    string uuid = 1;
    string path = 2;
    string method = 3;
    int64 latency_ms = 4;
    string query_params = 5;
    string client_ip = 6;
    int32 status_code = 7;
    int64 request_size = 8;
    int64 timestamp = 9;
    string user_agent = 10;
  }
  ```

---

## 🧠 3. AIService (Behavioral Anomaly Detection)
Exposes the analytical ML layer. It extracts signals from raw histories and formats prompts for local Large Language Models (LLMs).

### 3.1 Analyze Visitor History Log Array
* **Endpoint:** `POST /ai-service/api/v1/analyze`
* **Description:** Evaluates a series of visitor logs to assess threat level and anomaly metrics.
* **Request Headers:**
  * `Content-Type: application/json`
* **Request Body (JSON):**
  ```json
  {
    "uuid": "u-user456",
    "history": [
      {
        "uuid": "u-user456",
        "endpoint": "/api/v1/users/profile",
        "method": "GET",
        "status": 200,
        "latency": 35,
        "threatFlagged": false,
        "timestamp": "2026-06-20T09:12:00Z"
      },
      {
        "uuid": "u-user456",
        "endpoint": "/api/v1/payments/withdraw",
        "method": "POST",
        "status": 403,
        "latency": 150,
        "threatFlagged": true,
        "timestamp": "2026-06-20T09:12:15Z"
      }
    ]
  }
  ```
* **Response Body (JSON):** `200 OK`
  ```json
  {
    "isAnomaly": true,
    "confidenceScore": 0.89,
    "patternDetected": "CREDENTIAL_STUFFING_OR_PROBING",
    "suggestedBlockMinutes": 60
  }
  ```

---

## ⚡ 4. Kafka Event Schemas (Internal Backplane)
Kafka acts as the asynchronous communication backplane carrying logs and security alerts.

### 4.1 Topic: `user-logs` (Traffic Ingestion)
* **Emitted By:** `ApiGateway`
* **Consumed By:** `LoggingService`
* **Payload Structure (`LogEvent`):**
  ```json
  {
    "uuid": "u-user456",
    "path": "/api/v1/auth/login",
    "method": "POST",
    "routeId": "auth-service",
    "decision": "ALLOWED",
    "latencyMs": 150,
    "queryParams": "{}",
    "clientIp": "127.0.0.1",
    "statusCode": 200,
    "requestSize": 512,
    "timestamp": 1781946900000,
    "userAgent": "Mozilla/5.0..."
  }
  ```

### 4.2 Topic: `security-events` (Mitigation & Enforcement Trigger)
* **Emitted By:** `ApiGateway` (when response status is `< 200` or `>= 300`)
* **Consumed By:** `MCPService`
* **Payload Structure (`SecurityAlertEvent`):**
  ```json
  {
    "uuid": "u-attacker-99",
    "errorCode": 403,
    "reason": "Forbidden",
    "attemptedPath": "/api/v1/admin/secrets",
    "method": "GET",
    "userAgent": "curl/7.68.0",
    "clientIp": "192.168.1.100",
    "alertSeverity": "MEDIUM",
    "timestamp": 1781946972000
  }
  ```

---

## 💾 5. Redis Data Storage Structure
`MCPService` and `ApiGateway` communicate blocks asynchronously via Redis.

* **Key Format:** `blacklist:<uuid>`
* **Value Schema (JSON representing `BlockRecord`):**
  ```json
  {
    "reason": "RateAnomalyStrategy",
    "severity": "MEDIUM",
    "blockedAt": 1781946972000,
    "expiresAt": 1781950572000
  }
  ```
