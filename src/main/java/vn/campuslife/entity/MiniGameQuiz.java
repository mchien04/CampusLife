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
@Table(name = "mini_game_quizzes",
       uniqueConstraints = @UniqueConstraint(columnNames = "mini_game_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MiniGameQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Comment("Khóa chính")
    private Long id;

    @OneToOne
    @JoinColumn(name = "mini_game_id", nullable = false, unique = true)
    @Comment("Minigame")
    private MiniGame miniGame;

    @OneToMany(mappedBy = "miniGameQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("Danh sách câu hỏi")
    private Set<MiniGameQuizQuestion> questions = new LinkedHashSet<>();
}

