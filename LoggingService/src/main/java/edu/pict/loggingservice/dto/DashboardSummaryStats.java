package edu.pict.loggingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryStats implements Serializable {
    private long throughput; // requests per sec (derived)
    private long securityBlocks;
    private double p99Latency;
    private long totalTraffic;
}
