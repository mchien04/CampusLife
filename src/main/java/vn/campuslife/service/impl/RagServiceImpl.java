package vn.campuslife.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.campuslife.model.ContactChannel;
import vn.campuslife.model.ContactPoint;
import vn.campuslife.model.FaqData;
import vn.campuslife.model.FaqItem;
import vn.campuslife.service.RagService;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private final ObjectMapper objectMapper;

    private List<FaqItem> faqs = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("rag/faq.json");

            InputStream inputStream = resource.getInputStream();

            FaqData faqData = objectMapper.readValue(inputStream, FaqData.class);

            this.faqs = faqData.getFaqs();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<String> findAnswer(String question) {

        String normalizedQuestion = normalize(question);
        System.out.println("RAG question = " + normalizedQuestion);
        System.out.println("FAQ size = " + faqs.size());
        for (FaqItem faq : faqs) {
            if (faq.getQuestionPatterns() == null || faq.getQuestionPatterns().isEmpty()) {
                continue;
            }

            for (String pattern : faq.getQuestionPatterns()) {
                if (pattern == null || pattern.isBlank()) {
                    continue;
                }

                String normalizedPattern = normalize(pattern);

                if (matches(normalizedQuestion,normalizedPattern)) {
                    System.out.println("RAG matched = " + faq.getId());
                    if ("CONTACT_CHANNELS".equals(faq.getId())) {
                        Optional<String> contactAnswer = buildContactAnswer(normalizedQuestion, faq);
                        if (contactAnswer.isPresent()) {
                            return contactAnswer;
                        }
                    }
                    return Optional.of(faq.getAnswer());
                }
            }
        }

        return Optional.empty();
    }

    private Optional<String> buildContactAnswer(String normalizedQuestion, FaqItem faq) {
        if (faq.getContactChannels() == null || faq.getContactChannels().isEmpty()) {
            return Optional.empty();
        }

        for (ContactChannel channel : faq.getContactChannels()) {
            String unit = normalize(channel.getUnit());
            String code = normalize(channel.getCode());

            if (normalizedQuestion.contains(unit)
                    || normalizedQuestion.contains(code)
                    || matchesFacultyAlias(normalizedQuestion, code)) {

                return Optional.of(formatContactChannel(channel));
            }
        }

        return Optional.of(faq.getAnswer());
    }

    private boolean matchesFacultyAlias(String q, String code) {
        if (code == null) {
            return false;
        }
        return switch (code.toUpperCase()) {
            case "FIT" -> q.contains("cong nghe thong tin") || q.contains("cntt");
            case "FEEE" -> q.contains("dien") || q.contains("dien tu");
            case "FME" -> q.contains("co khi") || q.contains("che tao may");
            case "CTSV" -> q.contains("ctsv") || q.contains("cong tac sinh vien");
            case "SYSADMIN" -> q.contains("admin") || q.contains("he thong");
            default -> false;
        };
    }

    private String formatContactChannel(ContactChannel channel) {
        StringBuilder sb = new StringBuilder();

        sb.append(channel.getUnit()).append("\n");

        if (channel.getLocation() != null) {
            sb.append("Địa điểm: ").append(channel.getLocation()).append("\n");
        }

        if (channel.getOfficeHours() != null) {
            sb.append("Giờ làm việc: ").append(channel.getOfficeHours()).append("\n");
        }

        if (channel.getContactPoints() != null && !channel.getContactPoints().isEmpty()) {
            sb.append("Thông tin liên hệ:");
            channel.getContactPoints().stream()
                    .sorted(Comparator.comparing(cp -> cp.getPriority() == null ? Integer.MAX_VALUE : cp.getPriority()))
                    .forEach(p -> sb.append("\n- ").append(p.getType()).append(": ").append(p.getValue()));
        }

        return sb.toString();
    }

    private boolean matches(String question, String pattern) {

        String[] words = pattern.split(" ");

        int matched = 0;

        for (String word : words) {

            if (word.length() < 3) continue;

            if (question.contains(word)) {
                matched++;
            }
        }

        return matched >= Math.max(2, words.length / 2);
    }
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

}
