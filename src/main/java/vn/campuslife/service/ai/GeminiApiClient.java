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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GeminiApiClient {

    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String effectiveModel;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public String getEffectiveModel() {
        return effectiveModel == null || effectiveModel.isBlank() ? model : effectiveModel;
    }

    public Optional<String> generateText(String prompt) {
        return generate(prompt, null);
    }

    public Optional<String> generateJson(String prompt) {
        return generate(prompt, "application/json");
    }

    private Optional<String> generate(String prompt, String responseMimeType) {
        if (!isEnabled()) {
            return Optional.empty();
        }

        try {
            String modelToUse = getEffectiveModel();
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelToUse
                    + ":generateContent?key=" + apiKey;

            Map<String, Object> generationConfig = responseMimeType == null
                    ? Map.of("temperature", 0.2)
                    : Map.of("temperature", 0.2, "responseMimeType", responseMimeType);

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers),
                    String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return Optional.of("__GEMINI_HTTP__:" + resp.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText("");
                if (blockReason != null && !blockReason.isBlank()) {
                    return Optional.of("__GEMINI_BLOCKED__:" + blockReason);
                }
                return Optional.of("__GEMINI_EMPTY__");
            }

            JsonNode parts = candidates.path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return Optional.of("__GEMINI_EMPTY__");
            }

            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                JsonNode t = p.path("text");
                if (!t.isMissingNode() && !t.asText().isBlank()) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(t.asText());
                }
            }
            if (sb.length() == 0) {
                return Optional.of("__GEMINI_EMPTY__");
            }
            return Optional.of(sb.toString());
        } catch (HttpStatusCodeException e) {
            String errorMessage = "";
            try {
                JsonNode errRoot = objectMapper.readTree(e.getResponseBodyAsString());
                errorMessage = errRoot.path("error").path("message").asText("");
            } catch (Exception ignored) {
            }
            if (e.getStatusCode().value() == 404 && errorMessage != null && errorMessage.contains("is not found")) {
                Optional<String> picked = pickFirstGenerateContentModel();
                if (picked.isPresent() && (effectiveModel == null || !effectiveModel.equals(picked.get()))) {
                    effectiveModel = picked.get();
                    return generate(prompt, responseMimeType);
                }
            }
            if (errorMessage != null && !errorMessage.isBlank()) {
                return Optional.of("__GEMINI_HTTP__:" + e.getStatusCode().value() + ":" + errorMessage);
            }
            return Optional.of("__GEMINI_HTTP__:" + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            return Optional.of("__GEMINI_NETWORK__");
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<String> listGenerateContentModels() {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode modelsNode = root.path("models");
            if (!modelsNode.isArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode m : modelsNode) {
                JsonNode supported = m.path("supportedGenerationMethods");
                boolean ok = false;
                if (supported.isArray()) {
                    for (JsonNode sm : supported) {
                        if ("generateContent".equalsIgnoreCase(sm.asText())) {
                            ok = true;
                            break;
                        }
                    }
                }
                if (!ok) {
                    continue;
                }
                String name = m.path("name").asText("");
                if (name.startsWith("models/")) {
                    name = name.substring("models/".length());
                }
                if (!name.isBlank()) {
                    result.add(name);
                }
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Optional<String> pickFirstGenerateContentModel() {
        List<String> models = listGenerateContentModels();
        if (models.isEmpty()) {
            return Optional.empty();
        }
        List<String> preferred = List.of(
                "gemini-1.5-flash",
                "gemini-1.5-flash-001",
                "gemini-1.5-flash-latest",
                "gemini-2.0-flash",
                "gemini-2.0-flash-001",
                "gemini-2.5-flash",
                "gemini-3-flash-preview");
        for (String p : preferred) {
            for (String m : models) {
                if (m.equalsIgnoreCase(p)) {
                    return Optional.of(m);
                }
            }
        }
        for (String m : models) {
            if (m.toLowerCase().contains("gemini") && m.toLowerCase().contains("flash")) {
                return Optional.of(m);
            }
        }
        for (String m : models) {
            if (m.toLowerCase().contains("gemini")) {
                return Optional.of(m);
            }
        }
        return Optional.of(models.getFirst());
    }
}
