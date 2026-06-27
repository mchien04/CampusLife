package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;
import vn.campuslife.enumeration.ScoreRuleAudience;
import vn.campuslife.enumeration.SeriesPresetCode;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column(nullable = false)
    @Comment("Bật/tắt rule yêu cầu tham gia tối thiểu để tránh bị trừ điểm")
    private boolean minimumRequirementEnabled = false;

    @Column
    @Comment("Số sự kiện tối thiểu phải hoàn thành để không bị trừ điểm")
    private Integer minimumRequiredEvents;

    @Column
    @Comment("Số điểm bị trừ nếu không đạt yêu cầu tối thiểu")
    private Integer minimumPenaltyPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("Loại điểm để cộng milestone (REN_LUYEN, CONG_TAC_XA_HOI, etc.)")
    private vn.campuslife.enumeration.ScoreType scoreType;

    @ManyToOne
    @JoinColumn(name = "target_semester_id")
    @Comment("H?c k? c?ng di?m cho chu?i s? ki?n")
    private Semester targetSemester;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("Đối tượng áp dụng điểm (ALL_PARTICIPANTS, DEPARTMENT_ONLY, OUTSIDE_DEPARTMENTS_ONLY)")
    private ScoreRuleAudience audience = ScoreRuleAudience.ALL_PARTICIPANTS;

    @ManyToMany
    @JoinTable(name = "activity_series_departments",
        joinColumns = @JoinColumn(name = "series_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id"))
    @Comment("Danh sách khoa áp dụng khi audience != ALL_PARTICIPANTS")
    private Set<Department> targetDepartments = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "preset_code", length = 50)
    @Comment("Preset code used to configure score rules for this series")
    private SeriesPresetCode presetCode;
}

