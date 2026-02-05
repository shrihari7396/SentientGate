package edu.pict.policyservice.rules;

import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.RuleResult;
import org.springframework.stereotype.Component;

@Component
public class JwtReuseRule implements PolicyRule {

    @Override
    public RuleResult evaluate(PolicyContext ctx) {
        if (ctx.getJwtReuseCount() > 3) {
            return new RuleResult(
                    20,
                    "JWT reused multiple times",
                    false
            );
        }
        return null;
    }
}

