package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotGeminiPingResponse {
    private boolean enabled;
    private boolean ok;
    private String model;
    private String detail;
}
