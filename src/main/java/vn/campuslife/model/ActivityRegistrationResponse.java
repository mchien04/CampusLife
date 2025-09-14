package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.RegistrationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationResponse {

    private Long id;
    private Long activityId;
    private String activityName;
    private String activityDescription;
    private LocalDate activityStartDate;
    private LocalDate activityEndDate;
    private String activityLocation;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private RegistrationStatus status;
    private String feedback;
    private LocalDateTime registeredDate;
    private LocalDateTime createdAt;
}
