package vn.campuslife.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.MiniGameType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
@Entity
@Table(name = "mini_game_crossword_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameCrosswordQuestion {

    @jakarta.persistence.Id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String questionText;
    private String answer;
    private int startRow;
    private int startCol;
    private String direction; // "ACROSS" hoặc "DOWN"

    @ManyToOne
    @JoinColumn(name = "crossword_id")
    private MiniGameCrossword crossword;

}

