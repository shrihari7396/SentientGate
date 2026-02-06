package edu.pict.mcpservice.feienClients;

import edu.pict.mcpservice.model.PolicyContext;
import edu.pict.mcpservice.model.PolicyResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "policy-service")
public interface PolicyFeignClient {

    @PostMapping("/api/policyService/policy/evaluate")
    ResponseEntity<PolicyResult> evaluate(@RequestBody PolicyContext context);
}
