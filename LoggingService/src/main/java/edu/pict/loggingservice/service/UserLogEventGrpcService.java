package edu.pict.loggingservice.service;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.service.strategy.LogFetchStrategyResolver;
import edu.pict.mcpservice.grpc.UserLogEvent;
import edu.pict.mcpservice.grpc.UserLogEventResponse;
import edu.pict.mcpservice.grpc.UserLogEventServiceGrpc;
import edu.pict.mcpservice.grpc.UserLogEventsRequest;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserLogEventGrpcService extends UserLogEventServiceGrpc.UserLogEventServiceImplBase {

    private final LogFetchStrategyResolver strategyResolver;

    @Override
    public void getUserEvents(
            UserLogEventsRequest request, StreamObserver<UserLogEventResponse> responseObserver) {

        Instant since = Instant.now().minus(Duration.ofMinutes(request.getDuration()));

        // Strategy Pattern: tries Redis first (fast), falls back to PostgreSQL on cache miss
        List<GatewayLogEntity> logs =
                strategyResolver.fetchLogs(request.getUuid(), since);

        UserLogEventResponse.Builder responseBuilder = UserLogEventResponse.newBuilder();

        for (GatewayLogEntity entity : logs) {
            responseBuilder.addUserLogEvents(
                    UserLogEvent.newBuilder()
                            .setUuid(entity.getVisitorId() != null ? entity.getVisitorId() : "")
                            .setPath(entity.getPath() != null ? entity.getPath() : "")
                            .setMethod(entity.getMethod() != null ? entity.getMethod() : "")
                            .setLatencyMs((int) entity.getLatencyMs())
                            .setQueryParams(
                                    entity.getQueryParams() != null ? entity.getQueryParams() : "")
                            .setClientIp(entity.getClientIp() != null ? entity.getClientIp() : "")
                            .setStatusCode(entity.getStatusCode())
                            .setRequestSize((int) entity.getRequestSize())
                            .setTimestamp(entity.getOccurredAt().toEpochMilli())
                            .setUserAgent(
                                    entity.getUserAgent() != null ? entity.getUserAgent() : "")
                            .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
