package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "mini_game_answers",
       indexes = {
           @Index(name = "idx_attempt_question", columnList = "attempt_id, question_id"),
           @Index(name = "idx_is_correct", columnList = "is_correct")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    @Comment("Lần làm bài")
    private MiniGameAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    @Comment("Câu hỏi")
    private MiniGameQuizQuestion question;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    @Comment("Lựa chọn đã chọn")
    private MiniGameQuizOption selectedOption;

    @Column(nullable = false)
    @Comment("Đáp án đúng hay sai")
    private Boolean isCorrect = false;
}

