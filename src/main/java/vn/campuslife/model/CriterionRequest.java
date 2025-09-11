package vn.campuslife.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CriterionRequest {
    private Long groupId;
    private String name;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private Long departmentId; // optional assignment
    private String description;
}
