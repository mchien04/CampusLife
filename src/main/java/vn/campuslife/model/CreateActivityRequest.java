package vn.campuslife.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateActivityRequest {

    @NotBlank(message = "Tên hoạt động không được để trống")
    private String name;

    @NotNull(message = "Loại hoạt động bắt buộc")
    private ActivityType type;

    @NotNull(message = "Loại điểm bắt buộc")
    private ScoreType scoreType;

    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;


    private Boolean requiresSubmission = false;

    @DecimalMin(value = "0.0", inclusive = true, message = "Điểm tối đa không hợp lệ")
    private BigDecimal maxPoints;

    private BigDecimal penaltyPointsIncomplete;

    private LocalDateTime registrationStartDate;
    private LocalDateTime registrationDeadline;
    private String shareLink;

    private Boolean isDraft;

    private Boolean isImportant = false;

    private String bannerUrl;
    private String location;

    private Integer ticketQuantity;

    private String benefits;
    private String requirements;
    private String contactInfo;

    private Boolean requiresApproval;
    private Boolean mandatoryForFacultyStudents = false;


    private List<Long> organizerIds;

    private Long seriesId;

    private MiniGameConfig miniGameConfig;

    private SeriesConfig seriesConfig;
}
