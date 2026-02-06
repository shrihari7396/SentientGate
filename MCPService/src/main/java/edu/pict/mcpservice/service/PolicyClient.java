package edu.pict.mcpservice.service;

import edu.pict.mcpservice.feienClients.PolicyFeignClient;
import edu.pict.mcpservice.model.PolicyContext;
import edu.pict.mcpservice.model.PolicyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyClient {

    private final PolicyFeignClient policyClient;

    public PolicyResult evaluate(PolicyContext context) {

        try {
            return policyClient.evaluate(context).getBody();
        } catch (Exception e) {
            log.error("Policy service call failed", e);
            // FAIL-SAFE: no hard block, zero risk
            return PolicyResult.builder()
                    .riskScore(0)
                    .hardBlock(false)
                    .reasons(java.util.List.of("Policy service unavailable"))
                    .build();
        }
    }
}