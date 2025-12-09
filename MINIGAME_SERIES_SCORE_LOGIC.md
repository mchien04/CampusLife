# Logic Tính Điểm Minigame Trong Series

## 1. RewardPoints và Null

### Entity MiniGame
- `rewardPoints` (BigDecimal) - **Đã cho phép null** (không có `nullable = false`)
- Có thể set `rewardPoints = null` khi tạo minigame

### Validation Khi Tạo Minigame

**Nếu activity thuộc series (`activity.getSeriesId() != null`):**
- `rewardPoints` **có thể null** hoặc 0
- Điểm sẽ được tính từ milestone points của series
- Chỉ warning, không fail

**Nếu activity đơn lẻ (`activity.getSeriesId() == null`):**
- `rewardPoints` **nên có giá trị > 0**
- Warning nếu null hoặc <= 0 (nhưng không fail)
- Điểm sẽ được tính từ `rewardPoints`

---

## 2. Logic Tính Điểm Khi Pass Quiz

### 2.1. Quiz Trong Series

**Flow:**
```
1. Student pass quiz → status = PASSED
   ↓
2. calculateScoreAndCreateParticipation()
   ↓
3. Tạo ActivityParticipation:
   - participationType = COMPLETED
   - isCompleted = true
   - pointsEarned = 0 (KHÔNG tính từ rewardPoints)
   ↓
4. Gọi updateStudentProgress(studentId, activityId)
   - completedCount++ trong series progress
   - Tính lại milestone points (nếu đạt mốc)
   - Cộng milestone points vào StudentScore (scoreType từ series)
```

**Code:**
```java
if (activity.getSeriesId() != null) {
    // Activity trong series → KHÔNG tính điểm từ rewardPoints
    participation.setPointsEarned(BigDecimal.ZERO);
    
    // Update series progress (điểm milestone sẽ được tính tự động)
    activitySeriesService.updateStudentProgress(student.getId(), activity.getId());
}
```

### 2.2. Quiz Đơn Lẻ

**Flow:**
```
1. Student pass quiz → status = PASSED
   ↓
2. calculateScoreAndCreateParticipation()
   ↓
3. Validation: Nếu rewardPoints <= 0, return early (không tạo participation)
   ↓
4. Tạo ActivityParticipation:
   - participationType = COMPLETED
   - isCompleted = true
   - pointsEarned = rewardPoints
   ↓
5. Cập nhật StudentScore
   - Cộng rewardPoints vào StudentScore (scoreType từ activity)
```

**Code:**
```java
// Validation: Chỉ return early nếu activity đơn lẻ và không có điểm
if (activity.getSeriesId() == null && pointsEarned.compareTo(BigDecimal.ZERO) <= 0) {
    return Response.success("No points to award", null);
}

if (activity.getSeriesId() == null) {
    // Activity đơn lẻ → tính điểm bình thường từ rewardPoints
    participation.setPointsEarned(pointsEarned);
    updateStudentScoreFromParticipation(participation);
}
```

---

## 3. Logic Tính Milestone Points

### 3.1. updateStudentProgress()

**Logic:**
- Đếm tất cả activities (cả activity thường và minigame pass) vào `completedCount`
- Dựa trên `activityId`, không phân biệt loại activity
- Gọi `calculateMilestonePoints()` để tính lại milestone

### 3.2. calculateMilestonePoints()

**Logic:**
- Chỉ tính milestone points từ configuration JSON
- Không cộng thêm điểm từ quiz `rewardPoints`
- Cộng milestone points vào `StudentScore` theo `scoreType` của series

**Code:**
```java
// Tìm milestone cao nhất mà student đã đạt được
for (Map.Entry<String, Integer> entry : milestonePoints.entrySet()) {
    Integer milestoneCount = Integer.parseInt(entry.getKey());
    if (completedCount >= milestoneCount) {
        Integer milestonePointsValue = entry.getValue();
        if (milestonePointsValue > pointsToAward.intValue()) {
            pointsToAward = BigDecimal.valueOf(milestonePointsValue);
        }
    }
}

// Cập nhật StudentScore
updateRenLuyenScoreFromMilestone(studentId, seriesId, oldPoints, pointsToAward);
```

### 3.3. updateRenLuyenScoreFromMilestone()

**Logic MỚI (đã sửa):**
- Tính lại tổng điểm từ tất cả participations COMPLETED
- Tổng điểm = participations + milestone mới
- **KHÔNG ghi đè** điểm từ participations

**Code:**
```java
// Tính lại tổng điểm từ participations
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> p.getRegistration().getStudent().getId().equals(studentId)
                && p.getRegistration().getActivity().getScoreType().equals(scoreType)
                && p.getParticipationType().equals(ParticipationType.COMPLETED))
        .collect(Collectors.toList());

BigDecimal totalFromParticipations = allParticipations.stream()
        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

// Tổng điểm MỚI = điểm từ participations + milestone mới
BigDecimal updatedScore = totalFromParticipations.add(newPoints);
score.setScore(updatedScore);
```

---

## 4. Đảm Bảo Không Bị Ghi Đè, Sót, Tính Sai

### 4.1. Quiz Trong Series

**Đảm bảo:**
- ✅ `pointsEarned = 0` (không cộng rewardPoints)
- ✅ Vẫn tạo participation để update series progress
- ✅ Milestone points được tính từ configuration
- ✅ Không ghi đè điểm từ participations khác

### 4.2. Quiz Đơn Lẻ

**Đảm bảo:**
- ✅ `pointsEarned = rewardPoints` (nếu > 0)
- ✅ Cộng vào StudentScore đúng scoreType
- ✅ Giữ nguyên milestone points (nếu có)

### 4.3. Milestone Points

**Đảm bảo:**
- ✅ Tính lại từ participations + milestone mới
- ✅ Không ghi đè điểm từ participations
- ✅ Chỉ tính từ milestone configuration, không cộng quiz rewardPoints

### 4.4. Logic Tính Điểm Tổng

**Công thức:**
```
StudentScore.total = 
    (Tổng điểm từ participations COMPLETED) 
    + (Điểm milestone từ series - được giữ nguyên)
```

**Ví dụ:**
- Participations: 5đ + 3đ = 8đ
- Milestone: 10đ
- **Total: 8đ + 10đ = 18đ** ✅

---

## 5. Test Cases

### 5.1. Quiz Trong Series (rewardPoints = null)

**Setup:**
- Series có milestone: {"2": 5, "3": 10}
- Tạo quiz trong series với `rewardPoints = null`

**Test:**
1. Student pass quiz 1 → completedCount = 1, milestone = 0
2. Student pass quiz 2 → completedCount = 2, milestone = 5
3. Student pass quiz 3 → completedCount = 3, milestone = 10

**Kiểm tra:**
- ✅ Participation có `pointsEarned = 0`
- ✅ Series progress `completedCount` tăng đúng
- ✅ Milestone points được tính đúng
- ✅ StudentScore chỉ có milestone points, không có quiz rewardPoints

### 5.2. Quiz Đơn Lẻ (rewardPoints = 10)

**Setup:**
- Quiz đơn lẻ với `rewardPoints = 10`

**Test:**
1. Student pass quiz → participation có `pointsEarned = 10`
2. StudentScore được cộng 10đ

**Kiểm tra:**
- ✅ Participation có `pointsEarned = 10`
- ✅ StudentScore được cộng đúng 10đ

### 5.3. Chuỗi Kết Hợp (Activity Thường + Quiz)

**Setup:**
- Series có milestone: {"2": 5, "3": 10}
- 2 activity thường + 1 quiz (rewardPoints = null)

**Test:**
1. Student check-out activity 1 → completedCount = 1
2. Student check-out activity 2 → completedCount = 2, milestone = 5
3. Student pass quiz → completedCount = 3, milestone = 10

**Kiểm tra:**
- ✅ Quiz participation có `pointsEarned = 0`
- ✅ Series progress đếm đúng cả activity thường và quiz
- ✅ Milestone points được tính đúng

### 5.4. Không Ghi Đè Điểm

**Setup:**
- Student có milestone = 10đ, participations = 5đ → total = 15đ
- Pass quiz mới trong series → completedCount tăng, milestone = 15đ

**Test:**
- Milestone update: 10đ → 15đ

**Kiểm tra:**
- ✅ Participations vẫn = 5đ (không bị ghi đè)
- ✅ Milestone = 15đ
- ✅ Total = 5đ + 15đ = 20đ ✅

---

## 6. Tóm Tắt

### RewardPoints
- ✅ Đã cho phép null trong entity
- ✅ Quiz trong series: có thể null (tính từ milestone)
- ✅ Quiz đơn lẻ: nên có giá trị > 0 (tính từ rewardPoints)

### Logic Tính Điểm
- ✅ Quiz trong series: `pointsEarned = 0`, update series progress
- ✅ Quiz đơn lẻ: `pointsEarned = rewardPoints`, update StudentScore
- ✅ Milestone points: tính lại từ participations + milestone mới
- ✅ Không ghi đè điểm từ participations

### Đảm Bảo
- ✅ Không bị sót điểm
- ✅ Không bị ghi đè điểm
- ✅ Tính đúng điểm tổng

