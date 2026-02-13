package edu.pict.mcpservice.service;

import edu.pict.mcp.grpc.UserLogEvent;
import edu.pict.mcp.grpc.UserLogEventResponse;
import edu.pict.mcp.grpc.UserLogEventServiceGrpc;
import edu.pict.mcp.grpc.UserLogEventsRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventHistoryService {

    @GrpcClient("logging-service")
    private UserLogEventServiceGrpc.UserLogEventServiceBlockingStub stub;

    public List<UserLogEvent> getAllEventsInDuration(String uuid, int duration) {

        UserLogEventsRequest request = UserLogEventsRequest.newBuilder()
                .setUuid(uuid)
                .setDuration(duration)
                .build();

        UserLogEventResponse response = stub.getUserEvents(request);

        log.info("Fetched {} events for UUID={}",
                response.getUserLogEventsCount(), uuid);

        return response.getUserLogEventsList();
    }
}
