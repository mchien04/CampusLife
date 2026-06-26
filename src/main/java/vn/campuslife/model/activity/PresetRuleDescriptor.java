package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
