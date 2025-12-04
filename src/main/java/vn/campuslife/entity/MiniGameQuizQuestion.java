package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Comment;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "mini_game_quiz_questions",
       indexes = @Index(name = "idx_quiz_order", columnList = "mini_game_quiz_id, display_order"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MiniGameQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Comment("Khóa chính")
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("Nội dung câu hỏi")
    private String questionText;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("Danh sách lựa chọn")
    private Set<MiniGameQuizOption> options = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_game_quiz_id", nullable = false)
    @Comment("Quiz")
    private MiniGameQuiz miniGameQuiz;

    @Column(nullable = false)
    @Comment("Thứ tự hiển thị")
    private Integer displayOrder = 0;

    @Column(name = "image_url", length = 500)
    @Comment("URL ảnh cho câu hỏi")
    private String imageUrl;
}

