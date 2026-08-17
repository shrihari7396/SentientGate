# SentientGate Architecture

This document outlines the high-level architecture and the operational sequence of the SentientGate system. The system employs an out-of-band, event-driven security analysis flow using Apache Kafka and Redis.

## System Architecture

The architectural diagram shows how components are separated between the infrastructure layer and the application microservices.

```mermaid
graph TD
    classDef client fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#000;
    classDef gateway fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:#000;
    classDef infra fill:#f3e5f5,stroke:#8e24aa,stroke-width:2px,color:#000;
    classDef service fill:#e8f5e9,stroke:#388e3c,stroke-width:2px,color:#000;
    classDef mcp fill:#ffebee,stroke:#d32f2f,stroke-width:2px,color:#000;

    Client[Client / UI]:::client -->|HTTP/HTTPS| Gateway[API Gateway]:::gateway
    
    subgraph Infrastructure
        Kafka[Apache Kafka]:::infra
        Redis[(Redis Cache)]:::infra
        Postgres[(PostgreSQL)]:::infra
    end
    
    subgraph SentientGate Services
        Gateway
        Dummy[Target Services]:::service
        Logging[Logging Service]:::service
        MCP[MCP Service]:::mcp
        AI[AI Service]:::service
    end
    
    Gateway -->|Forwards Valid Traffic| Dummy
    Gateway -.->|Checks Blacklist| Redis
    Gateway -->|Publishes USER_LOGS| Kafka
    Gateway -->|Publishes SECURITY_EVENTS| Kafka
    
    Kafka -->|Consumes USER_LOGS| Logging
    Logging -->|Stores Logs| Postgres
    Logging -->|Caches Logs| Redis
    
    Kafka -->|Consumes SECURITY_EVENTS| MCP
    MCP -.->|Fetches History via gRPC| Logging
    MCP -->|Analyzes via Rules| MCP
    MCP -->|Async AI Analysis| AI
    MCP -.->|Writes Blacklist| Redis
```

## Sequence Diagram (Request & Threat Detection Flow)

This sequence diagram illustrates a typical request lifecycle. It highlights how the API Gateway processes requests, logs them asynchronously, and triggers the out-of-band threat detection mechanism.

```mermaid
sequenceDiagram
    box rgba(225, 245, 254, 0.4) External
        actor User as Client
    end
    box rgba(255, 249, 196, 0.4) Gateway Layer
        participant AG as API Gateway
    end
    box rgba(243, 229, 245, 0.4) Infrastructure
        participant RS as Redis
        participant K as Kafka
    end
    box rgba(232, 245, 233, 0.4) Core Microservices
        participant TS as Target Service
        participant LS as Logging Service
    end
    box rgba(255, 235, 238, 0.4) Security Brain
        participant MCP as MCP Service
        participant AI as AI Service
    end

    User->>AG: HTTP Request
    AG->>RS: Check if UUID is in Blacklist
    
    alt is Blocked
        RS-->>AG: return True
        AG-->>User: 403 Forbidden
    else is Not Blocked
        RS-->>AG: return False
        AG->>TS: Forward Request
        TS-->>AG: HTTP Response
        
        par Async Logging
            AG-)K: Publish to USER_LOGS
            K-)LS: Consume USER_LOGS
            LS->>RS: Cache in Redis
        end
        
        AG-->>User: HTTP Response
        
        opt If Response is Error (4xx, 5xx)
            AG-)K: Publish to SECURITY_EVENTS
            K-)MCP: Consume SECURITY_EVENTS
            
            MCP->>RS: Check Deduplication (RedisGuard)
            RS-->>MCP: Allow if First Occurrence
            
            MCP->>LS: Fetch History (gRPC)
            LS-->>MCP: Returns last 10m Logs
            
            MCP->>MCP: Run Synchronous Rules
            alt Rules Match Threat
                MCP->>RS: blockUser (Write to Blacklist)
            else No Rules Match
                MCP-)AI: Async AI Analysis Request
                AI-->>MCP: AI Response (Threat Score)
                opt If AI Detects Threat
                    MCP->>RS: blockUser (Write to Blacklist)
                end
            end
        end
    end
```

## Concurrency & Thread Pool Model

To achieve high throughput and reliability, SentientGate uses a mix of Reactive programming and strictly isolated Thread Pools across its services.

### 1. API Gateway & AI Service (Reactive Event Loop)
- Both the **API Gateway** and **AI Service** are built on **Spring WebFlux (Project Reactor)**. 
- They do not use traditional thread-per-request models. Instead, they use a small number of **Netty Event Loop** threads.
- All network calls (Redis checks, target routing, external AI model calls) are completely non-blocking, allowing them to handle thousands of concurrent requests with minimal memory overhead.

### 2. MCP Service (Isolated Thread Pools)
The **MCP Service** processes intensive security rules and acts as the brain of the system. To prevent Kafka lag and ensure high availability, it utilizes two isolated thread pools defined in `AsyncConfig`:

1. **`analysisExecutor` (mcp-analysis-*)**
   - **Core/Max Size**: 8 / 16 threads
   - **Queue Capacity**: 256
   - **Purpose**: When the Kafka Listener consumes a batch of `SECURITY_EVENTS`, it immediately offloads the per-UUID rule analysis to this thread pool. This frees up the Kafka consumer thread to continue polling without waiting for the IO-heavy operations (Redis checks, gRPC history fetching) to complete.
   - **Policy**: `CallerRunsPolicy` to naturally apply backpressure to Kafka if the system is overwhelmed.

2. **`aiExecutor` (mcp-ai-*)**
   - **Core/Max Size**: 4 / 8 threads
   - **Queue Capacity**: 128
   - **Purpose**: A strictly isolated pool dedicated entirely to asynchronous AI inference calls to the AI Service. Since AI models can be slow or experience latency spikes, isolating them in this pool ensures that synchronous rule-based analysis (e.g., Rate Limiting, Burst Traffic) is never starved or blocked by a stalled AI service.

### 3. Logging Service (Kafka Consumers & gRPC)
- Uses default Spring Kafka consumer threads to ingest `USER_LOGS` into PostgreSQL and Redis in bulk.
- Provides a gRPC endpoint served by Tomcat's standard worker thread pool to rapidly serve historical log data back to the MCP Service when requested.
