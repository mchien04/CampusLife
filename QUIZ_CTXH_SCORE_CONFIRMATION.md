# Xác Nhận: Quiz CTXH Không Bị Ghi Đè

## ✅ XÁC NHẬN

**Quiz có điểm cộng là CTXH thì cũng KHÔNG bị ghi đè.**

---

## LOGIC ĐẢM BẢO

### 1. Filter Đúng Theo ScoreType

**Code trong `MiniGameServiceImpl.updateStudentScoreFromParticipation()`:**

```java
// Tính lại tổng điểm từ tất cả ActivityParticipation COMPLETED
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                && p.getRegistration().getActivity().getScoreType().equals(activity.getScoreType()) // ← Filter đúng scoreType
                && p.getParticipationType().equals(ParticipationType.COMPLETED))
        .collect(java.util.stream.Collectors.toList());
```

**Kết quả:**
- Nếu quiz có `scoreType = CONG_TAC_XA_HOI`
- Chỉ tính participations có `scoreType = CONG_TAC_XA_HOI`
- Không tính participations có `scoreType = REN_LUYEN` hoặc `CHUYEN_DE`

### 2. Giữ Nguyên Điểm Milestone

**Code:**
```java
// QUAN TRỌNG: Giữ nguyên điểm milestone từ series (nếu có)
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

**Kết quả:**
- Điểm milestone CTXH (nếu có) được giữ nguyên
- Điểm từ participations CTXH được cộng dồn
- Không bị ghi đè

---

## VÍ DỤ CỤ THỂ

### Trường hợp 1: Quiz CTXH, không có milestone

**Ban đầu:**
- CTXH score: 0đ

**Tham gia activity CTXH 1 (maxPoints = 5đ):**
- Participation 1: pointsEarned = 5đ
- CTXH score: 5đ ✅

**Hoàn thành quiz CTXH (rewardPoints = 10đ):**
- Participation 2: pointsEarned = 10đ
- Milestone: 0đ
- CTXH score: 5đ + 10đ = 15đ ✅

**Tham gia activity CTXH 2 (maxPoints = 3đ):**
- Participation 3: pointsEarned = 3đ
- Milestone: 0đ
- CTXH score: 5đ + 10đ + 3đ = 18đ ✅

### Trường hợp 2: Quiz CTXH, có milestone từ series

**Ban đầu:**
- CTXH score: 20đ (từ milestone series CTXH)

**Hoàn thành quiz CTXH (rewardPoints = 10đ):**
- oldScore: 20đ
- oldParticipationScore: 0đ (chưa có participation CTXH nào)
- milestonePoints: 20đ - 0đ = 20đ (giữ nguyên)
- totalFromParticipations: 10đ (quiz mới)
- CTXH score: 10đ + 20đ = 30đ ✅

**Tham gia activity CTXH (maxPoints = 5đ):**
- oldScore: 30đ
- oldParticipationScore: 10đ (chỉ có quiz)
- milestonePoints: 30đ - 10đ = 20đ (giữ nguyên)
- totalFromParticipations: 10đ + 5đ = 15đ
- CTXH score: 15đ + 20đ = 35đ ✅

### Trường hợp 3: Quiz CTXH, có activities CTXH khác

**Ban đầu:**
- CTXH score: 0đ

**Tham gia activity CTXH 1 (maxPoints = 5đ):**
- Participation 1: pointsEarned = 5đ
- CTXH score: 5đ ✅

**Hoàn thành quiz CTXH (rewardPoints = 10đ):**
- oldScore: 5đ
- oldParticipationScore: 5đ (activity CTXH 1)
- milestonePoints: 5đ - 5đ = 0đ
- totalFromParticipations: 5đ + 10đ = 15đ
- CTXH score: 15đ + 0đ = 15đ ✅

**Tham gia activity CTXH 2 (maxPoints = 3đ):**
- oldScore: 15đ
- oldParticipationScore: 5đ + 10đ = 15đ
- milestonePoints: 15đ - 15đ = 0đ
- totalFromParticipations: 5đ + 10đ + 3đ = 18đ
- CTXH score: 18đ + 0đ = 18đ ✅

---

## KẾT LUẬN

### ✅ Đảm Bảo:

1. **Filter đúng scoreType:**
   - Quiz CTXH chỉ tính participations CTXH
   - Không tính participations REN_LUYEN hoặc CHUYEN_DE

2. **Giữ nguyên milestone:**
   - Điểm milestone CTXH (nếu có) được giữ nguyên
   - Không bị ghi đè khi tính lại từ participations

3. **Cộng dồn đúng:**
   - Điểm từ quiz CTXH được cộng vào
   - Điểm từ activities CTXH khác được cộng vào
   - Tất cả được cộng dồn đúng

### ✅ Quiz CTXH KHÔNG bị ghi đè!

**Logic áp dụng cho TẤT CẢ scoreType:**
- REN_LUYEN ✅
- CONG_TAC_XA_HOI ✅
- CHUYEN_DE ✅

