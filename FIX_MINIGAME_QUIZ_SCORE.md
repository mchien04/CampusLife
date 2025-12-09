# Sửa Lỗi Tính Điểm Quiz (Minigame)

## VẤN ĐỀ

**Triệu chứng:**
- Điểm hiện tại: 20đ (có thể từ milestone series hoặc activities khác)
- Sau khi hoàn thành quiz: Điểm còn 10đ (bị giảm thay vì tăng)
- Điểm quiz không được cộng đúng

**Nguyên nhân:**
- Method `updateStudentScoreFromParticipation()` trong `MiniGameServiceImpl` đang tính lại TỔNG điểm từ TẤT CẢ participations
- Không giữ lại điểm milestone từ series
- Ghi đè toàn bộ điểm hiện có → Mất điểm milestone

---

## GIẢI PHÁP ĐÃ SỬA

### 1. Sửa Logic Cộng Điểm Quiz

**File:** `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`

**Method:** `updateStudentScoreFromParticipation()`

**Logic mới:**
1. Tính tổng điểm từ TẤT CẢ participations (bao gồm participation mới)
2. Tính điểm milestone = điểm hiện tại - điểm từ participations cũ (không có participation mới)
3. Tổng điểm mới = participations mới + milestone (giữ nguyên)

**Code:**
```java
// Tính tổng điểm từ tất cả participations (bao gồm participation mới)
BigDecimal totalFromParticipations = allParticipations.stream()
        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

// Giữ nguyên điểm milestone từ series
BigDecimal oldScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;

// Tính điểm từ participations CŨ (không bao gồm participation hiện tại)
BigDecimal oldParticipationScore = allParticipations.stream()
        .filter(p -> !p.getId().equals(participation.getId()))
        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

// Điểm milestone = điểm hiện tại - điểm từ participations cũ
BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
if (milestonePoints.compareTo(BigDecimal.ZERO) < 0) {
    milestonePoints = BigDecimal.ZERO;
}

// Tổng điểm MỚI = điểm từ participations MỚI + điểm milestone (giữ nguyên)
BigDecimal total = totalFromParticipations.add(milestonePoints);
```

### 2. Sửa Logic Xóa Participation (Re-attempt)

**Method:** `updateStudentScoreFromParticipationRemoval()`

**Logic mới:**
- Tương tự như trên, giữ nguyên milestone khi xóa participation

---

## LOGIC TÍNH ĐIỂM QUIZ HOÀN CHỈNH

### Khi Student Submit Quiz (PASSED):

```
1. Tính điểm từ rewardPoints
   → pointsEarned = miniGame.getRewardPoints()
   
2. Tạo ActivityParticipation
   → participation.pointsEarned = rewardPoints
   → participation.participationType = COMPLETED
   
3. Cập nhật StudentScore
   → Tính lại tổng điểm từ participations
   → Giữ nguyên điểm milestone từ series
   → Tổng = participations + milestone
```

### Ví Dụ Cụ Thể:

**Trường hợp 1: Chỉ có quiz, không có milestone**

**Ban đầu:**
- REN_LUYEN score: 0đ

**Hoàn thành quiz 1 (rewardPoints = 10đ):**
- Participation 1: pointsEarned = 10đ
- Milestone: 0đ
- REN_LUYEN score: 10đ ✅

**Hoàn thành quiz 2 (rewardPoints = 5đ):**
- Participation 1: 10đ
- Participation 2: 5đ
- Milestone: 0đ
- REN_LUYEN score: 15đ ✅

**Trường hợp 2: Có milestone từ series**

**Ban đầu:**
- REN_LUYEN score: 20đ (từ milestone series)

**Hoàn thành quiz 1 (rewardPoints = 10đ):**
- oldScore: 20đ
- oldParticipationScore: 0đ (chưa có participation nào)
- milestonePoints: 20đ - 0đ = 20đ
- totalFromParticipations: 10đ (participation mới)
- REN_LUYEN score: 10đ + 20đ = 30đ ✅

**Hoàn thành quiz 2 (rewardPoints = 5đ):**
- oldScore: 30đ
- oldParticipationScore: 10đ (chỉ có quiz 1)
- milestonePoints: 30đ - 10đ = 20đ (giữ nguyên)
- totalFromParticipations: 10đ + 5đ = 15đ
- REN_LUYEN score: 15đ + 20đ = 35đ ✅

---

## ĐẢM BẢO ĐIỂM TỪ REWARDPOINTS

### Khi Tạo Minigame:

**Request:**
```json
{
  "activityId": 1,
  "title": "Quiz kiến thức IT",
  "rewardPoints": 10.0,
  "questions": [...]
}
```

**Kết quả:**
- `MiniGame.rewardPoints = 10.0`
- Khi student PASSED quiz → `ActivityParticipation.pointsEarned = 10.0`
- Điểm được cộng vào StudentScore đúng với `rewardPoints`

### Logic Đảm Bảo:

1. ✅ Điểm quiz lấy từ `miniGame.getRewardPoints()` (không phải `activity.maxPoints`)
2. ✅ Lưu vào `participation.pointsEarned`
3. ✅ Cộng vào StudentScore với logic giữ milestone
4. ✅ Không bị ghi đè bởi các participations khác

---

## FILES ĐÃ THAY ĐỔI

- `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`
  - Method: `updateStudentScoreFromParticipation()` (dòng 393-480)
  - Method: `updateStudentScoreFromParticipationRemoval()` (dòng 485-550)

---

## KIỂM TRA

### Test Case 1: Quiz không có milestone

1. Tạo minigame với `rewardPoints = 10đ`
2. Student hoàn thành quiz (PASSED)
3. Kiểm tra:
   - ActivityParticipation: `pointsEarned = 10đ`
   - StudentScore: `score = 10đ` ✅

### Test Case 2: Quiz có milestone

1. Student có milestone từ series: 20đ
2. Tạo minigame với `rewardPoints = 10đ`
3. Student hoàn thành quiz (PASSED)
4. Kiểm tra:
   - ActivityParticipation: `pointsEarned = 10đ`
   - StudentScore: `score = 20đ + 10đ = 30đ` ✅

### Test Case 3: Nhiều quiz

1. Student có milestone: 20đ
2. Quiz 1: `rewardPoints = 10đ` → Score: 30đ ✅
3. Quiz 2: `rewardPoints = 5đ` → Score: 35đ ✅
4. Quiz 3: `rewardPoints = 15đ` → Score: 50đ ✅

---

## GHI CHÚ

- Logic này đảm bảo điểm milestone từ series KHÔNG bị ghi đè
- Điểm quiz được cộng đúng với `rewardPoints` khi tạo minigame
- Mỗi lần tính lại, hệ thống:
  1. Tính tổng điểm từ participations
  2. Tính điểm milestone = điểm hiện tại - điểm participations cũ
  3. Cộng lại: participations mới + milestone
- Nếu có vấn đề, kiểm tra log để xem:
  - `oldScore`: Điểm hiện tại
  - `oldParticipationScore`: Điểm từ participations cũ
  - `milestonePoints`: Điểm milestone được giữ lại
  - `totalFromParticipations`: Tổng điểm từ participations mới
  - `total`: Tổng điểm cuối cùng

