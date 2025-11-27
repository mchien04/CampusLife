# Logic Xử Lý Điểm Cho Activity Trong Series

## VẤN ĐỀ

- Activity trong series vẫn dùng entity Activity (cho phép null các thuộc tính không cần)
- Chưa rõ cách xử lý điểm khi check-in activity trong series

## GIẢI PHÁP

### 1. Cấu trúc dữ liệu

#### Activity trong Series:
```java
Activity {
  id: 1,
  name: "Sự kiện 1",
  seriesId: 1,  // Thuộc series
  seriesOrder: 1,
  maxPoints: null,  // KHÔNG dùng để tính điểm
  scoreType: null,  // Có thể null (lấy từ series)
  type: null,  // Có thể null
  registrationStartDate: null,  // Lấy từ series
  registrationDeadline: null,  // Lấy từ series
  // ... các thuộc tính khác vẫn cần
}
```

#### ActivitySeries cần thêm:
```java
ActivitySeries {
  id: 1,
  name: "Chuỗi sự kiện mùa hè",
  milestonePoints: "{\"3\": 5, \"4\": 7, \"5\": 10}",
  scoreType: REN_LUYEN,  // Loại điểm để cộng milestone
  // ... các thuộc tính quy định khác
}
```

### 2. Logic xử lý điểm

#### Khi Student check-in Activity trong Series:

```
1. Check-in/Check-out thành công
   ↓
2. Tạo ActivityParticipation
   - pointsEarned = 0 (KHÔNG tính từ maxPoints)
   - isCompleted = true
   ↓
3. Update Series Progress
   - completedCount++
   - Thêm activityId vào completedActivityIds
   ↓
4. Tính Milestone Points
   - Kiểm tra completedCount có đạt mốc không
   - Nếu đạt → Cộng điểm milestone vào StudentScore
   - Loại điểm: lấy từ ActivitySeries.scoreType
```

### 3. Code Implementation

#### A. Thêm scoreType vào ActivitySeries:

```java
@Entity
@Table(name = "activity_series")
public class ActivitySeries {
    // ... existing fields
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("Loại điểm để cộng milestone (thường là REN_LUYEN)")
    private ScoreType scoreType;
}
```

#### B. Sửa logic check-in:

```java
// Trong ActivityRegistrationServiceImpl.checkIn()
if (!activity.isRequiresSubmission()) {
    participation.setIsCompleted(true);
    
    // XỬ LÝ ĐIỂM:
    if (activity.getSeriesId() != null) {
        // Activity trong series → KHÔNG tính điểm từ maxPoints
        participation.setPointsEarned(BigDecimal.ZERO);
        participation.setParticipationType(ParticipationType.COMPLETED);
        participationRepository.save(participation);
        
        // KHÔNG gọi updateStudentScoreFromParticipation() 
        // (vì không có điểm từ activity)
        
        // Chỉ update series progress
        activitySeriesService.updateStudentProgress(
            registration.getStudent().getId(), 
            activity.getId());
        // → calculateMilestonePoints() sẽ được gọi tự động
        // → Điểm milestone sẽ được cộng vào StudentScore
        
    } else {
        // Activity đơn lẻ → tính điểm bình thường
        BigDecimal points = activity.getMaxPoints() != null 
            ? activity.getMaxPoints() 
            : BigDecimal.ZERO;
        participation.setPointsEarned(points);
        participation.setParticipationType(ParticipationType.COMPLETED);
        participationRepository.save(participation);
        
        // Cập nhật StudentScore từ activity.maxPoints
        updateStudentScoreFromParticipation(participation);
        
        // Dual Score cho CHUYEN_DE_DOANH_NGHIEP
        if (activity.getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP 
            && activity.getMaxPoints() != null) {
            updateRenLuyenScoreFromParticipation(participation);
        }
    }
}
```

#### C. Sửa calculateMilestonePoints() để cộng đúng loại điểm:

```java
// Trong ActivitySeriesServiceImpl.calculateMilestonePoints()
private void updateRenLuyenScoreFromMilestone(...) {
    // Thay vì hardcode ScoreType.REN_LUYEN
    // → Lấy từ series.getScoreType()
    
    Optional<StudentScore> scoreOpt = studentScoreRepository
        .findByStudentIdAndSemesterIdAndScoreType(
            studentId, 
            currentSemester.getId(), 
            series.getScoreType());  // ← Lấy từ series
    // ...
}
```

### 4. Flow hoàn chỉnh

#### Khi Student check-in Activity trong Series:

```
Step 1: Check-out thành công
  → ActivityParticipation: pointsEarned = 0, isCompleted = true

Step 2: Update Series Progress
  → StudentSeriesProgress.completedCount = 3 (ví dụ)
  → completedActivityIds = [1, 2, 3]

Step 3: Calculate Milestone
  → Kiểm tra milestonePoints: {"3": 5, "4": 7, "5": 10}
  → completedCount = 3 → Đạt mốc 3 → Điểm = 5
  → Cộng 5 điểm vào StudentScore (ScoreType từ series.scoreType)

Step 4: Nếu student tham gia thêm 1 activity nữa
  → completedCount = 4 → Đạt mốc 4 → Điểm = 7
  → Cập nhật: Trừ điểm cũ (5) + Cộng điểm mới (7) = +2 điểm
```

### 5. Xử lý trường hợp đặc biệt

#### A. Student tham gia activity trong series nhưng chưa đăng ký series:
- **Không cho phép** check-in nếu chưa có SeriesRegistration
- Hoặc tự động tạo SeriesRegistration khi check-in lần đầu

#### B. Student đăng ký series:
- Tự động tạo ActivityRegistration cho TẤT CẢ activities trong series
- Tất cả có status = APPROVED (nếu series không cần duyệt)

#### C. Milestone points thay đổi:
- Khi completedCount tăng → Kiểm tra lại milestone
- Chỉ cộng thêm phần chênh lệch (không cộng lại từ đầu)

---

## TÓM TẮT

### Activity trong Series:
- ✅ `seriesId`, `seriesOrder`: Cần
- ✅ `name`, `description`, `startDate`, `endDate`, `location`: Cần
- ❌ `maxPoints`: null (không dùng)
- ❌ `scoreType`: null (lấy từ series)
- ❌ `type`: null (có thể null)
- ❌ `registrationStartDate`, `registrationDeadline`: null (lấy từ series)

### ActivitySeries cần thêm:
- ✅ `scoreType`: Loại điểm để cộng milestone (REN_LUYEN, CONG_TAC_XA_HOI, etc.)

### Logic tính điểm:
1. Activity trong series: `pointsEarned = 0` (không tính từ maxPoints)
2. Series progress: `completedCount++`
3. Milestone: Tính điểm từ milestonePoints, cộng vào StudentScore (scoreType từ series)

