package edu.pict.policyservice.rules;

import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class RouteSensitivityRule implements PolicyRule {

    @Override
    public RuleResult evaluate(PolicyContext ctx) {

        if ("HIGH".equalsIgnoreCase(ctx.getRouteSensitivity())
                && ctx.getRequestsPerMinute() > 50) {

            return new RuleResult(
                    25,
                    "High traffic on sensitive route",
                    false
            );
        }
        return null;
    }
}

