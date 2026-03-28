package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.WorkloadWarningType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadWarningDto {
    private Long studentId;
    private String studentName;
    private Long taskCount;
    private WorkloadWarningType type;
}

