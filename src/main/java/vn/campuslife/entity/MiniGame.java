package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;

@Entity
@Table(name = "mini_games")
@Data
public class MiniGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private Integer questionCount;
    private Integer timeLimit;
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    private MiniGameType type;

    @OneToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;
    private Integer requiredCorrectAnswers;

    private BigDecimal rewardPoints;
}
