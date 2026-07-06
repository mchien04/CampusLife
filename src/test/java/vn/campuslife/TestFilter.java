package vn.campuslife;

import vn.campuslife.util.ProfanityFilter;

public class TestFilter {
    public static void main(String[] args) {
        ProfanityFilter filter = new ProfanityFilter();
        System.out.println("Clean test: " + filter.containsProfanity("This is a clean and friendly comment."));
        System.out.println("FUCK YOU test: " + filter.containsProfanity("FUCK YOU"));
        System.out.println("dm test: " + filter.containsProfanity("đm m làm trò gì vậy"));
        System.out.println("vkl test: " + filter.containsProfanity("vkl"));
        System.out.println("ass test: " + filter.containsProfanity("he is in a class"));
    }
}
