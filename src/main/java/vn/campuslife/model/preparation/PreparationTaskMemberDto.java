package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskMemberRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationTaskMemberDto {
    private Long studentId;
    private String studentName;
    private PreparationTaskMemberRole role;
}

