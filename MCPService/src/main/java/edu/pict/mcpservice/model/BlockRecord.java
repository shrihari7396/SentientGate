package edu.pict.mcpservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlockRecord {
    private String reason;
    private String severity;
    private long blockedAt;
    private long expiresAt;
}
