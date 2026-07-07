# 🛡️ SentientGate UI: Sentinel Overwatch

A premium, high-performance monitoring and management dashboard for the **SentientGate** ecosystem. Built with cutting-edge technologies to provide real-time insights into your distributed gateway traffic, security posture, and infrastructure health.

---

## ✨ Core Features

### 📊 Network OS (Dashboard)
The core command center providing high-level telemetry:
- **Real-time Throughput**: Live tracking of request velocity.
- **Security Overwatch**: Immediate visualization of blocked threats and anomalous patterns.
- **P99 Latency Tracking**: Critical performance monitoring with neural-inspired visuals.
- **Traffic Velocity Charts**: Distribution of "Core Flow" vs. "Threat Vectors" in 1-minute windows.

### 📜 Traffic Ledger (Logs)
A comprehensive audit trail for every bit navigating the SentientGate:
- **Deep Filtering**: Filter by endpoint path, status codes (2xx, 4xx, 5xx), and client IDs.
- **Live Mode**: Real-time log streaming via asynchronous polling.
- **Execution Metadata**: Detailed breakdown of latency, route IDs, and transaction details.

### 🚀 Execution Pipeline
A visual guide to the SentientGate logic sequence:
- **Edge Fire**: Real-time identity isolation.
- **JTI Vault**: Cryptographic session integrity validation.
- **Rate Pulse**: Distributed rate limiting enforcement.
- **Shadow Log**: Asynchronous telemetry streaming.

### 🌐 Service Registry
Deep integration with **Netflix Eureka** and **Spring Boot Actuator**:
- **Discovery**: Auto-discovery of registered microservices.
- **Node Topology**: Monitor multiple instances, their health status, and homepages.
- **Actuator Telemetry**: Direct access to underlying service metrics.

### 🛠️ Infrastructure Fabric
Focused monitoring for mission-critical sub-systems:
- **Kafka Cluster**: Monitor streaming integrity and audit trails.
- **Redis Cache**: Track token buckets and session persistence performance.
- **PostgreSQL**: Persistence layer health and replicated identity monitoring.

---

## 🛠️ Technology Stack

SentientGate UI leverages a modern, reactive stack for a fluid user experience:

- **Framework**: [React 19](https://react.dev/) (Hooks-first, functional architecture)
- **Build Tool**: [Vite](https://vitejs.dev/) (Lightning-fast HMR and bundling)
- **State Management**: [TanStack Query v5](https://tanstack.com/query) (Powerful server-state management with polling)
- **Data Display**: [TanStack Table v8](https://tanstack.com/table) (Headless high-performance tables)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/) (Atomic CSS for premium design tokens)
- **Animations**: [Framer Motion](https://www.framer.com/motion/) (Fluid transitions and micro-interactions)
- **Visuals**: [Lucide React](https://lucide.dev/) & [Recharts](https://recharts.org/) (Sleek icons and responsive charts)
- **Networking**: [Axios](https://axios-http.com/) (Centralized API client with interceptors)
- **Mocks**: [Mock Service Worker (MSW)](https://mswjs.io/) (Local REST mock engine for decoupled frontend prototyping)

---

## 🚀 Mocks vs. Live Integration Mode

By default, the frontend starts with **Mock Service Worker (MSW)** enabled during local development (`npm run dev`). This allows prototyping the dashboard and telemetry UI screens without running the full microservice cluster.

### Switching to Live Backend Integration
To connect the UI to the actual **ApiGateway** on port `8079`:

1. **Modify the Environment File:**
   Ensure `UI/sentinel-gateway-ui/.env` points to the ApiGateway base URL:
   ```env
   VITE_API_BASE_URL=http://localhost:8079/api
   ```

2. **Disable MSW Mocking:**
   Go to `src/main.tsx` and disable the worker invocation:
   ```typescript
   // To disable mocking, comment out the worker start logic or set import.meta.env.DEV to false:
   async function deferRender() {
     /* Comment out or delete this block to connect to the live ApiGateway */
     // if (!import.meta.env.DEV) {
     //   return;
     // }
     // const { worker } = await import('./mocks/browser');
     // return worker.start({ onUnhandledRequest: 'bypass' });
     return;
   }
   ```

3. **Re-run the Frontend:**
   ```bash
   npm run dev
   ```

---

## 🏗️ Getting Started (Local Development)

### Prerequisites
- [Node.js](https://nodejs.org/) (v18 or higher)
- [npm](https://www.npmjs.com/)

### Setup
1. Navigate to the UI folder:
   ```bash
   cd UI/sentinel-gateway-ui
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite server:
   ```bash
   npm run dev
   ```
   The UI will be available at `http://localhost:5173`.

### Docker Deployment
The UI can be containerized and run via Nginx:
```bash
# Build the Docker image
docker build -t sentientgate-ui .

# Run the container mapping to port 5173
docker run -p 5173:80 sentientgate-ui
```

---

## 📊 Current Status & Integration Roadmap

- ✅ **UI Shell & Navigation**: Completed with Dark/Light mode support.
- ✅ **Dashboard Visualization**: Fully functional with live polling.
- ✅ **Logs Ledger**: High-performance table implementation with filtering.
- ✅ **Eureka Integration**: Service discovery viewer implemented.
- ✅ **Pipeline Overview**: Visualization of logic filters complete.
- 🟡 **Service Integration**: The frontend is fully decoupled and relies on MSW mocks for metrics/stats/infrastructure routes. A backend BFF (Backend-For-Frontend) or direct endpoint expansion on `ApiGateway`/`LoggingService` is required to decommission MSW completely.
