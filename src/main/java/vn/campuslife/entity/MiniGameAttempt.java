package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;
import vn.campuslife.enumeration.AttemptStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "mini_game_attempts",
       indexes = {
           @Index(name = "idx_student_minigame", columnList = "student_id, mini_game_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_started_at", columnList = "started_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mini_game_id", nullable = false)
    @Comment("Minigame")
    private MiniGame miniGame;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    @Comment("Sinh viên")
    private Student student;

    @Column(nullable = false)
    @Comment("Số câu đúng")
    private Integer correctCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Comment("Trạng thái")
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(nullable = false)
    @Comment("Thời gian bắt đầu")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column
    @Comment("Thời gian nộp bài")
    private LocalDateTime submittedAt;
}

