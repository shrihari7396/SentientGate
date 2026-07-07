import { http, HttpResponse } from 'msw';

export const handlers = [
  // Dashboard handlers
  http.get('*/api/dashboard/metrics', () => {
    return HttpResponse.json({
      requestsPerMin: Math.floor(Math.random() * 5000) + 1000,
      blockedThreats: Math.floor(Math.random() * 50),
      p99Latency: (Math.random() * 20 + 40).toFixed(1),
      activeServices: 14,
    });
  }),

  http.get('*/api/dashboard/traffic', () => {
    // Generate 20 buckets
    const data = Array.from({ length: 20 }).map((_, i) => {
      const d = new Date();
      d.setMinutes(d.getMinutes() - (19 - i));
      return {
        timestamp: d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        coreFlow: Math.floor(Math.random() * 1000) + 500,
        threatVectors: Math.floor(Math.random() * 100),
      };
    });
    return HttpResponse.json(data);
  }),

  http.get('*/api/dashboard/threat-dist', () => {
    return HttpResponse.json([
      { type: 'Rate Anomaly', count: 120 },
      { type: 'Pattern Repeat', count: 85 },
      { type: 'Suspicious Access', count: 45 },
      { type: 'AI Escalation', count: 12 },
    ]);
  }),

  http.get('*/api/dashboard/recent-blocks', () => {
    return HttpResponse.json([
      { uuid: 'a1b2c3d4e5f6', reason: 'AI_ESCALATION', blockedAt: new Date().toISOString(), ttlSeconds: 300 },
      { uuid: 'x9y8z7w6v5u4', reason: 'RATE_ANOMALY', blockedAt: new Date(Date.now() - 60000).toISOString(), ttlSeconds: 240 },
    ]);
  }),

  // Add generic handlers so nothing crashes for now
  http.get('*/api/threat/stats', () => {
    return HttpResponse.json({
      blockedToday: 342,
      aiEscalations: 12,
      avgAnomalyScore: 0.23,
      strategiesActive: 4
    });
  }),

  http.get('*/api/threat/strategies', () => {
    return HttpResponse.json([
      { id: '1', name: 'RateAnomalyStrategy', description: 'Detects high volume burst requests', enabled: true, lastFired: '2m ago', firesToday: 142 },
      { id: '2', name: 'PatternRepeatStrategy', description: 'Detects repetitive payload structures', enabled: true, lastFired: '45m ago', firesToday: 32 },
      { id: '3', name: 'JwtTamperStrategy', description: 'Detects signature manipulation attempts', enabled: false, lastFired: 'Never', firesToday: 0 },
    ]);
  }),

  http.post('*/api/threat/strategies/:id/toggle', () => {
    return HttpResponse.json({ success: true });
  }),

  http.get('*/api/threat/feed', () => {
    const feed = Array.from({ length: 8 }).map((_, i) => ({
      uuid: `u-8a7b6c5d4e3f2${i}`,
      timestamp: new Date(Date.now() - i * 15000).toISOString(),
      source: i % 3 === 0 ? 'AI_MODEL' : 'HEURISTIC',
      strategyFired: i % 3 !== 0 ? 'RateAnomalyStrategy' : undefined,
      anomalyScore: i % 3 === 0 ? 0.85 + Math.random() * 0.1 : 0.4 + Math.random() * 0.4,
      decision: i === 0 ? 'BLACKLISTED' : (i % 2 === 0 ? 'MONITORING' : 'ALLOWED'),
      reasoningText: i % 3 === 0 ? 'LLM flagged rapid switching of Authorization headers across IP range.' : undefined
    }));
    return HttpResponse.json(feed);
  }),

  // Comment out to integrate with live backend via ApiGateway
  // http.get('*/api/threat/blacklist', () => {
  //   return HttpResponse.json([
  //     { uuid: 'u-8a7b6c5d4e3f20', reason: 'AI_ESCALATION', blockedAt: new Date().toISOString(), ttlSeconds: 432 },
  //     { uuid: 'u-abc123xyz8901', reason: 'RateAnomalyStrategy', blockedAt: new Date(Date.now() - 100000).toISOString(), ttlSeconds: 22 },
  //   ]);
  // }),
  //
  // http.delete('*/api/threat/blacklist/:uuid', () => {
  //   return HttpResponse.json({ success: true });
  // }),

  // Comment out to integrate with live backend via ApiGateway
  // // Logs
  // http.get('*/api/logs', ({ request }) => {
  //   const url = new URL(request.url);
  //   const path = url.searchParams.get('path') || '';
  //   const status = url.searchParams.get('status') || '';
  // 
  //   let feed = Array.from({ length: 50 }).map((_, i) => {
  //     const isThreat = Math.random() > 0.8;
  //     const sCode = status ? (status === '2xx' ? 200 : status === '4xx' ? 403 : 500) : (isThreat ? 403 : 200);
  //     return {
  //       id: `req-${Date.now()}-${i}`,
  //       timestamp: new Date(Date.now() - i * 1234).toISOString(),
  //       uuid: `u-user${Math.floor(Math.random() * 1000)}xyz`,
  //       method: ['GET', 'POST', 'DELETE'][Math.floor(Math.random() * 3)],
  //       endpoint: path || (['/api/v1/users', '/api/v1/payments', '/api/v1/auth/login'][Math.floor(Math.random() * 3)]),
  //       status: sCode,
  //       latency: Math.floor(Math.random() * 800),
  //       routeId: `route-${Math.floor(Math.random() * 10)}`,
  //       threatFlagged: isThreat,
  //     };
  //   });
  // 
  //   return HttpResponse.json({ content: feed, totalElements: 50, totalPages: 1 });
  // }),

  // Pipeline
  http.get('*/api/pipeline/stats', () => {
    return HttpResponse.json([
      { stage: 'Request In', requestsToday: 145023, status: 'UP', lastError: null },
      { stage: 'Edge Fire', requestsToday: 145023, status: 'DOWN', lastError: 'Redis connection timeout' },
      { stage: 'JTI Vault', requestsToday: 144900, status: 'UP', lastError: null },
      { stage: 'Rate Pulse', requestsToday: 142000, status: 'UP', lastError: null },
      { stage: 'Shadow Log', requestsToday: 139000, status: 'UP', lastError: null },
      { stage: 'Response Out', requestsToday: 139000, status: 'UP', lastError: null },
    ]);
  }),

  http.get('*/api/pipeline/events', () => {
    const events = Array.from({ length: 15 }).map((_, i) => {
      const passed = Math.random() > 0.3;
      return {
        id: `ev-${Date.now()}-${i}`,
        timestamp: new Date(Date.now() - i * 5000).toISOString(),
        uuid: `u-${Math.floor(Math.random() * 10000)}`,
        status: passed ? 'PASSED' : 'BLOCKED',
        stage: passed ? 'Response Out' : ['Edge Fire', 'Rate Pulse', 'JTI Vault'][Math.floor(Math.random() * 3)]
      };
    });
    return HttpResponse.json(events);
  }),

  // Registry
  http.get('*/api/registry/services', () => {
    return HttpResponse.json([
      { id: 'sentient-gateway', name: 'sentient-gateway', instanceCount: 3, status: 'UP' },
      { id: 'auth-service', name: 'auth-service', instanceCount: 2, status: 'UP' },
      { id: 'payment-service', name: 'payment-service', instanceCount: 4, status: 'DOWN' },
      { id: 'mcp-orchestrator', name: 'mcp-orchestrator', instanceCount: 1, status: 'UP' },
    ]);
  }),

  http.get('*/api/registry/services/:id', ({ params }) => {
    return HttpResponse.json([
      { host: `10.0.1.5`, port: 8080, status: params.id === 'payment-service' ? 'DOWN' : 'UP', homepage: `http://10.0.1.5:8080`, lastHeartbeat: new Date().toISOString() },
      { host: `10.0.1.6`, port: 8080, status: params.id === 'payment-service' ? 'DOWN' : 'UP', homepage: `http://10.0.1.6:8080`, lastHeartbeat: new Date(Date.now() - 5000).toISOString() },
    ]);
  }),

  http.get('*/api/registry/actuator/:id/health', () => {
    return HttpResponse.json({ status: "UP", components: { db: { status: "UP", details: { database: "PostgreSQL" } }, redis: { status: "UP" } } });
  }),

  // Infra
  http.get('*/api/infra/kafka', () => {
    return HttpResponse.json({
      status: 'UP',
      topics: [
        { name: 'security-audit-events', partitions: 12, lag: 45, throughput: 1240 },
        { name: 'gateway-metrics', partitions: 6, lag: 0, throughput: 850 },
      ],
      recentAudit: Array.from({ length: 20 }).map((_, i) => `[Event-K${i}] Threat signature detected on Edge Fire. UUID=u-${i}xyz`)
    });
  }),
  http.get('*/api/infra/redis', () => {
    return HttpResponse.json({
      status: 'UP',
      latency: 4.2,
      memoryUsed: '2.4 GB',
      memoryMax: '4.0 GB',
      blacklistKeys: 1450,
      tokenBuckets: 142000,
      hitRatio: 0.94
    });
  }),
  http.get('*/api/infra/postgres', () => {
    return HttpResponse.json({
      status: 'UP',
      activeConnections: 45,
      idleConnections: 12,
      maxConnections: 100,
      totalIdentityRecords: 4500000,
      lastWrite: new Date().toISOString()
    });
  }),

  // User Context
  http.get('*/api/users/:uuid/context', ({ params }) => {
    return HttpResponse.json({
      uuid: params.uuid,
      status: 'MONITORING',
      timeline: Array.from({ length: 20 }).map((_, i) => ({
        timestamp: new Date(Date.now() - (20 - i) * 30000).toISOString(),
        requestCount: Math.floor(Math.random() * 50)
      })),
      recentRequests: Array.from({ length: 15 }).map((_, i) => ({
        timestamp: new Date(Date.now() - i * 15000).toISOString(),
        method: 'GET',
        endpoint: '/api/v1/auth/verify',
        status: 403,
        threat: true
      })),
      activeStrategies: ['RateAnomalyStrategy', 'PatternRepeatStrategy'],
      blacklistInfo: null,
      aiProfile: {
        lastAssessment: 'User behavior indicates rapid, sequential token validations typical of credential stuffing attacks.',
        anomalyScores: Array.from({ length: 10 }).map((_, i) => ({ timestamp: `t-${i}`, score: 0.6 + Math.random() * 0.3 }))
      }
    });
  })
];
