package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "mini_game_quiz_options",
       indexes = {
           @Index(name = "idx_question", columnList = "question_id"),
           @Index(name = "idx_is_correct", columnList = "is_correct")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameQuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("Nội dung lựa chọn")
    private String text;

    @Column(nullable = false)
    @Comment("Là đáp án đúng")
    private boolean isCorrect = false;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    @Comment("Câu hỏi")
    private MiniGameQuizQuestion question;
}

