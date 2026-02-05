package edu.pict.policyservice.rules;

import edu.pict.policyservice.model.PolicyContext;
import edu.pict.policyservice.model.RuleResult;

public interface PolicyRule {
    RuleResult evaluate(PolicyContext context);
}
