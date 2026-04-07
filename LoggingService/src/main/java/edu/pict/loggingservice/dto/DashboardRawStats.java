package edu.pict.loggingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardRawStats {
    private Long totalCount;
    private Long securityBlocks;
    private Double avgLatency;
    private Long uniqueIps;
}
