package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_registrations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Nếu đăng ký này được tạo thông qua đăng ký chuỗi sự kiện,
    // seriesId sẽ trỏ tới ActivitySeries tương ứng. Activity đơn lẻ: null.
    @Column(name = "series_id")
    private Long seriesId;

    private LocalDateTime registeredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private vn.campuslife.enumeration.RegistrationStatus status;

    @CreatedDate
    private LocalDateTime createdAt;
    @Column(unique = true, length = 20)
    private String ticketCode;
}
