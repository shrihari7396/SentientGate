package edu.pict.mcpservice.util;

import static org.junit.jupiter.api.Assertions.*;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LogEventMapperTest {

    @Nested
    @DisplayName("Single Event Mapping")
    class SingleEventMapping {

        @Test
        @DisplayName("Maps all fields correctly")
        void mapsAllFields() {
            UserLogEvent grpcEvent =
                    UserLogEvent.newBuilder()
                            .setUuid("user-123")
                            .setPath("/api/v1/data")
                            .setMethod("POST")
                            .setLatencyMs(45)
                            .setQueryParams("?filter=active")
                            .setClientIp("192.168.1.100")
                            .setStatusCode(201)
                            .setRequestSize(1024)
                            .setTimestamp(1670000000L)
                            .setUserAgent("Mozilla/5.0")
                            .build();

            LogEvent logEvent = LogEventMapper.fromGrpc(grpcEvent);

            assertNotNull(logEvent);
            assertEquals("user-123", logEvent.getUuid());
            assertEquals("/api/v1/data", logEvent.getPath());
            assertEquals("POST", logEvent.getMethod());
            assertEquals(45, logEvent.getLatencyMs());
            assertEquals("?filter=active", logEvent.getQueryParams());
            assertEquals("192.168.1.100", logEvent.getClientIp());
            assertEquals(201, logEvent.getStatusCode());
            assertEquals(1024, logEvent.getRequestSize());
            assertEquals(1670000000L, logEvent.getTimestamp());
            assertEquals("Mozilla/5.0", logEvent.getUserAgent());
        }

        @Test
        @DisplayName("Handles null input gracefully")
        void handlesNullInput() {
            assertNull(LogEventMapper.fromGrpc(null));
        }
    }

    @Nested
    @DisplayName("Batch Event Mapping")
    class BatchEventMapping {

        @Test
        @DisplayName("Maps a list of events correctly")
        void mapsList() {
            List<UserLogEvent> grpcEvents =
                    List.of(
                            UserLogEvent.newBuilder().setUuid("user-1").setStatusCode(200).build(),
                            UserLogEvent.newBuilder().setUuid("user-2").setStatusCode(404).build());

            List<LogEvent> logEvents = LogEventMapper.fromGrpcList(grpcEvents);

            assertNotNull(logEvents);
            assertEquals(2, logEvents.size());
            assertEquals("user-1", logEvents.get(0).getUuid());
            assertEquals(200, logEvents.get(0).getStatusCode());
            assertEquals("user-2", logEvents.get(1).getUuid());
            assertEquals(404, logEvents.get(1).getStatusCode());
        }

        @Test
        @DisplayName("Handles null list gracefully")
        void handlesNullList() {
            List<LogEvent> result = LogEventMapper.fromGrpcList(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Handles empty list gracefully")
        void handlesEmptyList() {
            List<LogEvent> result = LogEventMapper.fromGrpcList(List.of());
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
