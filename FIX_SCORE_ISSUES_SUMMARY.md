# Tóm Tắt Sửa Lỗi Tính Điểm

## VẤN ĐỀ 1: CTXH không được cộng điểm

**Triệu chứng:**
- Activity CTXH đã tạo với `type = CONG_TAC_XA_HOI`, `scoreType = CONG_TAC_XA_HOI`
- ActivityParticipation đã có `pointsEarned`
- Nhưng StudentScore (CONG_TAC_XA_HOI) không được cập nhật

**Nguyên nhân:**
- Logic `updateStudentScoreFromParticipation()` đã đúng
- Có thể do lỗi runtime hoặc filter không khớp

**Giải pháp:**
- Kiểm tra log khi check-out activity CTXH
- Đảm bảo:
  1. `activity.scoreType == CONG_TAC_XA_HOI`
  2. `participation.participationType == COMPLETED`
  3. Không có exception trong quá trình thực thi

**Hành động:**
- Thêm log chi tiết trong method `updateStudentScoreFromParticipation()`
- Test lại và gửi log nếu vẫn lỗi

---

## VẤN ĐỀ 2: REN_LUYEN bị reset khi tham gia CHUYEN_DE_DOANH_NGHIEP

**Triệu chứng:**
- Điểm REN_LUYEN cũ: 10đ (có thể từ milestone series)
- Tham gia activity CHUYEN_DE_DOANH_NGHIEP (maxPoints = 5đ)
- Điểm REN_LUYEN sau: 5đ (bị reset, mất 10đ cũ)

**Nguyên nhân:**
- Method `updateRenLuyenScoreFromParticipation()` chỉ tính điểm từ participations
- Không tính điểm milestone từ series
- Ghi đè toàn bộ điểm REN_LUYEN hiện có

**Giải pháp ĐÃ SỬA:**

### Bước 1: Tính điểm từ TẤT CẢ participations REN_LUYEN

```java
// Tính lại tổng điểm REN_LUYEN từ TẤT CẢ nguồn:
// 1. Activity có scoreType = REN_LUYEN
// 2. Activity có type = CHUYEN_DE_DOANH_NGHIEP (dual score)
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                && p.getParticipationType().equals(ParticipationType.COMPLETED)
                && (
                    // Activity có scoreType = REN_LUYEN
                    (p.getRegistration().getActivity().getScoreType() == ScoreType.REN_LUYEN)
                    ||
                    // Hoặc activity CHUYEN_DE_DOANH_NGHIEP có maxPoints (dual score)
                    (p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
                        && p.getRegistration().getActivity().getMaxPoints() != null)
                ))
        .collect(Collectors.toList());

BigDecimal totalFromParticipations = allParticipations.stream()
        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### Bước 2: Giữ nguyên điểm milestone từ series

```java
// QUAN TRỌNG: Giữ nguyên điểm milestone từ series
// Tính điểm milestone = điểm hiện tại - điểm từ participations cũ
BigDecimal oldScore = score.getScore() != null ? score.getScore() : BigDecimal.ZERO;

// Tính điểm từ participations CŨ (không bao gồm participation hiện tại)
BigDecimal oldParticipationScore = allParticipations.stream()
        .filter(p -> !p.getId().equals(participation.getId())) // Loại bỏ participation hiện tại
        .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

// Điểm milestone = điểm hiện tại - điểm từ participations cũ
BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
if (milestonePoints.compareTo(BigDecimal.ZERO) < 0) {
    milestonePoints = BigDecimal.ZERO; // Không cho âm
}
```

### Bước 3: Tổng điểm = participations + milestone

```java
// Tổng điểm MỚI = điểm từ participations MỚI + điểm milestone (giữ nguyên)
BigDecimal total = totalFromParticipations.add(milestonePoints);

// Cập nhật
score.setScore(total);
studentScoreRepository.save(score);
```

---

## LOGIC TÍNH ĐIỂM HOÀN CHỈNH

### Điểm REN_LUYEN bao gồm:

1. **Điểm từ Activity có scoreType = REN_LUYEN**
   - Tính từ `ActivityParticipation.pointsEarned`
   - Mỗi participation COMPLETED = cộng điểm

2. **Điểm từ CHUYEN_DE_DOANH_NGHIEP (dual score)**
   - Tính từ `activity.maxPoints`
   - Lưu vào `ActivityParticipation.pointsEarned`

3. **Điểm milestone từ series**
   - Tính từ `StudentSeriesProgress.pointsEarned`
   - Được cộng vào StudentScore bởi `ActivitySeriesServiceImpl.updateRenLuyenScoreFromMilestone()`
   - KHÔNG bị ghi đè khi tính lại từ participations

### Công thức tính:

```
REN_LUYEN score = 
    (Tổng điểm từ participations REN_LUYEN) 
    + (Tổng điểm từ participations CHUYEN_DE_DOANH_NGHIEP) 
    + (Điểm milestone từ series - được giữ nguyên)
```

---

## VÍ DỤ CỤ THỂ

### Trường hợp 1: Chỉ có participations

**Ban đầu:**
- REN_LUYEN score: 0đ

**Tham gia activity 1 (scoreType = REN_LUYEN, maxPoints = 3đ):**
- Participation 1: pointsEarned = 3đ
- REN_LUYEN score: 3đ

**Tham gia activity 2 (CHUYEN_DE_DOANH_NGHIEP, maxPoints = 5đ):**
- Participation 2: pointsEarned = 5đ
- REN_LUYEN score: 3đ + 5đ = 8đ ✅

### Trường hợp 2: Có milestone từ series

**Ban đầu:**
- REN_LUYEN score: 0đ

**Tham gia series (đạt milestone 3 sự kiện = 5đ):**
- Milestone: 5đ
- REN_LUYEN score: 5đ

**Tham gia activity 1 (scoreType = REN_LUYEN, maxPoints = 3đ):**
- Participation 1: pointsEarned = 3đ
- Milestone: 5đ (giữ nguyên)
- REN_LUYEN score: 3đ + 5đ = 8đ ✅

**Tham gia activity 2 (CHUYEN_DE_DOANH_NGHIEP, maxPoints = 5đ):**
- Participation 1: 3đ
- Participation 2: 5đ
- Milestone: 5đ (giữ nguyên)
- REN_LUYEN score: 3đ + 5đ + 5đ = 13đ ✅

### Trường hợp 3: Milestone tăng sau khi có participations

**Ban đầu:**
- REN_LUYEN score: 0đ

**Tham gia activity 1 (scoreType = REN_LUYEN, maxPoints = 3đ):**
- Participation 1: 3đ
- REN_LUYEN score: 3đ

**Tham gia series (đạt milestone 3 sự kiện = 5đ):**
- Participation 1: 3đ
- Milestone: 5đ (mới)
- REN_LUYEN score: 3đ + 5đ = 8đ ✅

**Tham gia activity 2 (CHUYEN_DE_DOANH_NGHIEP, maxPoints = 5đ):**
- oldScore: 8đ
- oldParticipationScore: 3đ
- milestonePoints: 8đ - 3đ = 5đ (giữ nguyên)
- totalFromParticipations: 3đ + 5đ = 8đ
- REN_LUYEN score: 8đ + 5đ = 13đ ✅

---

## FILES ĐÃ THAY ĐỔI

- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
  - Method: `updateRenLuyenScoreFromParticipation()` (dòng 974-1100)

---

## KIỂM TRA

### Test case 1: CTXH không được cộng điểm

1. Tạo activity CTXH với `scoreType = CONG_TAC_XA_HOI`, `maxPoints = 5đ`
2. Sinh viên check-out
3. Kiểm tra:
   - ActivityParticipation: `pointsEarned = 5đ`, `participationType = COMPLETED`
   - StudentScore (CONG_TAC_XA_HOI): `score = 5đ`
4. Nếu vẫn lỗi: Gửi log chi tiết

### Test case 2: REN_LUYEN không bị reset

1. Tạo series với milestone (3 sự kiện = 5đ)
2. Sinh viên tham gia đủ 3 sự kiện → REN_LUYEN: 5đ
3. Tạo activity CHUYEN_DE_DOANH_NGHIEP với `maxPoints = 5đ`
4. Sinh viên check-out
5. Kiểm tra:
   - REN_LUYEN score: 5đ (milestone) + 5đ (CHUYEN_DE) = 10đ ✅
6. Tham gia thêm activity REN_LUYEN với `maxPoints = 3đ`
7. Kiểm tra:
   - REN_LUYEN score: 5đ (milestone) + 5đ (CHUYEN_DE) + 3đ (REN_LUYEN) = 13đ ✅

---

## GHI CHÚ

- Logic này đảm bảo điểm milestone từ series KHÔNG bị ghi đè
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

