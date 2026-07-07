package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_participations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    private ActivityRegistration registration;

    @ManyToOne
    @JoinColumn(name = "student_department_id_at_participation")
    private Department studentDepartmentAtParticipation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private vn.campuslife.enumeration.ParticipationType participationType;

    private BigDecimal pointsEarned;

    private LocalDateTime date;

    @Column(nullable = true)
    private Boolean isCompleted; // null = chưa chấm, true = đạt, false = không đạt

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @PrePersist
    void captureStudentDepartmentSnapshot() {
        if (studentDepartmentAtParticipation != null || registration == null) {
            return;
        }
        if (registration.getStudentDepartmentAtRegistration() != null) {
            studentDepartmentAtParticipation = registration.getStudentDepartmentAtRegistration();
            return;
        }
        if (registration.getStudent() != null) {
            studentDepartmentAtParticipation = registration.getStudent().getDepartment();
        }
    }
}
