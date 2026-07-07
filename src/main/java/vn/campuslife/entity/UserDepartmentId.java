package vn.campuslife.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartmentId implements Serializable {
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "department_id")
    private Long departmentId;
}
