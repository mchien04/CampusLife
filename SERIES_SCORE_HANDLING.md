# Logic Xử Lý Điểm Cho Activity Trong Series

## TÓM TẮT

Activity trong series vẫn dùng entity `Activity` có sẵn, nhưng:
- Cho phép **null** các thuộc tính không cần thiết
- Điểm **KHÔNG** tính từ `maxPoints` của activity
- Điểm chỉ tính từ **milestone của series** khi đạt mốc

---

## 1. CẤU TRÚC DỮ LIỆU

### Activity trong Series:
```java
Activity {
  id: 1,
  name: "Sự kiện 1",
  seriesId: 1,           // ✅ Cần
  seriesOrder: 1,        // ✅ Cần
  maxPoints: null,       // ❌ Không dùng (cho phép null)
  scoreType: null,       // ❌ Không dùng (lấy từ series)
  type: null,            // ❌ Không dùng (cho phép null)
  registrationStartDate: null,  // ❌ Không dùng
  registrationDeadline: null,   // ❌ Không dùng
  requiresApproval: true,       // ❌ Không dùng (lấy từ series)
  ticketQuantity: null,        // ❌ Không dùng
  penaltyPointsIncomplete: null, // ❌ Không dùng
  // ✅ Các thuộc tính khác vẫn cần: name, description, startDate, endDate, location, etc.
}
```

### ActivitySeries (đã cập nhật):
```java
ActivitySeries {
  id: 1,
  name: "Chuỗi sự kiện mùa hè",
  milestonePoints: "{\"3\": 5, \"4\": 7, \"5\": 10}",
  scoreType: REN_LUYEN,  // ✅ MỚI: Loại điểm để cộng milestone
  // Các thuộc tính quy định khác có thể thêm sau nếu cần
}
```

---

## 2. LOGIC XỬ LÝ ĐIỂM

### Khi Student check-in Activity trong Series:

```
Step 1: Check-out thành công
  ↓
Step 2: Tạo ActivityParticipation
  - pointsEarned = 0  (KHÔNG tính từ maxPoints)
  - isCompleted = true
  ↓
Step 3: Update Series Progress
  - completedCount++  (tăng số sự kiện đã tham gia)
  - Thêm activityId vào completedActivityIds
  ↓
Step 4: Calculate Milestone Points
  - Kiểm tra completedCount có đạt mốc không
  - Nếu đạt → Cộng điểm milestone vào StudentScore
  - Loại điểm: lấy từ ActivitySeries.scoreType
```

### Code Implementation:

```java
// Trong ActivityRegistrationServiceImpl.checkIn()
if (activity.getSeriesId() != null) {
    // Activity trong series → KHÔNG tính điểm từ maxPoints
    participation.setPointsEarned(BigDecimal.ZERO);
    participation.setParticipationType(ParticipationType.COMPLETED);
    participationRepository.save(participation);
    
    // Chỉ update series progress
    // → calculateMilestonePoints() sẽ được gọi tự động
    // → Điểm milestone sẽ được cộng vào StudentScore (scoreType từ series)
    activitySeriesService.updateStudentProgress(...);
} else {
    // Activity đơn lẻ → tính điểm bình thường từ maxPoints
    BigDecimal points = activity.getMaxPoints() != null 
        ? activity.getMaxPoints() 
        : BigDecimal.ZERO;
    participation.setPointsEarned(points);
    updateStudentScoreFromParticipation(participation);
}
```

---

## 3. VÍ DỤ CỤ THỂ

### Scenario: Chuỗi 5 sự kiện, milestone {3: 5đ, 4: 7đ, 5: 10đ}

#### Student tham gia activity 1, 2, 3:

```
Activity 1 (seriesId=1, maxPoints=null):
  Check-out → pointsEarned = 0
  Series Progress: completedCount = 1

Activity 2 (seriesId=1, maxPoints=null):
  Check-out → pointsEarned = 0
  Series Progress: completedCount = 2

Activity 3 (seriesId=1, maxPoints=null):
  Check-out → pointsEarned = 0
  Series Progress: completedCount = 3
  → Đạt mốc 3 → Cộng 5 điểm vào StudentScore (ScoreType.REN_LUYEN)
```

#### Student tham gia thêm activity 4:

```
Activity 4 (seriesId=1, maxPoints=null):
  Check-out → pointsEarned = 0
  Series Progress: completedCount = 4
  → Đạt mốc 4 → Cập nhật: Trừ 5đ cũ + Cộng 7đ mới = +2 điểm
```

#### Student tham gia thêm activity 5:

```
Activity 5 (seriesId=1, maxPoints=null):
  Check-out → pointsEarned = 0
  Series Progress: completedCount = 5
  → Đạt mốc 5 → Cập nhật: Trừ 7đ cũ + Cộng 10đ mới = +3 điểm
```

**Tổng điểm nhận được:** 10 điểm RL (từ milestone, không phải từ maxPoints của từng activity)

---

## 4. ĐIỂM QUAN TRỌNG

### ✅ Đúng:
- Activity trong series: `maxPoints = null` (không dùng)
- Điểm chỉ tính từ milestone của series
- Loại điểm lấy từ `ActivitySeries.scoreType`

### ❌ Sai:
- Tính điểm từ `activity.maxPoints` cho activity trong series
- Tính điểm 2 lần (từ activity + từ milestone)
- Hardcode loại điểm (phải lấy từ series)

---

## 5. API TẠO SERIES (ĐÃ CẬP NHẬT)

```json
POST /api/series
{
  "name": "Chuỗi sự kiện mùa hè 2024",
  "description": "Các sự kiện trong mùa hè",
  "milestonePoints": "{\"3\": 5, \"4\": 7, \"5\": 10}",
  "scoreType": "REN_LUYEN",  // ✅ MỚI: Bắt buộc
  "mainActivityId": null
}
```

**Lưu ý:** `scoreType` là bắt buộc, thường là `REN_LUYEN` hoặc `CONG_TAC_XA_HOI`

---

## 6. KẾT LUẬN

- ✅ Activity trong series vẫn dùng entity Activity (cho phép null các thuộc tính không cần)
- ✅ Điểm KHÔNG tính từ `maxPoints` của activity
- ✅ Điểm chỉ tính từ milestone của series khi đạt mốc
- ✅ Loại điểm lấy từ `ActivitySeries.scoreType`

