package vn.campuslife.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_departments", indexes = {
        @Index(name = "idx_user_departments_user", columnList = "user_id"),
        @Index(name = "idx_user_departments_dept", columnList = "department_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartment {
    @EmbeddedId
    private UserDepartmentId id = new UserDepartmentId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("departmentId")
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    public UserDepartment(User user, Department department, User assignedBy) {
        this.user = user;
        this.department = department;
        this.assignedBy = assignedBy;
        this.id = new UserDepartmentId(
                user != null ? user.getId() : null,
                department != null ? department.getId() : null);
    }

    @PrePersist
    void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
