package vn.campuslife.util;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ProfanityFilter {

    private static final List<String> BLACKLIST = List.of(
        "đm", "đkm", "clm", "vl", "đcm", "vcl", "ngu", "óc chó", "oc cho",
        "fuck", "shit", "ass", "bitch", "cặc", "lồn", "đéo", "chó đẻ"
    );

    public boolean containsProfanity(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase().replaceAll("\\s+", "");
        return BLACKLIST.stream().anyMatch(lower::contains);
    }

    public String detectReason(String text) {
        if (containsProfanity(text)) return "PROFANITY";
        return null;
    }
}
