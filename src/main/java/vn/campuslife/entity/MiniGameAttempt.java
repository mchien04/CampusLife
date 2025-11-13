package vn.campuslife.entity;
import jakarta.persistence.*;
import lombok.Data;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;
@Entity
@Table(name = "mini_game_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mini_game_id","student_id"}))
@Data
public class MiniGameAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "mini_game_id", nullable = false)
    private MiniGame miniGame;

    @ManyToOne @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Integer correctCount;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    private java.time.LocalDateTime startedAt;
    private java.time.LocalDateTime submittedAt;
}

