package edu.pict.policyservice.controller;

import edu.pict.policyservice.engine.PolicyEvaluator;
import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.PolicyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyEvaluator evaluator;

    @PostMapping("/evaluate")
    public ResponseEntity<PolicyResult> evaluate(
            @RequestBody PolicyContext context) {

        return ResponseEntity.ok(evaluator.evaluate(context));
    }
}

