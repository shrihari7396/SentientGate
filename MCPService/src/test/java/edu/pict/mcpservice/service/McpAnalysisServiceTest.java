package edu.pict.mcpservice.service;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpAnalysisServiceTest {

    @Mock
    private EventHistoryService eventHistoryService;

    @Mock
    private EnforcementService enforcementService;

    @Mock
    private ThreatStrategy strategy1;

    @Mock
    private ThreatStrategy strategy2;

    private McpAnalysisService mcpAnalysisService;

    @BeforeEach
    void setUp() {
        mcpAnalysisService = new McpAnalysisService(eventHistoryService, enforcementService,
                Arrays.asList(strategy1, strategy2));
    }

    @Test
    void analyze_ShouldBlock_WhenStrategyTriggers() {
        SecurityAlertEvent alert = SecurityAlertEvent.builder().uuid("user-123").build();
        UserLogEvent grpcEvent = UserLogEvent.newBuilder().setUuid("user-123").build();

        when(eventHistoryService.getAllEventsInDuration(anyString(), anyInt()))
                .thenReturn(Collections.singletonList(grpcEvent));

        when(strategy1.isAvailable(any(), any())).thenReturn(false);
        when(strategy2.isAvailable(any(), any())).thenReturn(true);

        mcpAnalysisService.analyze(alert);

        verify(enforcementService).blockUser(eq("user-123"), eq(strategy2));
        verify(enforcementService, never()).blockUser(anyString(), eq(strategy1));
    }

    @Test
    void analyze_ShouldNotBlock_WhenNoStrategyTriggers() {
        SecurityAlertEvent alert = SecurityAlertEvent.builder().uuid("user-123").build();

        when(eventHistoryService.getAllEventsInDuration(anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        when(strategy1.isAvailable(any(), any())).thenReturn(false);
        when(strategy2.isAvailable(any(), any())).thenReturn(false);

        mcpAnalysisService.analyze(alert);

        verify(enforcementService, never()).blockUser(anyString(), any());
    }
}
