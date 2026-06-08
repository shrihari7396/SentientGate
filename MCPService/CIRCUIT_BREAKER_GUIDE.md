# Circuit Breaker Pattern - MCPService Implementation Guide

## Table of Contents
1. [What is a Circuit Breaker?](#what-is-a-circuit-breaker)
2. [How Circuit Breakers Work](#how-circuit-breakers-work)
3. [Circuit Breaker States](#circuit-breaker-states)
4. [Our Implementation](#our-implementation)
5. [Configuration Details](#configuration-details)
6. [Monitoring & Health Checks](#monitoring--health-checks)
7. [Scenarios & Examples](#scenarios--examples)
8. [Best Practices](#best-practices)

---

## What is a Circuit Breaker?

A **Circuit Breaker** is a design pattern used in microservices to prevent cascading failures. It acts like an electrical circuit breaker in your home:
- When everything is working fine → current flows freely (calls succeed)
- When problems occur → it "trips" and stops the current (fails fast)
- After some time → it tests if the problem is fixed (tries again)

### Why is it Important?

Imagine the AI Service goes down:
- **Without Circuit Breaker**: MCPService keeps sending requests to a dead service, wasting resources and slowing down the entire system
- **With Circuit Breaker**: After detecting failures, it immediately stops sending requests and returns a safe fallback response

---

## How Circuit Breakers Work

### Basic Flow

```
User Request
    ↓
[Circuit Breaker Check]
    ↓
Is Circuit CLOSED? → YES → Send to AI Service → Success/Failure recorded
Is Circuit OPEN?   → NO  → Return Fallback Response (fail fast)
Is Circuit HALF-OPEN? → Test Call to AI Service → Updates state
```

### Failure Detection Process

```
Request 1: FAIL → Count = 1
Request 2: FAIL → Count = 2
Request 3: FAIL → Count = 3
Request 4: FAIL → Count = 4
Request 5: FAIL → Count = 5 (Min threshold reached)

Failure Rate = 5/5 = 100% > 50% threshold
↓
OPEN THE CIRCUIT! 🔴
```

---

## Circuit Breaker States

### 1. **CLOSED State** ✅ (Normal Operation)
```
State: CLOSED
↓
Status: Requests flow normally to AI Service
↓
Behavior: 
- Every request is sent to the actual service
- Failures are counted
- If failure rate exceeds threshold → transition to OPEN

Condition to OPEN:
- Failure rate > 50% AND
- Minimum 5 calls received
```

**Example Timeline:**
```
Time  | Call | Result | Status
------|------|--------|----------
10:00 | 1    | ✓ OK   | CLOSED
10:01 | 2    | ✓ OK   | CLOSED
10:02 | 3    | ✗ FAIL | CLOSED (counting failures)
10:03 | 4    | ✗ FAIL | CLOSED
10:04 | 5    | ✗ FAIL | CIRCUIT OPENS → 60% failure rate
```

---

### 2. **OPEN State** 🔴 (Service Down)
```
State: OPEN
↓
Status: Service is temporarily unavailable
↓
Behavior:
- ALL requests are rejected immediately (fail fast)
- No calls sent to AI Service (saves resources)
- Fallback response returned
- Waits for 10 seconds before testing recovery

What is a Fallback Response?
{
  "isAnomaly": false,
  "confidenceScore": 0.0,
  "patternDetected": "SERVICE_UNAVAILABLE",
  "suggestedBlockMinutes": 0
}
```

**Example Timeline:**
```
Time  | Event
------|------
10:04 | Circuit OPENS (50% failure rate reached)
10:05 | User Request → Immediate rejection (fallback)
10:06 | User Request → Immediate rejection (fallback)
10:07 | User Request → Immediate rejection (fallback)
10:14 | 10 seconds passed → Transition to HALF_OPEN
```

**Benefits:**
- Prevents wasting resources on dead service
- Returns response faster (fail fast)
- Gives the service time to recover
- Prevents cascading failures to other services

---

### 3. **HALF-OPEN State** 🟡 (Testing Recovery)
```
State: HALF-OPEN
↓
Status: Testing if AI Service has recovered
↓
Behavior:
- Allows ONLY 3 test calls to the AI Service
- If any test succeeds → circuit CLOSES
- If any test fails → circuit reopens
- Prevents getting overwhelmed during recovery

Test Strategy:
Permit 3 calls to real service (slow probes)
If all 3 succeed → Service recovered → CLOSE circuit
If any 1 fails    → Service still down → reOPEN circuit
```

**Example Timeline:**
```
Time  | Event
------|------
10:14 | Circuit transitions to HALF-OPEN
10:15 | Test Call 1 → ✓ SUCCESS
10:15 | Test Call 2 → ✓ SUCCESS
10:16 | Test Call 3 → ✓ SUCCESS
10:16 | All tests passed! Circuit CLOSES again
```

OR

```
Time  | Event
------|------
10:14 | Circuit transitions to HALF-OPEN
10:15 | Test Call 1 → ✗ FAILURE
10:15 | Service still down! Circuit reopens
10:15 | Return to OPEN state (wait another 10 seconds)
```

---

## Our Implementation

### Architecture

```
MCPService
    ↓
AiServiceFeignClient (Feign HTTP Client)
    ↓
@CircuitBreaker Annotation
    ↓
Resilience4j Circuit Breaker Registry
    ↓
CircuitBreakerConfig (Spring Configuration)
    ↓
application.yml (Configuration Properties)
```

### Key Files Modified

#### 1. **CircuitBreakerConfig.java** - Configuration Bean
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(MeterRegistry meterRegistry) {
        // Creates the registry that manages all circuit breakers
        // Integrates with metrics for monitoring
    }
    
    @Bean
    public CircuitBreaker aiServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        // Creates specific circuit breaker for AI Service
        // Sets failure threshold to 50%
        // Sets wait duration to 10 seconds
    }
}
```

#### 2. **AiServiceFeignClient.java** - Feign Client with Protection
```java
@FeignClient(name = "ai-inference-service")
public interface AiServiceFeignClient {
    
    @PostMapping("/api/v1/analyze")
    @CircuitBreaker(name = "ai-service-circuit-breaker", 
                   fallbackMethod = "analyzeFallback")
    ResponseEntity<AnomalyDetectionResponse> analyze(
        @RequestBody AnomalyDetectionRequest request
    );
    
    // Fallback method called when circuit is OPEN
    default ResponseEntity<AnomalyDetectionResponse> analyzeFallback(
        AnomalyDetectionRequest request, Exception ex) {
        return ResponseEntity.ok(
            AnomalyDetectionResponse.builder()
                .isAnomaly(false)
                .confidenceScore(0.0)
                .patternDetected("SERVICE_UNAVAILABLE")
                .suggestedBlockMinutes(0)
                .build()
        );
    }
}
```

#### 3. **application.yml** - Configuration Properties
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50        # Open at 50% failure
        minimumNumberOfCalls: 5         # Evaluate after 5 calls
        waitDurationInOpenState: 10s    # Wait 10s before testing
        permittedNumberOfCallsInHalfOpenState: 3  # Test 3 calls
        slowCallDurationThreshold: 2s   # Calls > 2s are slow
        slowCallRateThreshold: 50       # Open at 50% slow calls
```

---

## Configuration Details

### Current Settings Explained

| Parameter | Value | Explanation |
|-----------|-------|-------------|
| **failureRateThreshold** | 50% | Open circuit if 50% of calls fail |
| **minimumNumberOfCalls** | 5 | Need at least 5 calls before evaluating |
| **waitDurationInOpenState** | 10s | Wait 10 seconds in OPEN state before trying again |
| **permittedNumberOfCallsInHalfOpenState** | 3 | Allow 3 test calls in HALF_OPEN state |
| **slowCallDurationThreshold** | 2s | Any call taking > 2s is considered slow |
| **slowCallRateThreshold** | 50% | Open at 50% slow calls |
| **automaticTransitionFromOpenToHalfOpenEnabled** | true | Auto-transition after wait duration |

### Configuration Tuning Guide

**For High-Traffic Services:**
```yaml
minimumNumberOfCalls: 10        # More calls before deciding
waitDurationInOpenState: 5s     # Faster retry
failureRateThreshold: 40        # More sensitive to failures
```

**For Resilient Services:**
```yaml
minimumNumberOfCalls: 3         # Quick decision making
waitDurationInOpenState: 30s    # Longer recovery time
failureRateThreshold: 60        # More tolerant of failures
```

---

## Monitoring & Health Checks

### 1. Access Health Endpoint
```bash
curl http://localhost:9991/mcp-service/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "circuitbreakers": {
      "status": "UP",
      "details": {
        "ai-service-circuit-breaker": {
          "status": "UP",
          "details": {
            "state": "CLOSED"
          }
        }
      }
    }
  }
}
```

### 2. Circuit Breaker Events
```bash
curl http://localhost:9991/mcp-service/actuator/circuitbreakerevents
```

**Response:**
```json
{
  "circuitBreakerEvents": [
    {
      "circuitBreakerName": "ai-service-circuit-breaker",
      "type": "STATE_TRANSITION",
      "creationTime": "2024-06-08T10:04:30Z",
      "stateTransition": "CLOSED -> OPEN"
    }
  ]
}
```

### 3. Metrics You Can Monitor

```
# How many requests succeeded
resilience4j_circuitbreaker_calls{kind="successful"} 45

# How many requests failed
resilience4j_circuitbreaker_calls{kind="failed"} 5

# Current state: 0=CLOSED, 1=OPEN, 2=HALF_OPEN
resilience4j_circuitbreaker_state 0

# How many times circuit changed states
resilience4j_circuitbreaker_state_transitions_total 2
```

---

## Scenarios & Examples

### Scenario 1: Normal Operation (CLOSED)
```
Time: 10:00-10:10

User Request 1 → AI Service ✓ Success
User Request 2 → AI Service ✓ Success
User Request 3 → AI Service ✓ Success
User Request 4 → AI Service ✓ Success
User Request 5 → AI Service ✓ Success

Circuit Status: CLOSED ✅
Failure Rate: 0%
Action: Continue normal operation
```

---

### Scenario 2: Failures Detected (CLOSED → OPEN)
```
Time: 10:20-10:25

User Request 1 → AI Service ✗ Timeout
User Request 2 → AI Service ✗ Timeout
User Request 3 → AI Service ✗ 500 Error
User Request 4 → AI Service ✗ 500 Error
User Request 5 → AI Service ✗ 500 Error

Failure Rate: 5/5 = 100% > 50% threshold
Minimum Calls Met: 5 ≥ 5

⚠️ CIRCUIT OPENS!
Circuit Status: OPEN 🔴
All future requests immediately rejected (fail fast)
```

---

### Scenario 3: Circuit Open - Fail Fast
```
Time: 10:25-10:30

User Request 1 → Circuit OPEN → Fallback Response (0ms)
User Request 2 → Circuit OPEN → Fallback Response (0ms)
User Request 3 → Circuit OPEN → Fallback Response (0ms)
User Request 4 → Circuit OPEN → Fallback Response (0ms)
User Request 5 → Circuit OPEN → Fallback Response (0ms)

Circuit Status: OPEN 🔴
Wait Duration: 10 seconds remaining
Action: Requests fail fast, return default response
```

---

### Scenario 4: Recovery Testing (OPEN → HALF-OPEN)
```
Time: 10:35 (10 seconds have passed)

Circuit transitions to HALF-OPEN
Now allowing 3 test calls to AI Service

Test Call 1 → AI Service ✓ Success
Test Call 2 → AI Service ✓ Success
Test Call 3 → AI Service ✓ Success

All tests passed! Service recovered!
Circuit Status: CLOSED ✅
```

---

### Scenario 5: Recovery Failed (HALF-OPEN → OPEN)
```
Time: 10:35 (10 seconds have passed)

Circuit transitions to HALF-OPEN
Now allowing 3 test calls to AI Service

Test Call 1 → AI Service ✗ Timeout

Service still down!
Circuit Status: OPEN 🔴
Wait another 10 seconds before retrying...
```

---

## Best Practices

### 1. **Fallback Responses**
Always provide sensible fallback values:
```java
// GOOD: Return safe default
default ResponseEntity<AnomalyDetectionResponse> analyzeFallback(...) {
    return ResponseEntity.ok(
        AnomalyDetectionResponse.builder()
            .isAnomaly(false)                    // Conservative: assume no anomaly
            .confidenceScore(0.0)                // No confidence in result
            .patternDetected("SERVICE_UNAVAILABLE")
            .suggestedBlockMinutes(0)            // Don't block without confirmation
            .build()
    );
}

// BAD: Throwing exception
default ResponseEntity<AnomalyDetectionResponse> analyzeFallback(...) {
    throw new RuntimeException("Service down");  // Causes cascading failures
}
```

### 2. **Monitoring**
Regularly check circuit breaker health:
```bash
# Watch for state transitions
curl -s http://localhost:9991/mcp-service/actuator/health | grep -A5 "circuitbreakers"

# Monitor in real-time
watch -n 2 'curl -s http://localhost:9991/mcp-service/actuator/health | jq'
```

### 3. **Alert on State Changes**
Set up alerts for:
- Circuit opens → Action required (service down)
- Frequent opens → Check configuration
- Stays open too long → Possible permanent failure

### 4. **Test Fallback Behavior**
```bash
# Stop AI Service
docker stop ai-service

# Make requests to MCPService
curl -X POST http://localhost:9991/mcp-service/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"endpoint":"/test","method":"GET"}'

# Should return fallback response immediately
# Check logs for circuit breaker state transitions
```

### 5. **Configuration Review**
Review and adjust based on:
- **Error Rate**: If too many false positives, increase threshold
- **Response Time**: If timeouts common, increase slow call threshold
- **Service Stability**: If service recovers fast, reduce wait duration

---

## Common Issues & Solutions

### Issue 1: Circuit Never Closes
**Problem**: Circuit stuck in OPEN state
```
Cause: Service still failing, no successful tests in HALF_OPEN
Solution: Check AI Service logs, verify it's actually running
```

### Issue 2: Circuit Opens Too Easily
**Problem**: Circuit opens even with minor failures
```
Cause: failureRateThreshold too low or minimumNumberOfCalls too low
Solution: Increase minimumNumberOfCalls or failureRateThreshold
```

### Issue 3: Clients Always Get Fallback
**Problem**: Circuit always returning default response
```
Cause: Circuit stuck in OPEN state
Solution: Verify AI Service is healthy, check wait duration
```

---

## Visual State Machine

```
                    ┌──────────────────────────────┐
                    │      CLOSED (Normal)         │
                    │   Requests flow normally     │
                    └──────────────────────────────┘
                              ▲          │
                              │          │ Failure rate > 50%
                              │          │ AND min calls reached
                              │          ▼
                    ┌──────────────────────────────┐
                    │  OPEN (Service Down)         │
                    │  All requests rejected       │
                    │  Wait 10 seconds             │
                    └──────────────────────────────┘
                              ▲          │
                              │          │ Wait duration elapsed
                              │          ▼
                    ┌──────────────────────────────┐
                    │  HALF-OPEN (Testing)         │
                    │  Allow 3 test calls          │
                    └──────────────────────────────┘
                              │          │
                 All succeed   │          │ Any failure
                              ▼          ▼
                          CLOSED      OPEN
```

---

## Summary

**Circuit Breaker in MCPService:**
1. **Protects** against cascading failures
2. **Fails Fast** when service is down
3. **Auto-Recovers** by testing service availability
4. **Monitored** via health and metrics endpoints
5. **Configurable** for different use cases

This implementation ensures MCPService remains stable even when AI Service experiences issues! 🚀
