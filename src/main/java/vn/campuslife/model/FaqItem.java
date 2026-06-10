package vn.campuslife.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaqItem {
    private String id;
    private String title;
    private List<String> questionPatterns;
    private String answer;
    private List<ContactChannel> contactChannels;
}
