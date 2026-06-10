package vn.campuslife.model;

import lombok.Data;

@Data
public class ContactPoint {
    private String type;
    private String value;
    private Integer priority;
}
