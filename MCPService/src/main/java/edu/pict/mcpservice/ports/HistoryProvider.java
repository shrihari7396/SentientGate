package edu.pict.mcpservice.ports;

import edu.pict.mcpservice.grpc.UserLogEvent;
import java.util.List;

public interface HistoryProvider {
    List<UserLogEvent> getAllEventsInDuration(String uuid, int duration);
}

