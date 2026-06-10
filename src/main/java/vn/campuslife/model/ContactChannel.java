package vn.campuslife.model;

import lombok.Data;

import java.util.List;

@Data
public class ContactChannel {
    private String unit;
    private String code;
    private String officeHours;
    private String location;
    private List<ContactPoint> contactPoints;
}
