package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition {
    private String fieldName;
    private String label;
    private String inputType;
    private boolean required;
    private Object defaultValue;
    private String visibility;
    private List<String> options;
}
