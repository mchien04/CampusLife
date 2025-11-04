package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "mini_game_quiz")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MiniGameQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "mini_game_id")
    private MiniGame miniGame;

    @OneToMany(mappedBy = "miniGameQuiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MiniGameQuizQuestion> questions = new LinkedHashSet<>();
}
