package edu.pict.loggingservice.service.strategy;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogFetchStrategyResolverTest {

    @Mock
    private RedisFetchStrategy redisFetchStrategy;

    @Mock
    private DatabaseFetchStrategy databaseFetchStrategy;

    @InjectMocks
    private LogFetchStrategyResolver strategyResolver;

    @Test
    void testFetchLogs_FastPath_RedisHits() {
        String uuid = "test-uuid";
        Instant since = Instant.now();
        List<GatewayLogEntity> expectedLogs = List.of(new GatewayLogEntity());

        when(redisFetchStrategy.fetchLogs(uuid, since)).thenReturn(expectedLogs);

        List<GatewayLogEntity> actualLogs = strategyResolver.fetchLogs(uuid, since);

        assertEquals(expectedLogs, actualLogs);
        verify(databaseFetchStrategy, never()).fetchLogs(anyString(), any(Instant.class));
    }

    @Test
    void testFetchLogs_Fallback_RedisMisses() {
        String uuid = "test-uuid";
        Instant since = Instant.now();
        List<GatewayLogEntity> expectedLogs = List.of(new GatewayLogEntity());

        when(redisFetchStrategy.fetchLogs(uuid, since)).thenReturn(Collections.emptyList());
        when(databaseFetchStrategy.fetchLogs(uuid, since)).thenReturn(expectedLogs);

        List<GatewayLogEntity> actualLogs = strategyResolver.fetchLogs(uuid, since);

        assertEquals(expectedLogs, actualLogs);
        verify(databaseFetchStrategy, times(1)).fetchLogs(uuid, since);
    }
}
