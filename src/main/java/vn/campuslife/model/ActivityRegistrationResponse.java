package vn.campuslife.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.ScoreType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRegistrationResponse {

    private Long id;
    private Long activityId;
    private String activityName;
    private String activityDescription;
    private LocalDateTime activityStartDate;
    private LocalDateTime activityEndDate;
    private String activityLocation;
    private Long studentId;
    private String studentName;
    private String studentCode;
    private RegistrationStatus status;
    private LocalDateTime registeredDate;
    private LocalDateTime createdAt;
    private String ticketCode;
    // Nếu registration thuộc một chuỗi sự kiện, seriesId != null
    private Long seriesId;
    private boolean isImportant;
    private boolean mandatoryForFacultyStudents;
    private ScoreType scoreType;
}
