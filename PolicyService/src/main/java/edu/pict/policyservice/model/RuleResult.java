package edu.pict.policyservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleResult {

    private int score;
    private String reason;
    private boolean hardBlock;
}
