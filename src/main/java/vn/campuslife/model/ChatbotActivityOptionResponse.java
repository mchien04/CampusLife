package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotActivityOptionResponse {
    private Long id;
    private String name;
    private LocalDateTime startDate;
    private String location;
}
