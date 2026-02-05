package edu.pict.policyservice.rules;

import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class AuthFailureRule implements PolicyRule {

    @Override
    public RuleResult evaluate(PolicyContext ctx) {
        if (ctx.getFailedAuthCount() >= 5) {
            return new RuleResult(
                    30,
                    "High authentication failure rate",
                    false
            );
        }
        return null;
    }
}
