package vn.campuslife.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ProfanityFilter {

    private static final Set<String> BLACKLIST = Set.of(
        "lon", "cac", "cat", "deo", "dm", "dkm", "vcl", "vl", "clm", "dcm",
        "cho de", "ngu", "oc cho", "dit me", "ma may", "me may", "thang cho", 
        "con lon", "do ngu", "cc", "cl",
        "fuck", "shit", "bitch", "ass", "cunt", "bastard", "motherfucker", "asshole",
        "wtf", "stfu", "fck", "fuk"
    );

    private static final Map<Character, Character> LEET_MAP = Map.of(
        '1','l', '0','o', '3','e', '@','a', '$','s', '4','a', '5','s', '7','t'
    );

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[._!?*|/\\\\,;:()'\"@#~`\\-]+");

    private String removeAccents(String text) {
        String nfdNormalizedString = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfdNormalizedString).replaceAll("");
        return result.replace('đ', 'd').replace('Đ', 'D');
    }

    private String normalize(String text) {
        String s = removeAccents(text.toLowerCase());
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(LEET_MAP.getOrDefault(c, c));
        }
        return SEPARATOR_PATTERN.matcher(sb.toString()).replaceAll("");
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        String normalized = normalize(text);
        
        // 1. Kiểm tra chính xác từng token hoặc chuỗi multi-word
        for (String badWord : BLACKLIST) {
            // Nếu là multi-word (chứa dấu cách) -> tìm chuỗi con
            if (badWord.contains(" ")) {
                if (normalized.contains(badWord)) return true;
            } else {
                // Nếu là token đơn -> check contains trong tập token
                for (String token : normalized.split("\\s+")) {
                    if (token.equals(badWord)) return true;
                }
            }
        }
        
        // 2. Kiểm tra chuỗi liền (không space)
        String noSpace = normalized.replaceAll("\\s+", "");
        for (String badWord : BLACKLIST) {
            String badNoSpace = badWord.replaceAll("\\s+", "");
            if (badNoSpace.length() >= 3 && noSpace.contains(badNoSpace)) {
                // Exceptions
                if (badNoSpace.equals("ass") && (noSpace.contains("class") || noSpace.contains("pass") || noSpace.contains("bass") || noSpace.contains("assig"))) continue;
                return true;
            }
        }
        return false;
    }

    public String detectReason(String text) {
        if (containsProfanity(text)) return "PROFANITY";
        return null;
    }
}
