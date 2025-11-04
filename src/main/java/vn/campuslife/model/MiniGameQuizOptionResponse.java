package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniGameQuizOptionResponse {
    private Long id;
    private String text;
    private boolean correct;
}
