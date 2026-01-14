File markdown hướng dẫn chi tiết:

```markdown:GUIDE_AUTO_SCORE_INIT_AND_SEMESTER_BASED_SCORING.md
# Hướng Dẫn Implement: Tự Động Tạo Điểm và Ghi Điểm Theo Học Kỳ

## 📋 Tổng Quan

Tài liệu này hướng dẫn implement 2 tính năng chính:

1. **Tự động tạo điểm khi tạo học kỳ mới** + API thủ công để gọi lại
2. **Ghi điểm vào đúng học kỳ** dựa vào thời gian diễn ra sự kiện (không phải thời gian tạo)

---

## 🎯 Mục Tiêu

### Tính Năng 1: Tự Động Tạo Điểm
- Khi admin tạo học kỳ mới và set `isOpen = true`, hệ thống tự động tạo 3 loại điểm (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE) cho tất cả sinh viên
- Có API thủ công để gọi lại nếu cần

### Tính Năng 2: Ghi Điểm Theo Học Kỳ Của Sự Kiện
- Điểm được ghi vào học kỳ mà sự kiện diễn ra (dựa vào `startDate` của Activity)
- Ví dụ: Activity được tạo trước khi học kỳ 2 mở, nhưng `startDate` nằm trong học kỳ 2 → điểm thuộc học kỳ 2

---

## 📁 Danh Sách Files Cần Thay Đổi

### Files Mới Tạo
1. `src/main/java/vn/campuslife/service/SemesterHelperService.java`

### Files Cần Chỉnh Sửa
1. `src/main/java/vn/campuslife/repository/SemesterRepository.java`
2. `src/main/java/vn/campuslife/service/StudentScoreInitService.java`
3. `src/main/java/vn/campuslife/service/AcademicService.java`
4. `src/main/java/vn/campuslife/service/impl/AcademicServiceImpl.java`
5. `src/main/java/vn/campuslife/controller/AcademicAdminController.java`
6. `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
7. `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`
8. `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`
9. `src/main/java/vn/campuslife/service/impl/ActivitySeriesServiceImpl.java`

---

## 🔧 Bước 1: Thêm Query Method vào SemesterRepository

### File: `src/main/java/vn/campuslife/repository/SemesterRepository.java`

```java
package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Semester;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    
    /**
     * Tìm semester mà một ngày cụ thể nằm trong khoảng startDate và endDate
     * Ưu tiên semester có isOpen = true nếu có nhiều semester trùng
     */
    @Query("""
        SELECT s FROM Semester s 
        WHERE s.startDate <= :date 
        AND s.endDate >= :date 
        ORDER BY s.isOpen DESC, s.startDate DESC
        """)
    Optional<Semester> findByDate(@Param("date") LocalDate date);
    
    /**
     * Tìm semester mà một LocalDateTime nằm trong khoảng startDate và endDate
     */
    default Optional<Semester> findByDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Optional.empty();
        }
        return findByDate(dateTime.toLocalDate());
    }
}
```

**Giải thích:**
- Query tìm semester có `startDate <= date <= endDate`
- Sắp xếp theo `isOpen DESC` để ưu tiên semester đang mở
- Method `findByDateTime` convert LocalDateTime → LocalDate

---

## 🔧 Bước 2: Tạo SemesterHelperService

### File: `src/main/java/vn/campuslife/service/SemesterHelperService.java` (FILE MỚI)

```java
package vn.campuslife.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Semester;
import vn.campuslife.repository.SemesterRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterHelperService {

    private final SemesterRepository semesterRepository;

    /**
     * Tìm semester dựa vào thời gian diễn ra activity
     * Logic: Dùng startDate của activity để xác định semester
     * Nếu không tìm thấy, fallback về semester đang mở
     */
    public Semester getSemesterForActivity(Activity activity) {
        if (activity == null) {
            return getCurrentOpenSemester();
        }

        // Ưu tiên: Dùng startDate của activity
        LocalDateTime activityStartDate = activity.getStartDate();
        if (activityStartDate != null) {
            Optional<Semester> semesterOpt = semesterRepository.findByDateTime(activityStartDate);
            if (semesterOpt.isPresent()) {
                log.debug("Found semester {} for activity {} based on startDate {}", 
                        semesterOpt.get().getId(), activity.getId(), activityStartDate);
                return semesterOpt.get();
            }
        }

        // Fallback: Dùng endDate nếu startDate không tìm thấy
        LocalDateTime activityEndDate = activity.getEndDate();
        if (activityEndDate != null) {
            Optional<Semester> semesterOpt = semesterRepository.findByDateTime(activityEndDate);
            if (semesterOpt.isPresent()) {
                log.debug("Found semester {} for activity {} based on endDate {}", 
                        semesterOpt.get().getId(), activity.getId(), activityEndDate);
                return semesterOpt.get();
            }
        }

        // Fallback cuối cùng: Dùng semester đang mở
        log.warn("Could not find semester for activity {} (startDate: {}, endDate: {}). " +
                "Using current open semester as fallback.", 
                activity.getId(), activityStartDate, activityEndDate);
        return getCurrentOpenSemester();
    }

    /**
     * Tìm semester dựa vào một ngày cụ thể
     */
    public Semester getSemesterForDate(LocalDate date) {
        if (date == null) {
            return getCurrentOpenSemester();
        }

        Optional<Semester> semesterOpt = semesterRepository.findByDate(date);
        if (semesterOpt.isPresent()) {
            return semesterOpt.get();
        }

        log.warn("Could not find semester for date {}. Using current open semester as fallback.", date);
        return getCurrentOpenSemester();
    }

    /**
     * Lấy semester đang mở (fallback)
     */
    private Semester getCurrentOpenSemester() {
        return semesterRepository.findAll().stream()
                .filter(Semester::isOpen)
                .findFirst()
                .orElse(semesterRepository.findAll().stream()
                        .findFirst()
                        .orElse(null));
    }
}
```

**Giải thích:**
- Method `getSemesterForActivity`: Tìm semester dựa vào `startDate` của activity
- Fallback: Nếu không tìm thấy → dùng `endDate` → cuối cùng dùng semester đang mở
- Log để debug khi không tìm thấy semester

---

## 🔧 Bước 3: Cập Nhật StudentScoreInitService

### File: `src/main/java/vn/campuslife/service/StudentScoreInitService.java`

**Thêm import:**
```java
import vn.campuslife.repository.StudentRepository;
import java.util.List;
import java.util.stream.Collectors;
```

**Thêm field:**
```java
private final StudentScoreRepository studentScoreRepository;
private final SemesterRepository semesterRepository;
private final StudentRepository studentRepository; // ✅ THÊM
```

**Thêm method mới:**
```java
/**
 * Initialize scores for all active students in a semester
 * Used when creating a new semester
 */
@Transactional
public void initializeScoresForAllStudents(Semester semester) {
    try {
        log.info("Initializing scores for all students in semester {}", semester.getId());
        
        // Get all active students
        List<Student> students = studentRepository.findAll()
                .stream()
                .filter(s -> !s.isDeleted())
                .collect(Collectors.toList());
        
        log.info("Found {} active students to initialize scores", students.size());
        
        int successCount = 0;
        int skipCount = 0;
        
        for (Student student : students) {
            try {
                // Check if already initialized
                boolean alreadyExists = studentScoreRepository
                        .findByStudentIdAndSemesterIdAndScoreType(
                                student.getId(), 
                                semester.getId(), 
                                ScoreType.REN_LUYEN)
                        .isPresent();
                
                if (alreadyExists) {
                    skipCount++;
                    continue;
                }
                
                initializeStudentScores(student, semester);
                successCount++;
                
                // Log progress every 100 students
                if (successCount % 100 == 0) {
                    log.info("Initialized scores for {} students...", successCount);
                }
            } catch (Exception e) {
                log.error("Failed to initialize scores for student {}: {}", 
                        student.getId(), e.getMessage());
            }
        }
        
        log.info("Completed initializing scores: {} created, {} skipped, {} total students", 
                successCount, skipCount, students.size());
        
    } catch (Exception e) {
        log.error("Failed to initialize scores for all students: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to initialize scores for all students", e);
    }
}
```

**Giải thích:**
- Lấy tất cả sinh viên chưa bị xóa
- Kiểm tra xem đã có điểm chưa (tránh duplicate)
- Log tiến độ mỗi 100 sinh viên
- Xử lý lỗi từng sinh viên để không fail cả batch

---

## 🔧 Bước 4: Cập Nhật AcademicService Interface

### File: `src/main/java/vn/campuslife/service/AcademicService.java`

**Thêm method:**
```java
Response toggleSemesterOpen(Long id, boolean open);

// ✅ THÊM
/**
 * Initialize scores for all students in a semester (manual trigger)
 */
Response initializeScoresForSemester(Long semesterId);
```

---

## 🔧 Bước 5: Cập Nhật AcademicServiceImpl

### File: `src/main/java/vn/campuslife/service/impl/AcademicServiceImpl.java`

**Thêm import:**
```java
import vn.campuslife.service.StudentScoreInitService;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
```

**Thêm field:**
```java
private final AcademicYearRepository yearRepo;
private final SemesterRepository semRepo;
private final StudentScoreInitService studentScoreInitService; // ✅ THÊM
```

**Cập nhật constructor:**
```java
public AcademicServiceImpl(
        AcademicYearRepository yearRepo, 
        SemesterRepository semRepo,
        StudentScoreInitService studentScoreInitService) { // ✅ THÊM
    this.yearRepo = yearRepo;
    this.semRepo = semRepo;
    this.studentScoreInitService = studentScoreInitService; // ✅ THÊM
}
```

**Cập nhật method `createSemester`:**
```java
@Override
@Transactional
public Response createSemester(SemesterRequest request) {
    AcademicYear year = yearRepo.findById(request.getYearId()).orElse(null);
    if (year == null)
        return new Response(false, "Year not found", null);
    Semester s = new Semester();
    s.setYear(year);
    s.setName(request.getName());
    s.setStartDate(request.getStartDate());
    s.setEndDate(request.getEndDate());
    if (request.getOpen() != null)
        s.setOpen(request.getOpen());
    Semester saved = semRepo.save(s);
    
    // ✅ THÊM: Tự động tạo điểm cho tất cả sinh viên nếu học kỳ được mở
    if (saved.isOpen()) {
        try {
            studentScoreInitService.initializeScoresForAllStudents(saved);
            log.info("Auto-initialized scores for all students in new semester {}", saved.getId());
        } catch (Exception e) {
            // Log error nhưng không fail việc tạo semester
            // Admin có thể gọi API thủ công sau
            log.error("Failed to auto-initialize scores for new semester {}: {}", 
                    saved.getId(), e.getMessage(), e);
        }
    }
    
    return new Response(true, "Semester created", saved);
}
```

**Thêm method mới:**
```java
@Override
@Transactional
public Response initializeScoresForSemester(Long semesterId) {
    Optional<Semester> semesterOpt = semRepo.findById(semesterId);
    if (semesterOpt.isEmpty()) {
        return new Response(false, "Semester not found", null);
    }
    
    Semester semester = semesterOpt.get();
    
    try {
        studentScoreInitService.initializeScoresForAllStudents(semester);
        
        Map<String, Object> result = new HashMap<>();
        result.put("semesterId", semester.getId());
        result.put("semesterName", semester.getName());
        result.put("message", "Scores initialized successfully for all students");
        
        return new Response(true, "Scores initialized successfully", result);
    } catch (Exception e) {
        log.error("Failed to initialize scores for semester {}: {}", 
                semesterId, e.getMessage(), e);
        return new Response(false, "Failed to initialize scores: " + e.getMessage(), null);
    }
}
```

---

## 🔧 Bước 6: Thêm Endpoint vào AcademicAdminController

### File: `src/main/java/vn/campuslife/controller/AcademicAdminController.java`

**Thêm endpoint:**
```java
@PostMapping("/semesters/{id}/toggle")
public ResponseEntity<Response> toggleSemester(@PathVariable Long id, @RequestParam("open") boolean open) {
    Response r = academicService.toggleSemesterOpen(id, open);
    return ResponseEntity.status(r.isStatus() ? 200 : 404).body(r);
}

// ✅ THÊM
@PostMapping("/semesters/{id}/initialize-scores")
public ResponseEntity<Response> initializeScoresForSemester(@PathVariable Long id) {
    Response r = academicService.initializeScoresForSemester(id);
    return ResponseEntity.status(r.isStatus() ? 200 : 500).body(r);
}
```

---

## 🔧 Bước 7: Cập Nhật ActivityRegistrationServiceImpl

### File: `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

**Thêm import:**
```java
import vn.campuslife.service.SemesterHelperService;
```

**Thêm field:**
```java
private final SemesterHelperService semesterHelperService; // ✅ THÊM
```

**Cập nhật method `updateStudentScoreFromParticipation`:**
```java
private void updateStudentScoreFromParticipation(ActivityParticipation participation) {
    try {
        Student student = participation.getRegistration().getStudent();
        Activity activity = participation.getRegistration().getActivity();

        // Validate activity có scoreType
        if (activity.getScoreType() == null) {
            logger.warn("Activity {} has no scoreType, skipping score update", activity.getId());
            return;
        }

        logger.debug("Updating score for student {} activity {} scoreType {} participation {}",
                student.getId(), activity.getId(), activity.getScoreType(), participation.getId());

        // ✅ THAY ĐỔI: Tìm semester dựa vào thời gian diễn ra activity
        Semester semester = semesterHelperService.getSemesterForActivity(activity);

        if (semester == null) {
            logger.warn("No semester found for activity {} score aggregation", activity.getId());
            return;
        }

        logger.debug("Using semester {} for activity {} (startDate: {})", 
                semester.getId(), activity.getId(), activity.getStartDate());

        // Tìm bản ghi StudentScore tổng hợp
        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        student.getId(),
                        semester.getId(), // ✅ Dùng semester từ activity
                        activity.getScoreType());

        if (scoreOpt.isEmpty()) {
            logger.warn("No aggregate score record found for student {} scoreType {} in semester {}. " +
                    "Creating new StudentScore record.",
                    student.getId(), activity.getScoreType(), semester.getId());
            
            // Tạo StudentScore mới nếu chưa có
            StudentScore newScore = new StudentScore();
            newScore.setStudent(student);
            newScore.setSemester(semester); // ✅ Dùng semester từ activity
            newScore.setScoreType(activity.getScoreType());
            newScore.setScore(BigDecimal.ZERO);
            scoreOpt = Optional.of(studentScoreRepository.save(newScore));
            logger.info("Created new StudentScore for student {} scoreType {} in semester {}",
                    student.getId(), activity.getScoreType(), semester.getId());
        }

        // ... rest of the method remains the same ...
    } catch (Exception e) {
        logger.error("Failed to update student score from participation: {}", e.getMessage(), e);
    }
}
```

**Cập nhật method `updateRenLuyenScoreFromParticipation`:**
```java
private void updateRenLuyenScoreFromParticipation(ActivityParticipation participation) {
    try {
        Student student = participation.getRegistration().getStudent();
        Activity activity = participation.getRegistration().getActivity();

        // ✅ THAY ĐỔI: Tìm semester dựa vào thời gian diễn ra activity
        Semester semester = semesterHelperService.getSemesterForActivity(activity);

        if (semester == null) {
            logger.warn("No semester found for RL score aggregation");
            return;
        }

        // Tìm bản ghi StudentScore REN_LUYEN
        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        student.getId(),
                        semester.getId(), // ✅ Dùng semester từ activity
                        ScoreType.REN_LUYEN);

        // ... rest of the method remains the same ...
    } catch (Exception e) {
        logger.error("Failed to update REN_LUYEN score: {}", e.getMessage(), e);
    }
}
```

---

## 🔧 Bước 8: Cập Nhật MiniGameServiceImpl

### File: `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`

**Thêm import:**
```java
import vn.campuslife.service.SemesterHelperService;
```

**Thêm field:**
```java
private final SemesterHelperService semesterHelperService; // ✅ THÊM
```

**Cập nhật method `updateStudentScoreFromMiniGame`:**
```java
private void updateStudentScoreFromMiniGame(Student student, Activity activity, BigDecimal points) {
    try {
        if (activity.getScoreType() == null) {
            logger.warn("Activity {} has no scoreType, skipping score update", activity.getId());
            return;
        }

        // ✅ THAY ĐỔI: Tìm semester dựa vào thời gian diễn ra activity
        Semester semester = semesterHelperService.getSemesterForActivity(activity);

        if (semester == null) {
            logger.warn("No semester found for score aggregation");
            return;
        }

        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        student.getId(),
                        semester.getId(), // ✅ Dùng semester từ activity
                        activity.getScoreType());

        // ... rest of the method remains the same ...
    } catch (Exception e) {
        logger.error("Failed to update student score from minigame: {}", e.getMessage(), e);
    }
}
```

---

## 🔧 Bước 9: Cập Nhật TaskSubmissionServiceImpl

### File: `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`

**Thêm import:**
```java
import vn.campuslife.service.SemesterHelperService;
```

**Thêm field:**
```java
private final SemesterHelperService semesterHelperService; // ✅ THÊM
```

**Cập nhật method `createScoreFromSubmission`:**
```java
private void createScoreFromSubmission(TaskSubmission submission) {
    try {
        logger.info("Creating score from submission {} with score {}", submission.getId(), submission.getScore());

        if (submission.getScore() == null || submission.getScore() <= 0) {
            logger.info("No score to create for submission {}", submission.getId());
            return;
        }

        ActivityTask task = submission.getTask();
        Activity activity = task.getActivity(); // ✅ Lấy activity từ task
        Student student = submission.getStudent();

        logger.info("Task: {}, Activity: {}, Student: {}, Grader: {}", 
                task.getId(), activity.getId(), student.getId(),
                submission.getGrader() != null ? submission.getGrader().getId() : "null");

        // ✅ THAY ĐỔI: Tìm semester dựa vào thời gian diễn ra activity
        Semester semester = semesterHelperService.getSemesterForActivity(activity);

        if (semester == null) {
            logger.warn("No semester found for score creation");
            return;
        }

        // Tìm bản ghi điểm tổng hợp theo scoreType của activity
        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        student.getId(),
                        semester.getId(), // ✅ Dùng semester từ activity
                        activity.getScoreType());

        // ... rest of the method remains the same ...
    } catch (Exception e) {
        logger.error("Failed to create score from submission: {}", e.getMessage(), e);
    }
}
```

---

## 🔧 Bước 10: Cập Nhật ActivitySeriesServiceImpl (Milestone Points)

### File: `src/main/java/vn/campuslife/service/impl/ActivitySeriesServiceImpl.java`

**Thêm import:**
```java
import vn.campuslife.service.SemesterHelperService;
```

**Thêm field:**
```java
private final SemesterHelperService semesterHelperService; // ✅ THÊM
```

**Cập nhật method `updateMilestoneScore`:**
```java
private void updateMilestoneScore(Long studentId, Long seriesId, ScoreType scoreType) {
    try {
        ActivitySeries series = seriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found"));

        if (series.getScoreType() == null) {
            logger.warn("Series {} has no scoreType, skipping milestone score update", seriesId);
            return;
        }

        // ✅ THAY ĐỔI: Tìm semester từ activity đầu tiên trong series
        // Hoặc có thể dùng logic khác tùy vào yêu cầu
        List<Activity> seriesActivities = activityRepository.findBySeriesIdAndIsDeletedFalse(seriesId);
        
        Semester semester = null;
        if (!seriesActivities.isEmpty()) {
            // Lấy activity có startDate sớm nhất
            Activity firstActivity = seriesActivities.stream()
                    .filter(a -> a.getStartDate() != null)
                    .min(Comparator.comparing(Activity::getStartDate))
                    .orElse(seriesActivities.get(0));
            
            semester = semesterHelperService.getSemesterForActivity(firstActivity);
        }
        
        // Fallback: Dùng semester đang mở
        if (semester == null) {
            semester = semesterRepository.findAll().stream()
                    .filter(Semester::isOpen)
                    .findFirst()
                    .orElse(semesterRepository.findAll().stream().findFirst().orElse(null));
        }

        if (semester == null) {
            logger.warn("No semester found for milestone score update");
            return;
        }

        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(studentId, semester.getId(), scoreType);

        // ... rest of the method remains the same ...
    } catch (Exception e) {
        logger.error("Failed to update milestone score: {}", e.getMessage(), e);
    }
}
```

---

## 🧪 Testing Guide

### Test 1: Tự Động Tạo Điểm Khi Tạo Học Kỳ Mới

**Bước 1:** Tạo học kỳ mới với `isOpen = true`

```bash
curl -X POST "http://localhost:8080/api/admin/academics/semesters" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "yearId": 1,
    "name": "Học kỳ 2 - 2024-2025",
    "startDate": "2025-01-15",
    "endDate": "2025-05-15",
    "open": true
  }'
```

**Kiểm tra:**
- Kiểm tra logs: Có thấy "Initializing scores for all students..."
- Query database: `SELECT COUNT(*) FROM student_scores WHERE semester_id = <new_semester_id>`
- Kết quả mong đợi: `COUNT = số_sinh_viên * 3` (3 loại điểm)

### Test 2: API Thủ Công Tạo Điểm

```bash
curl -X POST "http://localhost:8080/api/admin/academics/semesters/{semesterId}/initialize-scores" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Kiểm tra:**
- Response: `{"status": true, "message": "Scores initialized successfully", ...}`
- Database: Điểm được tạo cho tất cả sinh viên

### Test 3: Ghi Điểm Vào Đúng Học Kỳ

**Scenario:**
1. Tạo học kỳ 1 (01/09/2024 - 31/12/2024)
2. Tạo học kỳ 2 (01/01/2025 - 30/05/2025)
3. Tạo activity với `startDate = 15/02/2025` (trong học kỳ 2)
4. Sinh viên tham gia và hoàn thành activity

**Kiểm tra:**
- Query: `SELECT * FROM student_scores WHERE student_id = <student_id> AND activity_id = <activity_id>`
- Kết quả mong đợi: `semester_id` phải là ID của học kỳ 2

### Test 4: Activity Tạo Trước Khi Học Kỳ Mở

**Scenario:**
1. Tạo activity với `startDate = 15/02/2025` (trong tương lai)
2. Học kỳ 2 chưa được tạo
3. Tạo học kỳ 2 với `startDate = 01/01/2025`, `endDate = 30/05/2025`
4. Sinh viên tham gia activity

**Kiểm tra:**
- Điểm phải được ghi vào học kỳ 2 (không phải học kỳ hiện tại)

---

## ⚠️ Lưu Ý Quan Trọng

### 1. Migration Dữ Liệu Cũ
- Dữ liệu cũ có thể đã ghi điểm vào sai học kỳ
- Cần script migration để sửa lại dữ liệu cũ (nếu cần)

### 2. Performance
- Khi tạo học kỳ mới với nhiều sinh viên (1000+), quá trình có thể mất vài phút
- Nên chạy async hoặc có progress indicator

### 3. Transaction
- Tạo điểm cho tất cả sinh viên nên wrap trong transaction
- Nếu fail, có thể gọi lại API thủ công

### 4. Logging
- Tất cả các thay đổi đều có log để debug
- Kiểm tra logs khi có vấn đề

---

## 🔍 Troubleshooting

### Vấn đề 1: Điểm không tự động tạo khi tạo học kỳ mới

**Nguyên nhân:**
- `isOpen = false` → không tự động tạo
- Exception trong quá trình tạo → check logs

**Giải pháp:**
- Gọi API thủ công: `POST /api/admin/academics/semesters/{id}/initialize-scores`
- Check logs để xem lỗi cụ thể

### Vấn đề 2: Điểm ghi vào sai học kỳ

**Nguyên nhân:**
- Activity không có `startDate` → fallback về semester đang mở
- Activity `startDate` không nằm trong bất kỳ semester nào

**Giải pháp:**
- Kiểm tra `startDate` của activity có hợp lệ không
- Kiểm tra semester có `startDate` và `endDate` đúng không
- Check logs để xem semester nào được chọn

### Vấn đề 3: Performance chậm khi tạo điểm cho nhiều sinh viên

**Giải pháp:**
- Batch insert thay vì từng record (nếu cần optimize)
- Chạy async trong background job
- Monitor database performance

---

## ✅ Checklist Hoàn Thành

- [ ] Thêm query method vào `SemesterRepository`
- [ ] Tạo `SemesterHelperService`
- [ ] Cập nhật `StudentScoreInitService` với method mới
- [ ] Cập nhật `AcademicServiceImpl.createSemester()`
- [ ] Thêm API thủ công vào `AcademicService` và `AcademicAdminController`
- [ ] Cập nhật `ActivityRegistrationServiceImpl` để dùng `SemesterHelperService`
- [ ] Cập nhật `MiniGameServiceImpl` để dùng `SemesterHelperService`
- [ ] Cập nhật `TaskSubmissionServiceImpl` để dùng `SemesterHelperService`
- [ ] Cập nhật `ActivitySeriesServiceImpl` để dùng `SemesterHelperService`
- [ ] Test tạo học kỳ mới → điểm tự động tạo
- [ ] Test API thủ công
- [ ] Test ghi điểm vào đúng học kỳ
- [ ] Review logs và fix bugs (nếu có)

---

## 📝 Tóm Tắt

Sau khi implement, hệ thống sẽ:

1. ✅ **Tự động tạo điểm** cho tất cả sinh viên khi tạo học kỳ mới (nếu `isOpen = true`)
2. ✅ **Có API thủ công** để gọi lại nếu cần
3. ✅ **Ghi điểm vào đúng học kỳ** dựa vào thời gian diễn ra sự kiện (không phải thời gian tạo)

Điều này đảm bảo tính nhất quán và chính xác của dữ liệu điểm trong hệ thống.
```
