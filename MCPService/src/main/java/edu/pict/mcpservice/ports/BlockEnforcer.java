package edu.pict.mcpservice.ports;

import edu.pict.mcpservice.stratagies.blocking.ThreatStrategy;

public interface BlockEnforcer {
    boolean isBlocked(String uuid);

    void blockUser(String uuid, String clientIp, ThreatStrategy strategy);
}

