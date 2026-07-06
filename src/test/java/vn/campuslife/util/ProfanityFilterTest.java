package vn.campuslife.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    public void testCleanText_NoProfanity() {
        String input = "This is a clean and friendly comment.";
        assertFalse(filter.containsProfanity(input));
    }

    @Test
    public void testContainsProfanity_VietnameseBadWords() {
        assertTrue(filter.containsProfanity("dm m lam tro gi vay"));
        assertTrue(filter.containsProfanity("dit me may"));
        assertTrue(filter.containsProfanity("vai lon"));
        assertTrue(filter.containsProfanity("thang cho"));
        assertTrue(filter.containsProfanity("ngu vcl"));
    }

    @Test
    public void testContainsProfanity_EnglishBadWords() {
        assertTrue(filter.containsProfanity("what the fuck is this"));
        assertTrue(filter.containsProfanity("you are a bitch"));
        assertTrue(filter.containsProfanity("asshole"));
        assertTrue(filter.containsProfanity("this is shit"));
        assertTrue(filter.containsProfanity("fcking crazy")); // leetspeak
    }

    @Test
    public void testContainsProfanity_LeetSpeakAndObfuscation() {
        // Testing dots, numbers, asterisks
        assertTrue(filter.containsProfanity("f.u.c.k you"));
        assertTrue(filter.containsProfanity("f*ck you"));
        assertTrue(filter.containsProfanity("d.m may"));
        assertTrue(filter.containsProfanity("v.c.l"));
        assertTrue(filter.containsProfanity("v  c  l"));
    }

    @Test
    public void testContainsProfanity_CaseInsensitivity() {
        assertTrue(filter.containsProfanity("FUCK YOU"));
        assertTrue(filter.containsProfanity("DIT ME"));
        assertTrue(filter.containsProfanity("VcL"));
    }
    
    @Test
    public void testContainsProfanity_FalsePositives() {
        // Assume "đm" is bad, but "đam mê" is good.
        assertFalse(filter.containsProfanity("đam mê nghệ thuật"));
        // "ass" is bad but "class" or "classic" is good
        assertFalse(filter.containsProfanity("he is in a class"));
        assertFalse(filter.containsProfanity("classic music"));
        // "bitch" is bad but "pitch" is good
        assertFalse(filter.containsProfanity("pitch a ball"));
    }
}
