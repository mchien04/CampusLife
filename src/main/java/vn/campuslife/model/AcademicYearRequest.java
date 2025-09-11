package vn.campuslife.model;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AcademicYearRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
}
