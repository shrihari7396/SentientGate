package edu.pict.mcpservice.util;

import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.kafkaEvents.LogEvent;
import java.util.List;
import java.util.stream.Collectors;

/** Utility class for mapping gRPC UserLogEvent objects to internal LogEvent objects. */
public final class LogEventMapper {

    private LogEventMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Maps a single gRPC UserLogEvent to an internal LogEvent.
     *
     * @param grpcEvent the gRPC event
     * @return the mapped LogEvent
     */
    public static LogEvent fromGrpc(UserLogEvent grpcEvent) {
        if (grpcEvent == null) {
            return null;
        }
        return LogEvent.builder()
                .uuid(grpcEvent.getUuid())
                .path(grpcEvent.getPath())
                .method(grpcEvent.getMethod())
                .latencyMs(grpcEvent.getLatencyMs())
                .queryParams(grpcEvent.getQueryParams())
                .clientIp(grpcEvent.getClientIp())
                .statusCode(grpcEvent.getStatusCode())
                .requestSize(grpcEvent.getRequestSize())
                .timestamp(grpcEvent.getTimestamp())
                .userAgent(grpcEvent.getUserAgent())
                .build();
    }

    /**
     * Maps a list of gRPC UserLogEvents to a list of internal LogEvents.
     *
     * @param grpcEvents the list of gRPC events
     * @return the list of mapped LogEvents
     */
    public static List<LogEvent> fromGrpcList(List<UserLogEvent> grpcEvents) {
        if (grpcEvents == null) {
            return List.of();
        }
        return grpcEvents.stream().map(LogEventMapper::fromGrpc).collect(Collectors.toList());
    }
}
