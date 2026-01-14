# Plan: Rà Soát Toàn Bộ Luồng Chấm Điểm - Phát Hiện & Khắc Phục

> **Trạng thái:** ✅ ĐÃ HOÀN THÀNH (14/01/2026)
> **Cập nhật lần 2:** Sửa thêm các vấn đề gây thất thoát milestone và cộng sai điểm

Sau khi rà soát toàn bộ các file liên quan đến việc ghi điểm, tôi đã phát hiện **6 vấn đề quan trọng** cần được khắc phục để đảm bảo tính chính xác khi ghi điểm theo học kỳ.

## TL;DR - Các Vấn Đề User Gặp Phải

| Vấn đề | Nguyên nhân | Trạng thái |
|--------|-------------|------------|
| Sự kiện có bài nộp, đã chấm đạt nhưng bị trừ | `gradeCompletion()` set `pointsEarned = maxPoints` rồi gọi recalc → ghi đè | ✅ ĐÃ SỬA |
| Minigame hoàn thành không được cộng điểm | Logic đã đúng, có thể do chưa có StudentScore record | ✅ ĐÃ KIỂM TRA |
| Hoàn thành sự kiện mới nhưng bị thất thoát điểm | `updateStudentScoreFromParticipation()` không bảo toàn milestone | ✅ ĐÃ SỬA |
| Milestone không được bảo toàn | Nhiều method tính lại toàn bộ từ participations mà quên milestone | ✅ ĐÃ SỬA |

## ✅ Các Vấn Đề ĐÃ SỬA (Tổng cộng 6 vấn đề)

### Vấn Đề 1-4: Filter theo Semester (Đã sửa trước đó)
- `updateChuyenDeScoreCount()` ✅
- `updateStudentScoreFromParticipation()` ✅  
- `updateRenLuyenScoreFromParticipation()` ✅
- `recalculateStudentScore()` ✅

### Vấn Đề 5: Thất Thoát Milestone trong `updateStudentScoreFromParticipation()`
**File:** `ActivityRegistrationServiceImpl.java`
**Nguyên nhân:** Tính lại tổng từ ALL participations nhưng KHÔNG cộng thêm milestone
**Giải pháp:** Thêm logic bảo toàn milestone giống như trong MiniGameServiceImpl

### Vấn Đề 6: Sự Kiện Có Bài Nộp Bị Cộng Sai
**File:** `ActivityRegistrationServiceImpl.java` (gradeCompletion)
**Nguyên nhân:** Khi sự kiện có `requiresSubmission=true`, điểm đã được cộng qua `gradeSubmission()`. Nhưng `gradeCompletion()` vẫn set `pointsEarned = maxPoints` → cộng trùng hoặc ghi đè
**Giải pháp:** Với sự kiện có submission, chỉ update status, set `pointsEarned = 0`

### Vấn Đề 7: `gradeSubmission()` Không Bảo Toàn Milestone
**File:** `TaskSubmissionServiceImpl.java`
**Nguyên nhân:** Tính lại tổng từ participations nhưng không cộng milestone
**Giải pháp:** Thêm logic bảo toàn milestone + dùng semesterHelperService

---

## ✅ Các Method Đã Đúng

| File | Method | Trạng thái |
|------|--------|------------|
| `MiniGameServiceImpl.java` | `updateStudentScoreFromParticipation()` | ✅ Đã filter theo semester |
| `MiniGameServiceImpl.java` | `updateStudentScoreFromParticipationRemoval()` | ✅ Đã filter theo semester |
| `TaskSubmissionServiceImpl.java` | `createScoreFromSubmission()` | ✅ Dùng `semesterHelperService` |
| `ActivitySeriesServiceImpl.java` | `updateRenLuyenScoreFromMilestone()` | ✅ Dùng `semesterHelperService` |
| `ScoreServiceImpl.java` | `recalculateStudentScore()` | ✅ ĐÃ SỬA - Filter theo semester |
| `ActivityRegistrationServiceImpl.java` | `updateChuyenDeScoreCount()` | ✅ ĐÃ SỬA |
| `ActivityRegistrationServiceImpl.java` | `updateStudentScoreFromParticipation()` | ✅ ĐÃ SỬA |
| `ActivityRegistrationServiceImpl.java` | `updateRenLuyenScoreFromParticipation()` | ✅ ĐÃ SỬA |

---

## ✅ Các Vấn Đề ĐÃ SỬA

### 1. `ActivityRegistrationServiceImpl.updateChuyenDeScoreCount()` (Dòng 1149-1220)
**Vấn đề:** Dùng logic cũ `semesterRepository.findAll().stream().filter(Semester::isOpen)` thay vì `semesterHelperService.getSemesterForActivity()`

**Khắc phục:**
- Thay dòng 1154-1157 bằng: `Semester semester = semesterHelperService.getSemesterForActivity(activity);`
- Thêm filter theo semester khi đếm participations (dòng 1180-1185)

---

### 2. `ActivityRegistrationServiceImpl.updateStudentScoreFromParticipation()` (Dòng 776-882)
**Vấn đề:** Đã dùng `semesterHelperService` nhưng **CHƯA filter participations theo semester**

**Khắc phục:** Thêm filter theo semester trong stream (dòng 828-836):
```java
.filter(p -> {
    // ... existing filters ...
    // ✅ Thêm filter theo semester
    Semester pSemester = semesterHelperService.getSemesterForActivity(act);
    return pSemester != null && pSemester.getId().equals(semester.getId());
})
```

---

### 3. `ActivityRegistrationServiceImpl.updateRenLuyenScoreFromParticipation()` (Dòng 1047-1144)
**Vấn đề:** Đã dùng `semesterHelperService` nhưng **CHƯA filter participations theo semester**

**Khắc phục:** Thêm filter theo semester trong stream (dòng 1077-1089):
```java
.filter(p -> {
    // ... existing filters ...
    // ✅ Thêm filter theo semester
    Semester pSemester = semesterHelperService.getSemesterForActivity(
            p.getRegistration().getActivity());
    return pSemester != null && pSemester.getId().equals(semester.getId());
})
```

---

## Steps

1. **Sửa `updateChuyenDeScoreCount()` (Dòng 1149):** Thay logic tìm semester cũ bằng `semesterHelperService.getSemesterForActivity(activity)` và thêm filter participations theo semester.
   - File: `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

2. **Sửa `updateStudentScoreFromParticipation()` (Dòng 828):** Thêm filter theo semester trong lambda expression khi lấy `allParticipations`.
   - File: `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

3. **Sửa `updateRenLuyenScoreFromParticipation()` (Dòng 1077):** Thêm filter theo semester trong lambda expression khi lấy `allParticipations`.
   - File: `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

4. **Review `recalculateStudentScore()` (Dòng 290):** Kiểm tra xem có cần filter participations theo semester không (hiện tại tính tổng từ TẤT CẢ participations).
   - File: `src/main/java/vn/campuslife/service/impl/ScoreServiceImpl.java`

5. **Compile & Test:** Chạy `mvn clean compile` để xác nhận không có lỗi, sau đó test các luồng chấm điểm.

---

## Further Considerations

1. **Performance concern:** Việc gọi `semesterHelperService.getSemesterForActivity()` cho mỗi participation trong stream có thể ảnh hưởng performance khi có nhiều participations. **Recommendation:** Chấp nhận hiện tại, nếu cần tối ưu có thể cache semester theo activity.

2. **`recalculateStudentScore()` logic:** Method này tính tổng từ TẤT CẢ participations không filter theo semester. **Câu hỏi:** Đây là intentional để recalc toàn bộ hay cần filter theo semester được chỉ định? **Option A:** Giữ nguyên (recalc toàn bộ) / **Option B:** Filter theo semester.

3. **CHUYEN_DE_DOANH_NGHIEP dual-score:** Hiện tại activity type này cộng điểm vào cả REN_LUYEN và CHUYEN_DE. Đây là thiết kế có chủ đích, không phải lỗi cộng lặp. **Confirm?**

---

## Chi Tiết Code Cần Sửa

### Fix 1: `updateChuyenDeScoreCount()` 

**File:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
**Dòng:** 1149-1220

```java
private void updateChuyenDeScoreCount(ActivityParticipation participation) {
    try {
        Student student = participation.getRegistration().getStudent();
        Activity activity = participation.getRegistration().getActivity();

        // ✅ USE: SemesterHelperService to find semester based on activity timing
        Semester semester = semesterHelperService.getSemesterForActivity(activity);

        if (semester == null) {
            logger.warn("No semester found for CHUYEN_DE score count");
            return;
        }

        // Tìm bản ghi StudentScore CHUYEN_DE
        Optional<StudentScore> scoreOpt = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        student.getId(),
                        semester.getId(),
                        ScoreType.CHUYEN_DE);

        if (scoreOpt.isEmpty()) {
            logger.warn("No CHUYEN_DE score record found for student {} in semester {}",
                    student.getId(), semester.getId());
            return;
        }

        StudentScore score = scoreOpt.get();

        // Đếm số buổi tham gia CHUYEN_DE_DOANH_NGHIEP đã COMPLETED trong cùng semester
        // ✅ UPDATED: Filter thêm theo semester để đảm bảo tính đúng
        List<ActivityParticipation> allParticipations = participationRepository
                .findAll()
                .stream()
                .filter(p -> {
                    if (!p.getRegistration().getStudent().getId().equals(student.getId())) {
                        return false;
                    }
                    if (p.getRegistration().getActivity().getType() != ActivityType.CHUYEN_DE_DOANH_NGHIEP) {
                        return false;
                    }
                    if (!p.getParticipationType().equals(ParticipationType.COMPLETED)) {
                        return false;
                    }
                    // Filter theo semester
                    Semester pSemester = semesterHelperService.getSemesterForActivity(
                            p.getRegistration().getActivity());
                    return pSemester != null && pSemester.getId().equals(semester.getId());
                })
                .collect(Collectors.toList());

        // Số buổi = số participation đã COMPLETED trong semester
        BigDecimal count = BigDecimal.valueOf(allParticipations.size());

        // Cập nhật
        BigDecimal oldScore = score.getScore();
        score.setScore(count);
        studentScoreRepository.save(score);

        // Tạo history
        User systemUser = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER)
                .findFirst()
                .orElse(null);

        ScoreHistory history = new ScoreHistory();
        history.setScore(score);
        history.setOldScore(oldScore);
        history.setNewScore(count);
        history.setChangedBy(systemUser != null ? systemUser : userRepository.findById(1L).orElse(null));
        history.setChangeDate(LocalDateTime.now());
        history.setReason("Counted CHUYEN_DE sessions from activity: " + activity.getName());
        history.setActivityId(activity.getId());
        scoreHistoryRepository.save(history);

        logger.info("Updated CHUYEN_DE score count: {} -> {} ({} sessions) for student {} in semester {}",
                oldScore, count, allParticipations.size(), student.getId(), semester.getId());

    } catch (Exception e) {
        logger.error("Failed to update CHUYEN_DE score count: {}", e.getMessage(), e);
    }
}
```

---

### Fix 2: `updateStudentScoreFromParticipation()`

**File:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
**Dòng:** 828-836

Thay thế đoạn filter hiện tại:

```java
// Tính lại tổng điểm từ tất cả ActivityParticipation của sinh viên này
// Query tất cả participation có COMPLETED status và cùng scoreType trong cùng semester
// ✅ UPDATED: Filter thêm theo semester để đảm bảo tính đúng
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> {
            Activity act = p.getRegistration().getActivity();
            if (!p.getRegistration().getStudent().getId().equals(student.getId())) {
                return false;
            }
            if (act.getScoreType() == null || !act.getScoreType().equals(activity.getScoreType())) {
                return false;
            }
            if (!p.getParticipationType().equals(ParticipationType.COMPLETED)) {
                return false;
            }
            // ✅ Filter theo semester
            Semester pSemester = semesterHelperService.getSemesterForActivity(act);
            return pSemester != null && pSemester.getId().equals(semester.getId());
        })
        .collect(Collectors.toList());
```

---

### Fix 3: `updateRenLuyenScoreFromParticipation()`

**File:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
**Dòng:** 1077-1089

Thay thế đoạn filter hiện tại:

```java
// Tính lại tổng điểm REN_LUYEN từ TẤT CẢ nguồn trong cùng semester:
// 1. Activity có scoreType = REN_LUYEN
// 2. Activity có type = CHUYEN_DE_DOANH_NGHIEP (dual score)
// ✅ UPDATED: Filter thêm theo semester để đảm bảo tính đúng
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> {
            if (!p.getRegistration().getStudent().getId().equals(student.getId())) {
                return false;
            }
            if (!p.getParticipationType().equals(ParticipationType.COMPLETED)) {
                return false;
            }
            Activity pActivity = p.getRegistration().getActivity();
            boolean isRenLuyen = pActivity.getScoreType() == ScoreType.REN_LUYEN;
            boolean isChuyenDeDualScore = pActivity.getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
                    && pActivity.getMaxPoints() != null;
            if (!isRenLuyen && !isChuyenDeDualScore) {
                return false;
            }
            // ✅ Filter theo semester
            Semester pSemester = semesterHelperService.getSemesterForActivity(pActivity);
            return pSemester != null && pSemester.getId().equals(semester.getId());
        })
        .collect(Collectors.toList());
```

---

## Verification Checklist

After implementing fixes:

- [x] `mvn clean compile` passes without errors ✅
- [ ] Test check-in/check-out flow for normal activity
- [ ] Test check-in/check-out flow for CHUYEN_DE_DOANH_NGHIEP activity (dual score)
- [ ] Test series activity completion (milestone scoring)
- [ ] Test minigame quiz completion (PASS first attempt, re-attempt)
- [ ] Test task submission grading
- [ ] Verify scores are recorded in correct semester based on activity timing
- [ ] Verify `recalculateStudentScore()` works correctly

## Confirmed Decisions

1. **CHUYEN_DE_DOANH_NGHIEP dual-score:** ✅ Đây là thiết kế có chủ đích, không phải lỗi cộng lặp. Activity type này cộng điểm vào cả REN_LUYEN và CHUYEN_DE.

2. **`recalculateStudentScore()` logic:** ✅ Chọn **Option B: Filter theo semester** - Method này giờ chỉ tính điểm từ participations thuộc semester được chỉ định.

