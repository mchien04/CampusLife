package vn.campuslife.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SemesterRequest {
    private Long yearId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean open; // optional for toggle
}
