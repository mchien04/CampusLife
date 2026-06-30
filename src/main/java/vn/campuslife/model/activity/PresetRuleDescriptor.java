package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.ScoreRuleTrigger;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresetRuleDescriptor {
    private String ruleKey;
    private String label;
    private String description;
    private boolean required;
    private boolean enabledByDefault;
    @Builder.Default
    private List<FieldDefinition> fieldDefinitions = new ArrayList<>();
    @Builder.Default
    private List<ScoreRuleTrigger> suggestedCombinations = new ArrayList<>();
    @Builder.Default
    private List<String> conflictsWith = new ArrayList<>();
}
