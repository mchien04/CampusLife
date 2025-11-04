package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("CROSSWORD")
@Data
@NoArgsConstructor
public class MiniGameCrossword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer rows;
    private Integer cols;

    @Column(columnDefinition = "TEXT")
    private String gridData;
    @OneToOne
    @JoinColumn(name = "mini_game_id")
    private MiniGame miniGame;

    @OneToMany(mappedBy = "crossword", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MiniGameCrosswordQuestion> questions = new LinkedHashSet<>();
}
