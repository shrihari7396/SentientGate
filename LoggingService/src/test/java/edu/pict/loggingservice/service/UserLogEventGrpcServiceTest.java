package edu.pict.loggingservice.service;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.service.strategy.LogFetchStrategyResolver;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLogEventGrpcServiceTest {

    @Mock
    private LogFetchStrategyResolver strategyResolver;

    @Mock
    private StreamObserver<UserLogEventResponse> responseObserver;

    @InjectMocks
    private UserLogEventGrpcService grpcService;

    @Test
    void testGetUserEvents() {
        String uuid = "test-uuid";
        int durationMinutes = 60;
        UserLogEventsRequest request = UserLogEventsRequest.newBuilder()
                .setUuid(uuid)
                .setDuration(durationMinutes)
                .build();

        GatewayLogEntity logEntity = GatewayLogEntity.builder()
                .visitorId(uuid)
                .path("/api/test")
                .method("GET")
                .latencyMs(50L)
                .statusCode(200)
                .requestSize(100L)
                .occurredAt(Instant.now())
                .clientIp("127.0.0.1")
                .userAgent("test-agent")
                .queryParams("")
                .build();

        when(strategyResolver.fetchLogs(eq(uuid), any(Instant.class))).thenReturn(List.of(logEntity));

        grpcService.getUserEvents(request, responseObserver);

        ArgumentCaptor<UserLogEventResponse> responseCaptor = ArgumentCaptor.forClass(UserLogEventResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        UserLogEventResponse response = responseCaptor.getValue();
        assertEquals(1, response.getUserLogEventsCount());
        assertEquals(uuid, response.getUserLogEvents(0).getUuid());
        assertEquals("/api/test", response.getUserLogEvents(0).getPath());
    }

    @Test
    void testGetUserEvents_WithNullFields_MapsToEmptyStrings() {
        String uuid = "test-uuid";
        UserLogEventsRequest request = UserLogEventsRequest.newBuilder()
                .setUuid(uuid)
                .setDuration(60)
                .build();

        // Entity with null string fields
        GatewayLogEntity logEntity = GatewayLogEntity.builder()
                .visitorId(null)
                .path(null)
                .method(null)
                .clientIp(null)
                .userAgent(null)
                .queryParams(null)
                .occurredAt(Instant.now())
                .build();

        when(strategyResolver.fetchLogs(eq(uuid), any(Instant.class))).thenReturn(List.of(logEntity));

        grpcService.getUserEvents(request, responseObserver);

        ArgumentCaptor<UserLogEventResponse> responseCaptor = ArgumentCaptor.forClass(UserLogEventResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());

        UserLogEventResponse response = responseCaptor.getValue();
        assertEquals(1, response.getUserLogEventsCount());
        
        // Assert that the nulls were safely replaced by empty strings
        assertEquals("", response.getUserLogEvents(0).getUuid());
        assertEquals("", response.getUserLogEvents(0).getPath());
        assertEquals("", response.getUserLogEvents(0).getMethod());
        assertEquals("", response.getUserLogEvents(0).getClientIp());
        assertEquals("", response.getUserLogEvents(0).getUserAgent());
        assertEquals("", response.getUserLogEvents(0).getQueryParams());
    }
}
