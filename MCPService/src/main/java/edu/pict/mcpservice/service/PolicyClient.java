package edu.pict.mcpservice.service;

import edu.pict.mcpservice.model.PolicyContext;
import edu.pict.mcpservice.model.PolicyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyClient {

    private final RestTemplate restTemplate;

    @Value("${policy.service.url}")
    private String policyServiceUrl;

    public PolicyResult evaluate(PolicyContext context) {

        try {
            return restTemplate.postForObject(
                    policyServiceUrl + "/policy/evaluate",
                    context,
                    PolicyResult.class
            );
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