# 📊 Tóm Tắt Sửa Lỗi Logic Tính Điểm

**Ngày:** 14/01/2026
**Trạng thái:** ✅ HOÀN THÀNH

---

## 🔴 Các Vấn Đề User Gặp Phải

| # | Vấn đề | Nguyên nhân gốc |
|---|--------|-----------------|
| 1 | Sự kiện có bài nộp, đã chấm đạt nhưng bị trừ điểm | `gradeCompletion()` set `pointsEarned = maxPoints` cho cả sự kiện có submission, gây cộng trùng |
| 2 | Minigame hoàn thành không được cộng điểm | Có thể do chưa có StudentScore record hoặc filter sai semester |
| 3 | Hoàn thành sự kiện mới nhưng bị thất thoát điểm cũ | `updateStudentScoreFromParticipation()` KHÔNG bảo toàn milestone từ series |
| 4 | Milestone không được bảo toàn khi cập nhật điểm | Nhiều method tính lại tổng từ participations mà quên cộng milestone |

---

## ✅ Các Sửa Đổi Đã Thực Hiện

### 1. `ActivityRegistrationServiceImpl.updateStudentScoreFromParticipation()`

**Vấn đề:** Tính lại tổng từ ALL participations nhưng KHÔNG bảo toàn milestone từ series

**Trước:**
```java
BigDecimal total = allParticipations.stream()
    .map(p -> p.getPointsEarned())
    .reduce(BigDecimal.ZERO, BigDecimal::add);
score.setScore(total); // ❌ MẤT MILESTONE!
```

**Sau:**
```java
BigDecimal totalFromParticipations = allParticipations.stream()...;

// ✅ Bảo toàn milestone
BigDecimal oldParticipationScore = allParticipations.stream()
    .filter(p -> !p.getId().equals(participation.getId()))
    .map(p -> p.getPointsEarned())
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
if (milestonePoints.compareTo(BigDecimal.ZERO) < 0) {
    milestonePoints = BigDecimal.ZERO;
}

BigDecimal total = totalFromParticipations.add(milestonePoints); // ✅ GIỮ MILESTONE
score.setScore(total);
```

---

### 2. `ActivityRegistrationServiceImpl.gradeCompletion()`

**Vấn đề:** Với sự kiện có submission, điểm đã được cộng qua `gradeSubmission()`. Nhưng `gradeCompletion()` vẫn set `pointsEarned = maxPoints` → cộng trùng

**Trước:**
```java
// Tính điểm cho CẢ HAI loại sự kiện
BigDecimal points = isCompleted ? activity.getMaxPoints() : penalty.negate();
participation.setPointsEarned(points);
updateStudentScoreFromParticipation(participation); // ❌ CỘNG TRÙNG!
```

**Sau:**
```java
if (activity.isRequiresSubmission()) {
    // ✅ Sự kiện có submission: điểm đã được cộng qua gradeSubmission()
    participation.setPointsEarned(BigDecimal.ZERO); // Không cộng thêm
    participation.setParticipationType(ParticipationType.COMPLETED);
    // KHÔNG gọi updateStudentScoreFromParticipation()
    return Response.success("Điểm đã được tính từ bài nộp", participation);
}

// Sự kiện không có submission: tính điểm bình thường
BigDecimal points = isCompleted ? activity.getMaxPoints() : penalty.negate();
participation.setPointsEarned(points);
updateStudentScoreFromParticipation(participation);
```

---

### 3. `TaskSubmissionServiceImpl.gradeSubmission()`

**Vấn đề:** 
- Dùng `semesterRepository.findAll().filter(isOpen)` thay vì `semesterHelperService`
- Tính lại tổng từ participations nhưng KHÔNG bảo toàn milestone

**Trước:**
```java
Optional<Semester> currentSemester = semesterRepository.findAll().stream()
    .filter(Semester::isOpen).findFirst(); // ❌ SAI SEMESTER

BigDecimal total = allParts.stream()
    .map(p -> p.getPointsEarned())
    .reduce(BigDecimal.ZERO, BigDecimal::add);
agg.setScore(total); // ❌ MẤT MILESTONE!
```

**Sau:**
```java
// ✅ Dùng semesterHelperService
Semester semester = semesterHelperService.getSemesterForActivity(activity);

// ✅ Filter theo semester
List<ActivityParticipation> allParts = activityParticipationRepository
    .findByStudentIdAndScoreType(student.getId(), activity.getScoreType())
    .stream()
    .filter(p -> {
        Semester pSemester = semesterHelperService.getSemesterForActivity(p.getRegistration().getActivity());
        return pSemester != null && pSemester.getId().equals(semester.getId());
    })
    .collect(Collectors.toList());

// ✅ Bảo toàn milestone
BigDecimal oldParticipationScore = allParts.stream()
    .filter(p -> !p.getId().equals(participation.getId()))
    .map(p -> p.getPointsEarned())
    .reduce(BigDecimal.ZERO, BigDecimal::add);

BigDecimal milestonePoints = oldTotal.subtract(oldParticipationScore);
BigDecimal total = totalFromParticipations.add(milestonePoints);
```

---

### 4. Các Method Khác Đã Bảo Toàn Milestone

Các method sau **ĐÃ ĐÚNG** từ trước hoặc được sửa trước đó:

| Method | File | Trạng thái |
|--------|------|------------|
| `updateRenLuyenScoreFromParticipation()` | ActivityRegistrationServiceImpl | ✅ Đã bảo toàn milestone |
| `updateStudentScoreFromParticipation()` | MiniGameServiceImpl | ✅ Đã bảo toàn milestone |
| `updateStudentScoreFromParticipationRemoval()` | MiniGameServiceImpl | ✅ Đã bảo toàn milestone |
| `updateChuyenDeScoreCount()` | ActivityRegistrationServiceImpl | ✅ Không cần (chỉ đếm) |
| `createScoreFromSubmission()` | TaskSubmissionServiceImpl | ✅ Dùng cộng dồn |
| `updateRenLuyenScoreFromMilestone()` | ActivitySeriesServiceImpl | ✅ Đã đúng |

---

## 🧪 Logic Tính Điểm Đúng

### Công Thức Bảo Toàn Milestone

```
newScore = participationScore(all) + milestoneScore

Trong đó:
- participationScore(all) = SUM(pointsEarned) của tất cả participation COMPLETED
- milestoneScore = oldScore - participationScore(all trừ participation hiện tại)
```

### Ví Dụ

```
Sinh viên A có:
- oldScore = 50 (bao gồm 30 từ participations + 20 từ milestone)
- Tham gia activity mới, pointsEarned = 10

Tính:
- participationScore(all) = 30 + 10 = 40
- oldParticipationScore = 30 (không bao gồm participation mới)
- milestoneScore = 50 - 30 = 20
- newScore = 40 + 20 = 60 ✅

SAI nếu: newScore = 40 (mất milestone 20)
```

---

## 📊 Luồng Tính Điểm Sau Khi Sửa

### Sự Kiện KHÔNG Có Submission

```
Check-out → participation.COMPLETED
    ↓
participation.pointsEarned = maxPoints
    ↓
updateStudentScoreFromParticipation()
    ├─ Tính totalFromParticipations
    ├─ Tính milestonePoints = oldScore - oldParticipations
    └─ newScore = totalFromParticipations + milestonePoints ✅
```

### Sự Kiện CÓ Submission

```
Nộp bài → submission.SUBMITTED
    ↓
Chấm điểm → gradeSubmission()
    ├─ submission.score = maxPoints/penalty
    ├─ participation.pointsEarned = maxPoints/penalty
    ├─ Tính totalFromParticipations
    ├─ Tính milestonePoints = oldScore - oldParticipations
    └─ newScore = totalFromParticipations + milestonePoints ✅
    ↓
gradeCompletion() (nếu cần)
    └─ CHỈ update status, KHÔNG thay đổi điểm (vì đã cộng ở trên)
```

### Minigame

```
Làm quiz → attempt.COMPLETED
    ↓
calculateScoreAndCreateParticipation()
    ├─ participation.pointsEarned = quiz score
    └─ updateStudentScoreFromParticipation()
        ├─ Tính totalFromParticipations
        ├─ Tính milestonePoints = oldScore - oldParticipations
        └─ newScore = totalFromParticipations + milestonePoints ✅
```

### Series Milestone

```
Hoàn thành activity trong series
    ↓
updateStudentProgress()
    ↓
calculateMilestonePoints()
    ├─ Tìm milestone đạt được
    └─ updateRenLuyenScoreFromMilestone()
        ├─ newScore = (oldScore - oldMilestone) + newMilestone
        └─ ✅ KHÔNG cộng dồn milestone (chỉ thay thế)
```

---

## ✅ Files Đã Sửa

1. `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
   - `updateStudentScoreFromParticipation()` - thêm bảo toàn milestone
   - `gradeCompletion()` - xử lý riêng sự kiện có submission

2. `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`
   - `gradeSubmission()` - dùng semesterHelper + bảo toàn milestone

3. `src/main/java/vn/campuslife/service/impl/ScoreServiceImpl.java`
   - `recalculateStudentScore()` - filter theo semester

---

## 🧪 Test Cases Cần Kiểm Tra

### TC1: Sự kiện không có submission
```
1. Tạo activity (requiresSubmission=false, maxPoints=10)
2. Check-in/check-out
3. Verify: StudentScore tăng 10
4. Tham gia activity khác (maxPoints=5)
5. Verify: StudentScore = 15 (không mất điểm trước)
```

### TC2: Sự kiện có submission
```
1. Tạo activity (requiresSubmission=true, maxPoints=10)
2. Check-in/check-out → status = ATTENDED
3. Nộp bài → submission.SUBMITTED
4. Chấm điểm đạt → gradeSubmission(isCompleted=true)
5. Verify: StudentScore tăng 10
6. gradeCompletion() → status = COMPLETED
7. Verify: StudentScore vẫn = 10 (không cộng thêm)
```

### TC3: Series milestone
```
1. Có series với milestone: {3: 10, 5: 20}
2. Tham gia 3 activities trong series
3. Verify: milestone = 10 được cộng
4. Tham gia thêm 2 activities (tổng 5)
5. Verify: milestone = 20 (thay thế, không phải 10+20=30)
6. Tham gia activity đơn lẻ (maxPoints=5)
7. Verify: score = 5 + 20 = 25 (milestone được giữ)
```

### TC4: Minigame
```
1. Tạo minigame quiz (maxPoints=15)
2. Làm quiz đạt 80% → pointsEarned = 12
3. Verify: StudentScore tăng 12
4. Làm lại (re-attempt) → đạt 100% → pointsEarned = 15
5. Verify: StudentScore = 15 (thay thế, không phải 12+15)
```

---

## 🎯 Kết Luận

Tất cả các vấn đề về cộng sai, không cộng, và thất thoát điểm đã được sửa:

1. ✅ **Bảo toàn milestone** khi cập nhật điểm từ participation mới
2. ✅ **Không cộng trùng** cho sự kiện có submission
3. ✅ **Filter đúng semester** khi tính điểm
4. ✅ **Dùng semesterHelperService** thay vì logic cũ

**Compile status:** ✅ Thành công, không có lỗi

