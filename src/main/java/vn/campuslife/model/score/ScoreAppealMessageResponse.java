package vn.campuslife.model.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAppealMessageResponse {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String content;
    private LocalDateTime createdAt;
}
