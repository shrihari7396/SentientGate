package edu.pict.loggingservice.service;

import edu.pict.loggingservice.dto.IpActivitySummary;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IpAggregationServiceTest {

    @Mock
    private GatewayLogRepository gatewayLogRepository;

    private IpAggregationService ipAggregationService;

    @BeforeEach
    void setUp() {
        ipAggregationService = new IpAggregationService(gatewayLogRepository);
    }

    @Test
    void summarize_ShouldReturnCorrectSummary() {
        String ip = "192.168.1.1";
        Instant start = Instant.now().minusSeconds(600);
        Instant end = Instant.now();

        Object[] mockResult = new Object[] {
                100L, // totalRequests
                5L, // rateLimitedCount
                2L, // invalidJwtCount
                10L, // uniqueRoutes
                50.5 // avgLatencyMs
        };

        when(gatewayLogRepository.aggregateByIp(eq(ip), any(), any())).thenReturn(mockResult);

        IpActivitySummary summary = ipAggregationService.summarize(ip, start, end);

        assertEquals(ip, summary.clientIp());
        assertEquals(100L, summary.totalRequests());
        assertEquals(5L, summary.rateLimitedCount());
        assertEquals(2L, summary.invalidJwtCount());
        assertEquals(10L, summary.uniqueRoutes());
        assertEquals(50.5, summary.avgLatencyMs());
    }

    @Test
    void summarize_ShouldHandleNullAvgLatency() {
        String ip = "192.168.1.1";
        Object[] mockResult = new Object[] { 0L, 0L, 0L, 0L, null };

        when(gatewayLogRepository.aggregateByIp(eq(ip), any(), any())).thenReturn(mockResult);

        IpActivitySummary summary = ipAggregationService.summarize(ip, Instant.now(), Instant.now());

        assertEquals(0.0, summary.avgLatencyMs());
    }
}
