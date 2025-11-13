package vn.campuslife.entity;
import jakarta.persistence.*;
import lombok.Data;
import vn.campuslife.enumeration.AttemptStatus;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;
@Entity
@Table(name = "mini_game_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id","question_id"}))
@Data
public class MiniGameAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "attempt_id", nullable = false)
    private MiniGameAttempt attempt;

    @ManyToOne @JoinColumn(name = "question_id", nullable = false)
    private MiniGameQuizQuestion question;

    @ManyToOne @JoinColumn(name = "option_id", nullable = false)
    private MiniGameQuizOption selectedOption;

    private Boolean isCorrect;
}

