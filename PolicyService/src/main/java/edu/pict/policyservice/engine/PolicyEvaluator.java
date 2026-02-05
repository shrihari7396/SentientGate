package edu.pict.policyservice.engine;


import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.PolicyResult;
import edu.pict.policyservice.model.RuleResult;
import edu.pict.policyservice.rules.PolicyRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyEvaluator {

    private final List<PolicyRule> rules;

    public PolicyResult evaluate(PolicyContext context) {

        int totalScore = 0;
        boolean hardBlock = false;
        List<String> reasons = new ArrayList<>();

        for (PolicyRule rule : rules) {
            RuleResult result = rule.evaluate(context);
            if (result != null) {
                totalScore += result.getScore();
                reasons.add(result.getReason());
                hardBlock |= result.isHardBlock();
            }
        }

        return PolicyResult.builder()
                .riskScore(totalScore)
                .hardBlock(hardBlock)
                .reasons(reasons)
                .build();
    }
}
