package vn.campuslife.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeminiApiClient {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> generateJson(String prompt) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", prompt))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(textNode.asText());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
