package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "mini_game_quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MiniGameQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String questionText;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MiniGameQuizOption> options = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_game_quiz_id")
    private MiniGameQuiz miniGameQuiz;
}
