package edu.pict.policyservice.rules;

import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class RateLimitRule implements PolicyRule {

    @Override
    public RuleResult evaluate(PolicyContext ctx) {
        if (ctx.getRequestsPerMinute() > 100) {
            return new RuleResult(
                    25,
                    "Excessive request rate",
                    false
            );
        }
        return null;
    }
}
