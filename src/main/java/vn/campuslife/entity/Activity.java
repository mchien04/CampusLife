package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Comment;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Activity {

    /** Khóa chính (tự tăng). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    /** Loại hoạt động (TRAINING, BUSINESS, SOCIAL, ...). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("Loại hoạt động (enum)")
    private ActivityType type;

    /** Kiểu tính điểm cho hoạt động (tham gia, nộp minh chứng, sản phẩm, ...). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("Kiểu tính điểm")
    private ScoreType scoreType;


    /** Tên hoạt động hiển thị cho sinh viên. */
    @Column(nullable = false)
    @Comment("Tên hoạt động")
    private String name;

    /** Mô tả chi tiết hoạt động. */
    @Column(columnDefinition = "TEXT")
    @Comment("Mô tả chi tiết")
    private String description;

    /** Ngày bắt đầu. */
    @Comment("Ngày bắt đầu")
    private LocalDate startDate;

    /** Ngày kết thúc. */
    @Comment("Ngày kết thúc")
    private LocalDate endDate;

    /** Có yêu cầu nộp minh chứng/báo cáo sau khi tham gia hay không. */
    @Column(nullable = false)
    @Comment("Yêu cầu nộp minh chứng")
    private boolean requiresSubmission = false;

    /** Điểm tối đa sinh viên có thể đạt được. */
    @Comment("Điểm tối đa")
    private BigDecimal maxPoints;

    /** Ngày mở đăng ký tham gia. */
    @Comment("Ngày bắt đầu đăng ký")
    private LocalDate registrationStartDate;

    /** Hạn cuối đăng ký tham gia. */
    @Comment("Hạn chót đăng ký")
    private LocalDate registrationDeadline;

    /** Đường dẫn chia sẻ hoạt động. */
    @Comment("Link chia sẻ")
    private String shareLink;

    /** Đánh dấu hoạt động quan trọng/ưu tiên. */
    @Column(nullable = false)
    @Comment("Hoạt động quan trọng")
    private boolean isImportant = false;

    /** Đường dẫn ảnh banner. */
    @Comment("Ảnh banner")
    private String bannerUrl;

    /** Địa điểm tổ chức (có thể là 'Online'). */
    @Comment("Địa điểm tổ chức")
    private String location;

    /** Trạng thái xóa mềm (true = đã xóa logic). */
    @Column(nullable = false)
    @Comment("Cờ xóa mềm")
    private boolean isDeleted = false;

    /** Số lượng vé/slot có thể đăng ký (null = không giới hạn). */
    @Comment("Số lượng vé (null = không giới hạn)")
    private Integer ticketQuantity;

    /** Quyền lợi khi tham gia (vd: chứng nhận, quà tặng). */
    @Column(columnDefinition = "TEXT")
    @Comment("Quyền lợi khi tham gia")
    private String benefits;

    /** Yêu cầu đối với người tham gia (điều kiện, chuẩn bị). */
    @Column(columnDefinition = "TEXT")
    @Comment("Yêu cầu tham gia")
    private String requirements;

    /** Thông tin liên hệ hỗ trợ (email/số điện thoại). */
    @Comment("Thông tin liên hệ")
    private String contactInfo;

    /** Có bắt buộc cho sinh viên thuộc khoa tham gia hay không. */
    @Column(nullable = false)
    @Comment("Bắt buộc cho sinh viên thuộc khoa")
    private boolean mandatoryForFacultyStudents = false;

    /** Điểm trừ khi tham gia nhưng không hoàn thành yêu cầu. */
    @Column(precision = 10, scale = 2)
    @Comment("Điểm trừ khi không hoàn thành")
    private BigDecimal penaltyPointsIncomplete;


    /** Danh sách đơn vị tổ chức (nhiều Department cho 1 Activity). */
    @ManyToMany
    @JoinTable(
            name = "activity_departments",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Comment("Danh sách đơn vị tổ chức")
    private Set<Department> organizers = new LinkedHashSet<>();

    /** Ngày tạo bản ghi. */
    @CreatedDate
    @Comment("Ngày tạo")
    private LocalDateTime createdAt;

    /** Ngày chỉnh sửa gần nhất. */
    @LastModifiedDate
    @Comment("Ngày chỉnh sửa")
    private LocalDateTime updatedAt;

    /** Người tạo (username/email). */
    @CreatedBy
    @Comment("Người tạo")
    private String createdBy;

    /** Người chỉnh sửa cuối cùng (username/email). */
    @LastModifiedBy
    @Comment("Người chỉnh sửa cuối cùng")
    private String lastModifiedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public ScoreType getScoreType() {
        return scoreType;
    }

    public void setScoreType(ScoreType scoreType) {
        this.scoreType = scoreType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isRequiresSubmission() {
        return requiresSubmission;
    }

    public void setRequiresSubmission(boolean requiresSubmission) {
        this.requiresSubmission = requiresSubmission;
    }

    public BigDecimal getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(BigDecimal maxPoints) {
        this.maxPoints = maxPoints;
    }

    public LocalDate getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(LocalDate registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public Integer getTicketQuantity() {
        return ticketQuantity;
    }

    public void setTicketQuantity(Integer ticketQuantity) {
        this.ticketQuantity = ticketQuantity;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public boolean isMandatoryForFacultyStudents() {
        return mandatoryForFacultyStudents;
    }

    public void setMandatoryForFacultyStudents(boolean mandatoryForFacultyStudents) {
        this.mandatoryForFacultyStudents = mandatoryForFacultyStudents;
    }

    public BigDecimal getPenaltyPointsIncomplete() {
        return penaltyPointsIncomplete;
    }

    public void setPenaltyPointsIncomplete(BigDecimal penaltyPointsIncomplete) {
        this.penaltyPointsIncomplete = penaltyPointsIncomplete;
    }

    public Set<Department> getOrganizers() {
        return organizers;
    }

    public void setOrganizers(Set<Department> organizers) {
        this.organizers = organizers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
