package edu.pict.mcpservice.service;

import edu.pict.mcpservice.kafkaEvents.LogEvent;
import edu.pict.mcpservice.kafkaEvents.SecurityAlertEvent;
import edu.pict.mcpservice.ports.BlockEnforcer;
import edu.pict.mcpservice.stratagies.blocking.AsyncThreatStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncThreatEvaluator {

    private final BlockEnforcer blockEnforcer;

    @Async("aiAnalysisExecutor")
    public void evaluate(
            SecurityAlertEvent alert, List<LogEvent> history, List<AsyncThreatStrategy> asyncStrategies) {
        if (blockEnforcer.isBlocked(alert.getUuid())) {
            log.debug("Skipping async evaluation for already blocked UUID {}", alert.getUuid());
            return;
        }

        asyncStrategies.stream()
                .filter(strategy -> strategy.isAvailable(alert, history))
                .findFirst()
                .ifPresent(
                        strategy ->
                                blockEnforcer.blockUser(alert.getUuid(), alert.getClientIp(), strategy));
    }
}

