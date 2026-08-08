package edu.pict.loggingservice.service.strategy;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.repository.GatewayLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseFetchStrategyTest {

    @Mock
    private GatewayLogRepository gatewayLogRepository;

    @InjectMocks
    private DatabaseFetchStrategy databaseFetchStrategy;

    @Test
    void testFetchLogs() {
        String uuid = "test-uuid";
        Instant since = Instant.now();
        List<GatewayLogEntity> expectedLogs = List.of(new GatewayLogEntity());

        when(gatewayLogRepository.findByVisitorIdAndOccurredAtAfter(uuid, since)).thenReturn(expectedLogs);

        List<GatewayLogEntity> actualLogs = databaseFetchStrategy.fetchLogs(uuid, since);

        assertEquals(expectedLogs, actualLogs);
    }
}
