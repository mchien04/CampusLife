package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;

@Entity
@Table(name = "mini_games", 
       uniqueConstraints = @UniqueConstraint(columnNames = "activity_id"),
       indexes = {
           @Index(name = "idx_type", columnList = "type"),
           @Index(name = "idx_is_active", columnList = "is_active")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @Column(nullable = false)
    @Comment("Tiêu đề minigame")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Comment("Mô tả")
    private String description;

    @Column(nullable = false)
    @Comment("Số lượng câu hỏi")
    private Integer questionCount;

    @Column
    @Comment("Thời gian giới hạn (giây)")
    private Integer timeLimit;

    @Column(nullable = false)
    @Comment("Đang hoạt động")
    private boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("Loại minigame")
    private MiniGameType type;

    @OneToOne
    @JoinColumn(name = "activity_id", nullable = false, unique = true)
    @Comment("Activity")
    private Activity activity;

    @Column
    @Comment("Số câu đúng tối thiểu để đạt")
    private Integer requiredCorrectAnswers;

    @Column(precision = 10, scale = 2)
    @Comment("Điểm thưởng nếu đạt")
    private BigDecimal rewardPoints;

    @Column
    @Comment("Số lần làm quiz tối đa (null = không giới hạn)")
    private Integer maxAttempts;
}

