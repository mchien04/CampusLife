# Logic Tính Điểm Quiz (Minigame) - Đã Sửa

## 1. ĐẢM BẢO ĐIỂM TỪ REWARDPOINTS

### ✅ Đã Đúng

**Khi tạo minigame:**
- `rewardPoints` được lưu vào `MiniGame.rewardPoints`
- Khi student PASSED quiz → `ActivityParticipation.pointsEarned = rewardPoints`
- Điểm được cộng vào StudentScore đúng với `rewardPoints`

**Code:**
```java
// Tính điểm từ rewardPoints
BigDecimal pointsEarned = miniGame.getRewardPoints() != null
        ? miniGame.getRewardPoints()
        : BigDecimal.ZERO;

participation.setPointsEarned(pointsEarned);
```

---

## 2. ĐIỂM CTXH KHÔNG BỊ GHI ĐÈ

### ✅ Đã Sửa

**Logic hiện tại:**
- Method `updateStudentScoreFromParticipation()` đã được sửa để giữ nguyên điểm milestone
- Áp dụng cho TẤT CẢ scoreType (REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE)
- Filter đúng theo `activity.getScoreType()`

**Code:**
```java
// Tính lại tổng điểm từ tất cả ActivityParticipation COMPLETED
List<ActivityParticipation> allParticipations = participationRepository
        .findAll()
        .stream()
        .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
                && p.getRegistration().getActivity().getScoreType().equals(activity.getScoreType()) // ← Filter đúng scoreType
                && p.getParticipationType().equals(ParticipationType.COMPLETED))
        .collect(java.util.stream.Collectors.toList());

// Giữ nguyên điểm milestone
BigDecimal milestonePoints = oldScore.subtract(oldParticipationScore);
BigDecimal total = totalFromParticipations.add(milestonePoints);
```

**Kết quả:**
- ✅ Điểm CTXH từ quiz được cộng đúng
- ✅ Điểm CTXH từ activities khác không bị mất
- ✅ Điểm milestone (nếu có) được giữ nguyên

---

## 3. ĐIỂM CHỈ ĐƯỢC GHI NHẬN 1 LẦN

### ✅ Đã Sửa

**Yêu cầu:**
- Quiz cho phép làm lại sau khi pass
- Điểm quiz chỉ được ghi nhận 1 lần từ khi pass lần đầu
- Sau khi đã pass, dù làm lại thành fail cũng không mất điểm
- Sau khi đã pass, dù làm lại pass cũng không cộng thêm

**Logic mới:**
```java
// Kiểm tra xem đã có participation COMPLETED chưa
Optional<ActivityParticipation> existingParticipationOpt = participationRepository
        .findByRegistration(registration);
if (existingParticipationOpt.isPresent()) {
    ActivityParticipation existingParticipation = existingParticipationOpt.get();
    // Nếu đã có participation COMPLETED (đã pass trước đó)
    if (existingParticipation.getParticipationType() == ParticipationType.COMPLETED
            && existingParticipation.getIsCompleted()) {
        // Không tạo lại, không cộng điểm thêm
        return Response.success("Participation already exists. Points already awarded.", existingParticipation);
    }
}
```

**Flow:**
1. **Lần đầu PASSED:**
   - Tạo ActivityParticipation
   - Cộng điểm vào StudentScore
   - ✅ Điểm được ghi nhận

2. **Làm lại PASSED (đã pass trước đó):**
   - Kiểm tra: Đã có participation COMPLETED
   - Không tạo lại participation
   - Không cộng điểm thêm
   - ✅ Điểm không thay đổi

3. **Làm lại FAILED (đã pass trước đó):**
   - Kiểm tra: Đã có participation COMPLETED
   - Không tạo participation mới
   - Không trừ điểm
   - ✅ Điểm không thay đổi

---

## 4. MAX_ATTEMPTS = NULL = KHÔNG GIỚI HẠN

### ✅ Đã Đúng

**Entity:**
```java
@Column
@Comment("Số lần làm quiz tối đa (null = không giới hạn)")
private Integer maxAttempts;
```

**Logic kiểm tra:**
```java
// Kiểm tra maxAttempts
if (miniGame.getMaxAttempts() != null) {
    List<MiniGameAttempt> allAttempts = attemptRepository.findByStudentIdAndMiniGameId(studentId, miniGameId);
    int totalAttempts = allAttempts.size();
    if (totalAttempts >= miniGame.getMaxAttempts()) {
        return Response.error("Bạn đã đạt số lần làm quiz tối đa (" + miniGame.getMaxAttempts() + " lần)");
    }
}
// Nếu maxAttempts == null → Không kiểm tra → Không giới hạn
```

**Kết quả:**
- ✅ `maxAttempts = null` → Không giới hạn số lần làm
- ✅ `maxAttempts = 3` → Chỉ cho phép làm tối đa 3 lần

---

## 5. TÓM TẮT LOGIC HOÀN CHỈNH

### Khi Student Submit Quiz:

```
1. Submit quiz với answers
   ↓
2. Tính số câu đúng (correctCount)
   ↓
3. Xác định status:
   - correctCount >= requiredCorrectAnswers → PASSED
   - correctCount < requiredCorrectAnswers → FAILED
   ↓
4. Nếu PASSED:
   a) Kiểm tra đã có participation COMPLETED chưa
      - Nếu có → Không làm gì (điểm đã được ghi nhận)
      - Nếu chưa → Tạo participation và cộng điểm
   b) Điểm = rewardPoints (từ minigame)
   c) Cộng vào StudentScore (giữ milestone)
   ↓
5. Nếu FAILED:
   - Không tạo participation
   - Không trừ điểm
   - Chỉ lưu attempt với status = FAILED
```

### Khi Student Làm Lại Quiz (Re-attempt):

**Trường hợp 1: Đã PASSED trước đó**
- Làm lại PASSED → Không cộng điểm thêm ✅
- Làm lại FAILED → Không mất điểm ✅

**Trường hợp 2: Chưa PASSED (chỉ có FAILED)**
- Làm lại PASSED → Cộng điểm ✅
- Làm lại FAILED → Không làm gì ✅

---

## 6. VÍ DỤ CỤ THỂ

### Ví dụ 1: Quiz CTXH, rewardPoints = 10đ

**Lần 1: PASSED**
- Tạo participation: pointsEarned = 10đ
- CTXH score: 0đ → 10đ ✅

**Lần 2: Làm lại PASSED**
- Kiểm tra: Đã có participation COMPLETED
- Không tạo lại participation
- CTXH score: 10đ (không đổi) ✅

**Lần 3: Làm lại FAILED**
- Kiểm tra: Đã có participation COMPLETED
- Không tạo participation mới
- CTXH score: 10đ (không đổi) ✅

### Ví dụ 2: Quiz REN_LUYEN, rewardPoints = 5đ, có milestone 20đ

**Lần 1: PASSED**
- Tạo participation: pointsEarned = 5đ
- REN_LUYEN score: 20đ (milestone) → 25đ (20đ + 5đ) ✅

**Lần 2: Làm lại PASSED**
- Kiểm tra: Đã có participation COMPLETED
- Không tạo lại participation
- REN_LUYEN score: 25đ (không đổi) ✅

### Ví dụ 3: Quiz FAILED lần đầu, PASSED lần 2

**Lần 1: FAILED**
- Không tạo participation
- Score: 0đ (không đổi)

**Lần 2: PASSED**
- Tạo participation: pointsEarned = 10đ
- Score: 0đ → 10đ ✅

**Lần 3: Làm lại PASSED**
- Kiểm tra: Đã có participation COMPLETED
- Không tạo lại participation
- Score: 10đ (không đổi) ✅

---

## 7. FILES ĐÃ THAY ĐỔI

- `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`
  - Method: `calculateScoreAndCreateParticipation()` (dòng 303-388)
    - ✅ Kiểm tra đã có participation COMPLETED chưa
    - ✅ Chỉ tạo participation 1 lần khi PASSED lần đầu
  - Method: `updateStudentScoreFromParticipation()` (dòng 393-480)
    - ✅ Giữ nguyên điểm milestone từ series
    - ✅ Áp dụng cho tất cả scoreType (REN_LUYEN, CTXH, CHUYEN_DE)

---

## 8. KIỂM TRA

### Test Case 1: Quiz CTXH, rewardPoints = 10đ

1. Tạo minigame với `rewardPoints = 10đ`, `scoreType = CONG_TAC_XA_HOI`
2. Student PASSED lần 1
3. Kiểm tra:
   - ActivityParticipation: `pointsEarned = 10đ`, `isCompleted = true`
   - CTXH score: 10đ ✅
4. Student làm lại PASSED lần 2
5. Kiểm tra:
   - Không có participation mới
   - CTXH score: 10đ (không đổi) ✅

### Test Case 2: Quiz REN_LUYEN với milestone

1. Student có milestone: 20đ
2. Tạo minigame với `rewardPoints = 5đ`, `scoreType = REN_LUYEN`
3. Student PASSED
4. Kiểm tra:
   - REN_LUYEN score: 20đ + 5đ = 25đ ✅
5. Student làm lại PASSED
6. Kiểm tra:
   - REN_LUYEN score: 25đ (không đổi) ✅

### Test Case 3: maxAttempts = null

1. Tạo minigame với `maxAttempts = null`
2. Student làm quiz nhiều lần (5, 10, 20 lần...)
3. Kiểm tra:
   - Không bị chặn ✅
   - Có thể làm không giới hạn ✅

### Test Case 4: maxAttempts = 3

1. Tạo minigame với `maxAttempts = 3`
2. Student làm quiz lần 1, 2, 3 → OK ✅
3. Student làm quiz lần 4 → Bị chặn với message "Bạn đã đạt số lần làm quiz tối đa (3 lần)" ✅

---

## 9. GHI CHÚ

### Điểm Quiz:
- ✅ Lấy từ `MiniGame.rewardPoints` (khi tạo minigame)
- ✅ Chỉ được ghi nhận 1 lần khi PASSED lần đầu
- ✅ Không bị ghi đè bởi milestone hoặc activities khác
- ✅ Áp dụng đúng cho tất cả scoreType (REN_LUYEN, CTXH, CHUYEN_DE)

### Re-attempt:
- ✅ Cho phép làm lại sau khi pass
- ✅ Điểm không thay đổi dù làm lại pass hay fail
- ✅ Chỉ ghi nhận điểm 1 lần

### maxAttempts:
- ✅ `null` = Không giới hạn
- ✅ Số nguyên = Giới hạn số lần làm

