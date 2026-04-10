# SentientGate UI — Master Build Prompt
> Hand this entire document to your AI agent (Antigravity / Cursor / v0 / etc.)
> It contains full context, design spec, screen-by-screen requirements, and implementation rules.

---

## 0. What You Are Building

**SentientGate Sentinel Overwatch** — a production-grade monitoring and management dashboard
for an AI-powered adaptive security API gateway.

The system is a Spring Boot WebFlux gateway that:
- Intercepts every incoming HTTP request
- Checks a Redis blacklist for blocked UUIDs
- Emits Kafka events for async logging and threat analysis
- Runs an MCP (strategy orchestrator) that fires rule-based detection strategies
- Escalates to an Ollama local LLM for AI-based anomaly scoring
- Dynamically blacklists malicious users in Redis with TTL enforcement

**This UI must make all of that visible, live, and beautiful.**

---

## 1. Tech Stack (Do Not Deviate)

| Concern | Library |
|---|---|
| Framework | React 19 (hooks-first, functional only) |
| Build | Vite |
| Server state | TanStack Query v5 (useQuery, polling via refetchInterval) |
| Tables | TanStack Table v8 (headless) |
| Styling | Tailwind CSS (utility-first, no custom CSS files) |
| Animation | Framer Motion |
| Icons | Lucide React |
| Charts | Recharts |
| HTTP | Axios (centralised client at `src/shared/api/client.ts`) |
| Routing | React Router v6 |

**Base API URL:** read from `import.meta.env.VITE_API_BASE_URL` (default: `http://localhost:8079/api`)

---

## 2. Design Language

### Aesthetic Direction
**Cyberpunk-Ops / Dark Command Center** — refined, not garish.
Think: Vercel dashboard × security operations center.
- Dark as default. Muted slate backgrounds, NOT pure black.
- Accent color: Electric teal `#00E5CC` (primary) + Amber `#F59E0B` (warning/anomaly) + Red `#EF4444` (threat/blocked)
- Subtle grid lines in backgrounds (opacity 4–6%)
- Text: `JetBrains Mono` for data/numbers, `Inter` for labels/prose (both from Google Fonts)
- No rounded pill buttons everywhere — use sharp-cornered `rounded-sm` or `rounded` only

### Color Tokens (Tailwind extend or CSS vars)
```
--bg-base:     #0D1117   (page background)
--bg-surface:  #161B22   (card / panel background)
--bg-elevated: #1C2128   (nested card, input bg)
--border:      #30363D   (all borders)
--text-primary:#E6EDF3
--text-muted:  #7D8590
--accent-teal: #00E5CC
--accent-amber:#F59E0B
--accent-red:  #EF4444
--accent-blue: #58A6FF
```

### Typography Rules
- All numbers/metrics → `font-mono`
- All labels/headings → `font-sans` (Inter)
- Metric values → `text-3xl font-mono font-semibold`
- Section labels → `text-xs font-semibold tracking-widest uppercase text-muted`
- Table cells → `text-sm font-mono`

### Motion Rules (Framer Motion)
- Page entry: `initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.25 }}`
- Cards stagger: use `staggerChildren: 0.05` on container
- Live data update flash: on new data, briefly flash the changed cell/card with teal `#00E5CC` at 30% opacity for 400ms
- No bouncing, no spring physics — `ease: "easeOut"` only

---

## 3. Project Structure

```
src/
├── app/
│   ├── App.tsx              # Router + QueryClient + Layout wrapper
│   ├── providers.tsx        # QueryClientProvider, ThemeProvider
│   └── routes.tsx           # All route definitions
├── features/
│   ├── dashboard/
│   │   ├── components/      # MetricCard, ThreatChart, TrafficChart, LatencyGauge
│   │   └── hooks/           # useDashboardMetrics.ts
│   ├── logs/
│   │   ├── components/      # LogsTable, LogFilters, LogDetailDrawer
│   │   └── hooks/           # useLogs.ts
│   ├── pipeline/
│   │   └── components/      # PipelineStep, PipelineFlow
│   ├── registry/
│   │   ├── components/      # ServiceCard, NodeList, ActuatorPanel
│   │   └── hooks/           # useEurekaServices.ts
│   ├── threat/              # NEW — AI/Security layer
│   │   ├── components/      # AnomalyFeed, StrategyFireCard, BlacklistTable, UserContextDrawer
│   │   └── hooks/           # useThreatFeed.ts, useBlacklist.ts
│   └── infra/               # NEW — Infra detail views
│       ├── components/      # KafkaPanel, RedisPanel, PostgresPanel
│       └── hooks/           # useInfraHealth.ts
├── shared/
│   ├── api/
│   │   └── client.ts        # Axios instance with base URL + interceptors
│   ├── components/
│   │   ├── Layout.tsx       # Sidebar + topbar shell
│   │   ├── Sidebar.tsx
│   │   ├── StatusBadge.tsx  # Reusable status pill
│   │   ├── MetricCard.tsx   # Reusable metric surface card
│   │   └── LiveDot.tsx      # Pulsing green dot for live indicators
│   └── hooks/
│       └── usePolling.ts    # Wrapper for TanStack Query polling
└── assets/
    └── logo.svg
```

---

## 4. Screen-by-Screen Requirements

---

### Screen 1: Dashboard (`/`)

**Purpose:** Real-time command center overview.

**Layout:** Top row of 4 metric cards → middle: traffic chart (left 60%) + threat distribution (right 40%) → bottom: P99 latency sparkline + recent blocked events list.

**Metric Cards (top row) — poll every 3s:**
| Card | Value | Accent color |
|---|---|---|
| Requests / min | integer | teal |
| Blocked threats | integer | red |
| P99 Latency | ms, 1 decimal | amber |
| Active services | integer | blue |

**Traffic Velocity Chart (Recharts AreaChart):**
- X-axis: last 20 time buckets (1-min windows), label as `HH:mm`
- Two area series: `Core Flow` (teal, 40% fill opacity) and `Threat Vectors` (red, 30% fill opacity)
- No grid lines on chart — use subtle dot pattern background on the card instead
- Poll every 5s

**Threat Distribution (Recharts PieChart / RadialBarChart):**
- Show breakdown: Rate Anomaly / Pattern Repeat / Suspicious Access / AI Escalation
- Colors: amber, red, blue, purple
- Center label: total threats today

**Recent Blocked Events (bottom right):**
- Last 5 blocked UUIDs with timestamp, reason, TTL remaining
- Each row: monospace UUID (truncated to 12 chars + `…`), reason badge, countdown timer that ticks

**API endpoints to call:**
```
GET /api/dashboard/metrics       → { requestsPerMin, blockedThreats, p99Latency, activeServices }
GET /api/dashboard/traffic       → [{ timestamp, coreFlow, threatVectors }]  (last 20 buckets)
GET /api/dashboard/threat-dist   → [{ type, count }]
GET /api/dashboard/recent-blocks → [{ uuid, reason, blockedAt, ttlSeconds }]
```

---

### Screen 2: Traffic Ledger (`/logs`)

**Purpose:** Full audit trail of every request.

**Layout:** Filter bar (top) → full-width table → row click opens detail drawer (slides in from right, 480px wide).

**Filter Bar:**
- Text input: filter by endpoint path (debounce 300ms)
- Select: Status code group — All / 2xx / 4xx / 5xx
- Select: Client UUID (free text search)
- Toggle: **Live Mode** — when ON, poll every 2s, prepend new rows with a teal flash, auto-scroll to top
- Clear filters button

**Table columns (TanStack Table):**
| Column | Width | Notes |
|---|---|---|
| Timestamp | 140px | `YYYY-MM-DD HH:mm:ss.SSS` monospace |
| UUID | 160px | first 12 chars + `…`, click copies full UUID |
| Method | 70px | colored badge: GET=blue, POST=teal, DELETE=red |
| Endpoint | — | flex grow, truncate with tooltip on hover |
| Status | 80px | colored badge: 2xx=green, 4xx=amber, 5xx=red |
| Latency | 90px | `XXXms` monospace, color: <100ms green, 100-500ms amber, >500ms red |
| Route ID | 120px | monospace |
| Threat | 80px | if flagged: red `THREAT` badge; else `—` |

**Row detail drawer:**
- Full UUID (copyable)
- Full endpoint path
- All request headers (collapsed by default, expandable)
- Kafka event ID that was emitted (if available)
- Threat analysis result (if any): strategy that fired, AI anomaly score, final decision
- Redis blacklist status: is this UUID currently blacklisted? TTL?

**API endpoints:**
```
GET /api/logs?path=&status=&uuid=&page=&size=20   → paginated log entries
GET /api/logs/:id                                  → single log detail
```

---

### Screen 3: Execution Pipeline (`/pipeline`)

**Purpose:** Visual, live representation of the gateway filter chain.

**Layout:** Horizontal flow of 5 pipeline stages. Below each stage: live stats for that stage. A live event ticker at the bottom shows requests flowing through in real time.

**Pipeline Stages (left to right):**

```
[Request In] → [Edge Fire] → [JTI Vault] → [Rate Pulse] → [Shadow Log] → [Response Out]
```

Each stage box shows:
- Stage name (large, monospace)
- One-line description (small, muted)
- Live stat: requests processed by this stage today
- Status indicator: green dot (healthy) / amber (degraded) / red (error)
- On hover: expand to show last 5 events that passed/failed at this stage

**Animated request flow:**
- Every 2–3 seconds, an animated dot travels left-to-right across the pipeline
- If the request is clean: teal dot that completes the full journey
- If blocked at a stage: red dot that stops and pulses at the blocking stage, then fades
- Implement using Framer Motion `animate` on x position

**Bottom event ticker:**
- Scrolling horizontally (CSS marquee-style or JS): `[HH:mm:ss] UUID=xxxx → BLOCKED at Rate Pulse | [HH:mm:ss] UUID=yyyy → PASSED`
- Color coded: green for pass, red for block

**API endpoints:**
```
GET /api/pipeline/stats    → [{ stage, requestsToday, status, lastError }]
GET /api/pipeline/events   → last 20 pipeline events (SSE or poll every 2s)
```

---

### Screen 4: Service Registry (`/registry`)

**Purpose:** View all microservices registered in Netflix Eureka.

**Layout:** Header with total service count + healthy/unhealthy split → grid of service cards → each card expandable to show instance list.

**Service Card:**
- Service name (large)
- App ID from Eureka
- Instance count badge (e.g., `3 instances`)
- Health status: green (all UP) / amber (some DOWN) / red (all DOWN)
- On expand: list each instance with:
  - Host:Port
  - Status (UP / DOWN / STARTING)
  - Homepage URL (clickable)
  - Actuator health link (clickable, opens new tab)
  - Last heartbeat timestamp

**Actuator Panel (right sidebar, opens on service click):**
- `/actuator/health` → health tree rendered as expandable JSON tree
- `/actuator/metrics` → key metrics listed as stat rows
- `/actuator/info` → build info table

**API endpoints:**
```
GET /api/registry/services          → Eureka app list
GET /api/registry/services/:id      → single service instances
GET /api/registry/actuator/:id/health
GET /api/registry/actuator/:id/metrics
```

---

### Screen 5: Threat Intelligence (`/threat`) ⭐ MOST IMPORTANT

**Purpose:** Real-time view of the AI security brain. This is the flagship screen.

**Layout (3-column):**
- Left column (30%): Strategy Console — list of active detection strategies with toggle + last fire time
- Center column (40%): Live Anomaly Feed — scrolling card feed of threat events as they happen
- Right column (30%): Blacklist Manager — current Redis blacklist entries

**Left: Strategy Console**
Each strategy row:
- Strategy name (e.g., `RateAnomalyStrategy`, `PatternRepeatStrategy`)
- Toggle switch: enable/disable
- Last triggered: relative time (e.g., `2m ago`)
- Total fires today: number badge
- Short description of what it detects

**Center: Live Anomaly Feed (auto-scroll, newest at top)**
Each feed card shows:
- UUID (monospace, truncated)
- Timestamp
- Detection source: `HEURISTIC` (amber) or `AI_MODEL` (purple)
- Strategy that fired (if heuristic)
- Ollama anomaly score: 0.0–1.0 shown as a progress bar (green→amber→red gradient)
- Final decision: `BLACKLISTED` (red badge) or `ALLOWED` (green badge) or `MONITORING` (amber badge)
- Click to expand: full MCP context, AI reasoning text, Kafka event ID

**Right: Blacklist Manager**
Table of current Redis blacklist:
| Column | Notes |
|---|---|
| UUID | monospace, truncated |
| Reason | strategy name or `AI_ESCALATION` |
| Blocked at | timestamp |
| TTL remaining | live countdown in `Xm Xs` format, turns red when <60s |
| Action | `Unblock` button with confirm dialog |

**Global threat stats bar (full-width, below header):**
- Blocked today / AI escalations today / Avg anomaly score / Strategies active

**API endpoints:**
```
GET  /api/threat/strategies              → list of strategies + enabled status + stats
POST /api/threat/strategies/:id/toggle   → enable/disable
GET  /api/threat/feed                    → last 50 anomaly events (poll every 2s)
GET  /api/threat/blacklist               → current blacklist entries
DELETE /api/threat/blacklist/:uuid       → unblock a UUID
GET  /api/threat/stats                   → global threat stats
```

---

### Screen 6: Infrastructure (`/infra`)

**Purpose:** Deep health monitoring of Kafka, Redis, PostgreSQL.

**Layout:** Tab bar (Kafka / Redis / PostgreSQL) → tab-specific panel below.

**Kafka Tab:**
- Cluster status badge (healthy / degraded)
- Topics table: topic name, partition count, message lag, throughput (msg/s)
- Recent audit messages: scrolling list of last 20 security audit events from the Kafka topic
- Producer / Consumer group status

**Redis Tab:**
- Connection status + ping latency
- Memory usage bar: used / max
- Blacklist key count (from `blacklist:*` keyspace)
- Token bucket key count (from `ratelimit:*` keyspace)
- Top 10 keys by access frequency (if available from Redis INFO)
- Hit/miss ratio as a donut chart

**PostgreSQL Tab:**
- Connection pool: active / idle / max
- Identity store: total records, last write timestamp
- Replication status (if applicable): primary/replica with lag in ms
- Recent slow queries list (if available)

**API endpoints:**
```
GET /api/infra/kafka
GET /api/infra/redis
GET /api/infra/postgres
```

---

### Screen 7: User Context Viewer (drawer, not a page)

**Trigger:** Click any UUID anywhere in the app.

**Behaviour:** A full-height drawer slides in from the right (600px wide). Shows:
- UUID header (copyable)
- Current status: CLEAN / MONITORING / BLACKLISTED
- Request timeline: last 10 minutes of activity as a small timeline chart (requests per 30s bucket)
- Full request list: paginated table of all requests from this UUID in last 10 mins
- Active strategies that have flagged this UUID
- If blacklisted: reason, when, TTL remaining, Unblock button
- AI profile: anomaly score over time (line chart), last AI assessment text

**API endpoint:**
```
GET /api/users/:uuid/context    → full context object
```

---

## 5. Shared Components

### `<StatusBadge status="BLOCKED | ALLOWED | MONITORING | UP | DOWN" />`
- BLOCKED / DOWN → red bg, red text
- ALLOWED / UP → green bg, green text
- MONITORING → amber bg, amber text
- Tailwind: `text-xs font-mono font-semibold px-2 py-0.5 rounded-sm`

### `<LiveDot active={boolean} />`
- A 6px circle, green when active
- CSS pulse animation via `animate-ping` (Tailwind) on a pseudo-element
- Shows "LIVE" label to the right in teal, `text-xs font-mono`

### `<MetricCard label="" value="" unit="" trend="" accentColor="" />`
- `bg-surface` card, 1px `border-border`
- Label: `text-xs uppercase tracking-widest text-muted`
- Value: `text-3xl font-mono font-semibold` in `accentColor`
- Unit: `text-sm text-muted` inline after value
- Trend: optional small sparkline (5 data points, 40px wide, 20px tall)
- On value change: brief background flash in accent color at 20% opacity

### `<CopyableUUID uuid="" maxChars={12} />`
- Shows truncated UUID
- Click → copies full UUID to clipboard
- Show a checkmark icon for 1.5s after copy

### `<TTLCountdown ttlSeconds={number} />`
- Live countdown: `Xm Xs`
- Below 60s: turns red
- At 0: shows `EXPIRED` badge

### `usePolling(queryKey, fetchFn, intervalMs)`
Wrapper around TanStack Query's `useQuery` with `refetchInterval: intervalMs`.
Returns `{ data, isLoading, isError, lastUpdated }`.

---

## 6. API Client Setup

```typescript
// src/shared/api/client.ts
import axios from 'axios';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8079/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor: attach JWT if present in localStorage
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('sg_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: global error handling
apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      // redirect to /login
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);
```

---

## 7. Error & Empty States

Every screen must handle three states:

**Loading:** Skeleton shimmer (`animate-pulse bg-elevated`) matching the layout of the actual content. Not a spinner in the center of the screen.

**Error:** A muted error card inside the content area. Show: icon (AlertTriangle from Lucide), short message, a Retry button that calls `refetch()`.

**Empty:** An empty state illustration (SVG inline, simple) + helpful message. E.g., "No threats detected — gateway is clean" with a shield icon.

---

## 8. Navigation Sidebar

**Fixed left sidebar, 220px wide, `bg-surface` with right border.**

Nav items:
```
[Logo]  SentientGate

[LayoutDashboard]  Dashboard        /
[ScrollText]       Traffic Ledger   /logs
[GitBranch]        Pipeline         /pipeline
[Globe]            Service Registry /registry
[ShieldAlert]      Threat Intel     /threat      ← accent teal, always highlighted
[Server]           Infrastructure   /infra

[bottom]
[Settings]         Settings         /settings
[Moon/Sun]         Theme toggle
```

Active state: teal left border `border-l-2 border-accent-teal`, teal text.
Hover: `bg-elevated`.

Live indicators:
- Threat Intel nav item: show a small pulsing red dot badge if `blockedThreats > 0` in last 60s
- Traffic Ledger: show count badge of new logs since last visit

---

## 9. Data Mocking (Dev Mode)

All API calls should have mock data fallbacks when `VITE_MOCK=true` in `.env`.

Use `msw` (Mock Service Worker) for dev mocking. Create handlers in `src/mocks/handlers.ts` for every endpoint listed above. Make mock data realistic — generate random UUIDs, vary latency values, simulate occasional threats arriving.

This allows full UI development without the backend running.

---

## 10. Performance Requirements

- No page should take more than 200ms to first meaningful paint
- Polling must stop when the tab is hidden (`document.visibilityState === 'hidden'`) — use TanStack Query's `refetchIntervalInBackground: false`
- Tables with >1000 rows must use virtual scrolling (`@tanstack/react-virtual`)
- Charts must debounce re-renders on rapid polling updates — use a 500ms debounce before passing new data to Recharts

---

## 11. What NOT to Do

- Do NOT use Material UI, Chakra, Ant Design, or any component library — build from Tailwind + primitives
- Do NOT use purple gradients on white — this is a dark ops dashboard
- Do NOT use Inter for numbers — always `font-mono`
- Do NOT put spinners in the center of full pages — use skeleton loaders
- Do NOT make the Threat Intel screen generic — it must feel like a real security operations center
- Do NOT abbreviate UUID display to just 4 chars — use 12 chars minimum
- Do NOT use `any` types in TypeScript — all API responses must be typed

---

## 12. Deliverables

Build the following in order:

1. Project scaffold (Vite + React 19 + Tailwind + all deps installed)
2. Design system: CSS variables, Tailwind config, shared components
3. App shell: sidebar, routing, layout
4. Screen 1: Dashboard (with mock data)
5. Screen 5: Threat Intelligence (highest priority feature screen)
6. Screen 2: Traffic Ledger
7. Screen 3: Execution Pipeline
8. Screen 4: Service Registry
9. Screen 6: Infrastructure
10. User Context Drawer (shared, used across screens)
11. MSW mock handlers for all endpoints
12. README with setup instructions

---

*Built for SentientGate — AI-Powered Adaptive Security Gateway*
*Backend: Spring Boot WebFlux + Kafka + Redis + Ollama + MCP*
