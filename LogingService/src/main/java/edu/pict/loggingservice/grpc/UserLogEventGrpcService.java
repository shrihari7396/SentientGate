package edu.pict.loggingservice.grpc;

import edu.pict.loggingservice.entity.GatewayLogEntity;
import edu.pict.loggingservice.repository.GatewayLogRepository;
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

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserLogEventGrpcService extends UserLogEventServiceGrpc.UserLogEventServiceImplBase {

    private final GatewayLogRepository repository;

    @Override
    public void getUserEvents(
            UserLogEventsRequest request, StreamObserver<UserLogEventResponse> responseObserver) {
        try {
            int durationMinutes = request.getDuration() <= 0 ? 10 : request.getDuration();
            Instant since = Instant.now().minus(Duration.ofMinutes(durationMinutes));

            List<GatewayLogEntity> logs =
                    repository.findByVisitorIdAndOccurredAtAfter(request.getUuid(), since);

            List<UserLogEvent> protoEvents = logs.stream().map(this::toProto).toList();
            responseObserver.onNext(
                    UserLogEventResponse.newBuilder().addAllUserLogEvents(protoEvents).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Failed to fetch gRPC user events for UUID {}", request.getUuid(), e);
            responseObserver.onError(e);
        }
    }

    private UserLogEvent toProto(GatewayLogEntity entity) {
        return UserLogEvent.newBuilder()
                .setUuid(nullSafe(entity.getVisitorId()))
                .setPath(nullSafe(entity.getPath()))
                .setMethod(nullSafe(entity.getMethod()))
                .setLatencyMs(entity.getLatencyMs())
                .setQueryParams(nullSafe(entity.getQueryParams()))
                .setClientIp(nullSafe(entity.getClientIp()))
                .setStatusCode(entity.getStatusCode())
                .setRequestSize(entity.getRequestSize())
                .setTimestamp(entity.getOccurredAt().toEpochMilli())
                .setUserAgent(nullSafe(entity.getUserAgent()))
                .build();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

