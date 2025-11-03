<!-- 28e799c6-66ec-437e-af14-dc567118cb63 c815263c-dc6f-41f7-a251-6b97377a7244 -->
# Tái cấu trúc hệ thống điểm sinh viên

## Vấn đề hiện tại

- StudentScore hiện tạo nhiều bản ghi riêng lẻ cho mỗi activity/submission
- Thiếu khởi tạo bản ghi điểm khi sinh viên đăng ký tài khoản
- Entity có nhiều trường dư thừa (taskId, submissionId, scoreSourceType)
- Không có cơ chế lưu danh sách activity đã nhận điểm

## Giải pháp

### 1. Tái cấu trúc StudentScore entity

**File:** `src/main/java/vn/campuslife/entity/StudentScore.java`

Thay đổi:

- Loại bỏ: `taskId`, `submissionId`, `scoreSourceType`, `sourceNote`, `enteredBy`, `entryDate`, `isLocked`
- Giữ lại: `student`, `semester`, `scoreType`, `score` (tổng điểm tích lũy)
- Thêm mới: `activityIds` (String, lưu JSON array của activity IDs, ví dụ: "[1,5,10]")
- Giữ lại `criterion` (nullable) cho phần điểm rèn luyện chi tiết theo tiêu chí

Cấu trúc mới:

```java
@Entity
@Table(name = "student_scores")
public class StudentScore {
    private Long id;
    private Student student;        // @ManyToOne
    private Semester semester;      // @ManyToOne
    private ScoreType scoreType;    // REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
    private BigDecimal score;       // Tổng điểm tích lũy
    
    @Column(columnDefinition = "TEXT")
    private String activityIds;     // JSON: "[1,5,10]" - danh sách activity đã nhận điểm
    
    @ManyToOne
    @JoinColumn(name = "criterion_id")
    private Criterion criterion;    // Nullable, dùng cho điểm rèn luyện chi tiết
    
    private String notes;           // Ghi chú
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. Tạo bản ghi điểm khi đăng ký sinh viên

**File:** `src/main/java/vn/campuslife/service/impl/AuthServiceImpl.java`

Sau khi tạo Student profile (line 126), thêm:

- Lấy semester hiện tại (isOpen = true)
- Nếu có semester, tạo 3 bản ghi StudentScore với:
  - `scoreType`: REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
  - `score`: 0
  - `activityIds`: "[]"
  - `criterion`: null

### 3. Tạo service quản lý điểm tích lũy

**File mới:** `src/main/java/vn/campuslife/service/StudentScoreInitService.java`

Tạo service với method:

```java
void initializeStudentScores(Student student, Semester semester)
```

- Tạo 3 bản ghi StudentScore cho các scoreType
- Logic tái sử dụng cho cả đăng ký mới và chuyển học kỳ mới

### 4. Cập nhật logic cộng điểm từ activity

**File:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

Sửa method `createScoreFromCheckIn` (line 303-365):

- Tìm bản ghi StudentScore tổng hợp (student + semester + scoreType từ activity)
- Parse `activityIds` JSON, thêm activityId mới vào list
- Cộng điểm vào `score`
- Lưu lại JSON mới vào `activityIds`
- Cập nhật ScoreHistory với logic đơn giản hơn

### 5. Cập nhật logic cộng điểm từ submission

**File:** `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`

Sửa method `createScoreFromSubmission` (line 296-373):

- Tương tự như check-in, tìm bản ghi tổng hợp
- Thêm activityId vào list (từ task.getActivity())
- Cộng điểm vào tổng

### 6. Cập nhật ScoreHistory entity

**File:** `src/main/java/vn/campuslife/entity/ScoreHistory.java`

Đơn giản hóa:

- Loại bỏ: `taskId`, `submissionId`, `scoreSourceType`
- Giữ lại: `score`, `oldScore`, `newScore`, `changedBy`, `changeDate`, `reason`
- Thêm: `activityId` (nullable)

### 7. Cập nhật Repository methods

**File:** `src/main/java/vn/campuslife/repository/StudentScoreRepository.java`

Thay thế các method:

- Loại bỏ: `existsByStudentIdAndActivityIdAndScoreSourceType`, `existsByStudentIdAndSubmissionIdAndScoreSourceType`
- Thêm: `findByStudentIdAndSemesterIdAndScoreType(Long studentId, Long semesterId, ScoreType scoreType)`

### 8. Cập nhật ScoreService

**File:** `src/main/java/vn/campuslife/service/impl/ScoreServiceImpl.java`

Sửa các method:

- `viewScores`: Parse `activityIds` JSON để hiển thị danh sách activity
- `getTotalScore`: Tính tổng 3 loại điểm
- `calculateTrainingScore`: Logic tính điểm rèn luyện theo tiêu chí (với criterion)

### 9. Cập nhật DTOs

**File:** `src/main/java/vn/campuslife/model/ScoreViewResponse.java`

Sửa `ScoreItem`:

```java
public static class ScoreItem {
    private BigDecimal score;
    private List<Long> activityIds;  // Danh sách activity đã nhận điểm
    private Long criterionId;        // Nullable
}
```

### 10. Migration database

Sau khi cập nhật entities, cần:

- Drop và recreate bảng `student_scores` (hoặc migrate dữ liệu cũ)
- Khởi tạo lại điểm cho các sinh viên hiện có

## Lợi ích

1. Giảm số lượng bản ghi trong database (từ nhiều bản ghi/activity → 3 bản ghi/học kỳ)
2. Dễ dàng tính tổng điểm cho từng loại
3. Vẫn truy vết được các activity đã nhận điểm qua `activityIds`
4. Loại bỏ các trường không cần thiết, tối ưu storage
5. Tự động khởi tạo điểm khi đăng ký tài khoản

### To-dos

- [ ] Refactor StudentScore entity - loại bỏ trường dư thừa, thêm activityIds JSON
- [ ] Đơn giản hóa ScoreHistory entity
- [ ] Tạo StudentScoreInitService để khởi tạo 3 bản ghi điểm
- [ ] Cập nhật AuthServiceImpl để tạo điểm khi đăng ký sinh viên
- [ ] Sửa logic cộng điểm từ activity check-in (ActivityRegistrationServiceImpl)
- [ ] Sửa logic cộng điểm từ task submission (TaskSubmissionServiceImpl)
- [ ] Cập nhật StudentScoreRepository methods
- [ ] Cập nhật ScoreServiceImpl cho logic mới
- [ ] Cập nhật ScoreViewResponse và các DTOs liên quan
- [ ] Test compilation và fix lỗi nếu có