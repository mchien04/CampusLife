package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotGeminiModelsResponse {
    private boolean enabled;
    private String configuredModel;
    private String effectiveModel;
    private List<String> availableGenerateContentModels = new ArrayList<>();
}
