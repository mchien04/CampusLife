package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_series")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @Column(nullable = false)
    @Comment("Tên chuỗi sự kiện")
    private String name;

    @Column(columnDefinition = "TEXT")
    @Comment("Mô tả chuỗi sự kiện")
    private String description;

    @Column(columnDefinition = "TEXT")
    @Comment("JSON: {\"3\": 5, \"4\": 7, \"5\": 10} - Mốc điểm thưởng")
    private String milestonePoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("Loại điểm để cộng milestone (REN_LUYEN, CONG_TAC_XA_HOI, etc.)")
    private vn.campuslife.enumeration.ScoreType scoreType;

    @ManyToOne
    @JoinColumn(name = "main_activity_id")
    @Comment("Activity chính (có thể null)")
    private Activity mainActivity;

    @Column
    @Comment("Ngày mở đăng ký tham gia chuỗi")
    private LocalDateTime registrationStartDate;

    @Column
    @Comment("Hạn chót đăng ký tham gia chuỗi")
    private LocalDateTime registrationDeadline;

    @Column(nullable = false)
    @Comment("Đăng ký có cần duyệt hay không")
    private boolean requiresApproval = true;

    @Column
    @Comment("Số lượng vé/slot có thể đăng ký (null = không giới hạn)")
    private Integer ticketQuantity;

    @Column(nullable = false, updatable = false)
    @Comment("Ngày tạo")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Comment("Cờ xóa mềm")
    private boolean isDeleted = false;
}
