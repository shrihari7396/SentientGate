# 🛡️ SentientGate UI: Sentinel Overwatch

A premium, high-performance monitoring and management dashboard for the **SentientGate** ecosystem. Built with cutting-edge technologies to provide real-time insights into your distributed gateway traffic, security posture, and infrastructure health.

---

## ✨ Features

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

---

## 🚀 Getting Started

### Prerequisites
- [Node.js](https://nodejs.org/) (v18 or higher)
- [npm](https://www.npmjs.com/) or [yarn](https://yarnpkg.com/)

### Development Setup
1. **Clone the repository** (if not already in the SentientGate workspace).
2. **Navigate to the UI folder**:
   ```bash
   cd UI/sentinel-gateway-ui
   ```
3. **Install dependencies**:
   ```bash
   npm install
   ```
4. **Configure Environment**:
   Create or edit `.env` file:
   ```env
   VITE_API_BASE_URL=http://localhost:8079/api
   ```
5. **Launch the command center**:
   ```bash
   npm run dev
   ```

### Production Build
To generate a production bundle:
```bash
npm run build
npm run preview
```

---

## 🏗️ Architecture

The project follows a **Feature-Driven** architecture for scalability:

```text
src/
├── app/          # Global providers, routing, and core App component
├── features/     # Feature-specific modules (Dashboard, Logs, Registry, etc.)
│   ├── [feature]/
│   │   ├── components/  # Feature-specific UI
│   │   └── hooks/       # Feature-specific logic
├── shared/       # Reusable components, API clients, and utilities
├── hooks/        # Global custom hooks
└── assets/       # Static branding and assets
```

---

## 📊 Current Status

- ✅ **UI Shell & Navigation**: Completed with Dark/Light mode support.
- ✅ **Dashboard Visualization**: Fully functional with live polling.
- ✅ **Logs Ledger**: High-performance table implementation with filtering.
- ✅ **Eureka Integration**: Service discovery viewer implemented.
- ✅ **Pipeline Overview**: Visualization of logic filters complete.
- 🟡 **Advanced Auth**: Preparation for OAuth2/JWT integration (interceptors ready).
- 🔵 **Infrastructure Detailed Views**: Kafka/Redis/Postgres specific detail views in progress.

---

*Designed with ❤️ by the SentientGate Team.*
