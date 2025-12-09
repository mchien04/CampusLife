# Logic Tính Điểm Dual Score Cho Chuyên Đề Doanh Nghiệp

## 1. TÓM TẮT

Khi tạo sự kiện với:
- **Loại sự kiện:** `CHUYEN_DE_DOANH_NGHIEP`
- **Cách tính điểm:** `CHUYEN_DE`
- **Điểm tối đa (maxPoints):** Ví dụ: `5.0`

**Hệ thống tự động hiểu:**
- ✅ **CHUYEN_DE score:** Đếm số buổi tham gia (mỗi lần check-out = +1)
- ✅ **REN_LUYEN score:** Cộng điểm từ `maxPoints` (nếu có nhập maxPoints)

---

## 2. LOGIC HIỆN TẠI

### 2.1. Khi Sinh Viên Check-Out

**File:** `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

**Dòng 439-460:**
```java
} else if (activity.getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP) {
    // CHUYEN_DE_DOANH_NGHIEP: Dual Score Calculation
    // Lưu maxPoints vào pointsEarned để dùng cho REN_LUYEN
    BigDecimal points = activity.getMaxPoints() != null ? activity.getMaxPoints() : BigDecimal.ZERO;
    participation.setPointsEarned(points);
    participationRepository.save(participation);
    
    try {
        // CHUYEN_DE: Đếm số buổi (không dùng pointsEarned, chỉ đếm số participation)
        updateChuyenDeScoreCount(participation);
        
        // REN_LUYEN: Cộng điểm từ maxPoints (nếu có)
        if (activity.getMaxPoints() != null) {
            updateRenLuyenScoreFromParticipation(participation);
        }
        
        logger.info("Auto-completed CHUYEN_DE_DOANH_NGHIEP participation for activity {}. Count: +1, RL Points: {}",
                activity.getName(), activity.getMaxPoints());
    } catch (Exception e) {
        logger.error("Failed to update dual score after auto-completion: {}", e.getMessage(), e);
    }
}
```

### 2.2. Cập Nhật Điểm CHUYEN_DE (Đếm Số Buổi)

**Method:** `updateChuyenDeScoreCount()`

**Logic:**
1. Đếm tất cả `ActivityParticipation` có:
   - `activity.type == CHUYEN_DE_DOANH_NGHIEP`
   - `participationType == COMPLETED`
   - Cùng `studentId`
2. Số buổi = số participation đã COMPLETED
3. Cập nhật `StudentScore.score = count` (ScoreType = CHUYEN_DE)

**Ví dụ:**
- Tham gia 1 buổi → CHUYEN_DE score = 1
- Tham gia 2 buổi → CHUYEN_DE score = 2
- Tham gia 3 buổi → CHUYEN_DE score = 3

### 2.3. Cập Nhật Điểm REN_LUYEN (Từ maxPoints)

**Method:** `updateRenLuyenScoreFromParticipation()`

**Logic:**
1. Tính tổng điểm từ tất cả `ActivityParticipation` có:
   - `activity.type == CHUYEN_DE_DOANH_NGHIEP`
   - `activity.maxPoints != null`
   - `participationType == COMPLETED`
   - Cùng `studentId`
2. Tổng điểm = tổng `pointsEarned` của tất cả participations
3. Cập nhật `StudentScore.score = total` (ScoreType = REN_LUYEN)

**Ví dụ:**
- Activity 1: maxPoints = 5.0 → REN_LUYEN score = 5.0
- Activity 2: maxPoints = 5.0 → REN_LUYEN score = 10.0
- Activity 3: maxPoints = 5.0 → REN_LUYEN score = 15.0

---

## 3. VÍ DỤ CỤ THỂ

### 3.1. Tạo Activity Chuyên Đề Doanh Nghiệp

**Request:**
```json
{
  "name": "Chuyên đề doanh nghiệp - Buổi 1",
  "type": "CHUYEN_DE_DOANH_NGHIEP",
  "scoreType": "CHUYEN_DE",
  "maxPoints": 5.0,
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T17:00:00",
  "requiresSubmission": false
}
```

**Kết quả:**
- Activity được tạo với `type = CHUYEN_DE_DOANH_NGHIEP`
- `scoreType = CHUYEN_DE` (để đếm số buổi)
- `maxPoints = 5.0` (để cộng vào REN_LUYEN)

### 3.2. Sinh Viên Check-Out

**Sau khi check-out:**
- ✅ **CHUYEN_DE score:** +1 (đếm số buổi)
- ✅ **REN_LUYEN score:** +5.0 (từ maxPoints)

**StudentScore sau check-out:**
```json
{
  "scoreType": "CHUYEN_DE",
  "score": 1.0  // Số buổi đã tham gia
},
{
  "scoreType": "REN_LUYEN",
  "score": 5.0  // Điểm từ maxPoints
}
```

### 3.3. Sinh Viên Tham Gia Thêm 2 Buổi Nữa

**Sau 3 buổi:**
- ✅ **CHUYEN_DE score:** 3 (đếm số buổi)
- ✅ **REN_LUYEN score:** 15.0 (3 buổi × 5.0 điểm/buổi)

**StudentScore sau 3 buổi:**
```json
{
  "scoreType": "CHUYEN_DE",
  "score": 3.0  // 3 buổi đã tham gia
},
{
  "scoreType": "REN_LUYEN",
  "score": 15.0  // 3 buổi × 5.0 điểm
}
```

---

## 4. LƯU Ý QUAN TRỌNG

### 4.1. Điều Kiện Cộng Điểm REN_LUYEN

**⚠️ CHỈ cộng vào REN_LUYEN nếu:**
- `activity.maxPoints != null` (có nhập điểm tối đa)

**Nếu không nhập maxPoints:**
- ✅ CHUYEN_DE score vẫn được đếm (số buổi)
- ❌ REN_LUYEN score **KHÔNG** được cộng

### 4.2. Logic Tính Tổng Điểm REN_LUYEN

**Method `updateRenLuyenScoreFromParticipation()` tính lại TỔNG điểm từ TẤT CẢ participations:**
- Không phải cộng dồn từng buổi
- Mà tính lại tổng từ đầu mỗi lần check-out
- Đảm bảo tính chính xác nếu có thay đổi (xóa, sửa participation)

**Code:**
```java
// Tính lại tổng điểm RL từ tất cả ActivityParticipation CHUYEN_DE_DOANH_NGHIEP có maxPoints
List<ActivityParticipation> allParticipations = participationRepository
    .findAll()
    .stream()
    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
            && p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
            && p.getRegistration().getActivity().getMaxPoints() != null
            && p.getParticipationType().equals(ParticipationType.COMPLETED))
    .collect(Collectors.toList());

BigDecimal total = allParticipations.stream()
    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### 4.3. Logic Tính Số Buổi CHUYEN_DE

**Method `updateChuyenDeScoreCount()` tính lại TỔNG số buổi từ TẤT CẢ participations:**
- Đếm tất cả participations COMPLETED
- Không phân biệt có maxPoints hay không
- Mỗi participation COMPLETED = 1 buổi

**Code:**
```java
// Đếm số buổi tham gia CHUYEN_DE_DOANH_NGHIEP đã COMPLETED
List<ActivityParticipation> allParticipations = participationRepository
    .findAll()
    .stream()
    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
            && p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP
            && p.getParticipationType().equals(ParticipationType.COMPLETED))
    .collect(Collectors.toList());

BigDecimal count = BigDecimal.valueOf(allParticipations.size());
```

---

## 5. KẾT LUẬN

### ✅ CÓ TỰ ĐỘNG HIỂU

Khi tạo activity với:
- `type = CHUYEN_DE_DOANH_NGHIEP`
- `scoreType = CHUYEN_DE`
- `maxPoints = 5.0` (ví dụ)

**Hệ thống tự động:**
1. ✅ **CHUYEN_DE score:** Đếm số buổi (mỗi check-out = +1)
2. ✅ **REN_LUYEN score:** Cộng điểm từ maxPoints (nếu có nhập maxPoints)

**Không cần làm gì thêm!** Logic đã được implement sẵn trong code.

### 📝 LƯU Ý KHI TẠO ACTIVITY

**Để đảm bảo dual score hoạt động đúng:**
1. ✅ Chọn `type = CHUYEN_DE_DOANH_NGHIEP`
2. ✅ Chọn `scoreType = CHUYEN_DE` (để đếm số buổi)
3. ✅ **Nhập `maxPoints`** (ví dụ: 5.0) để cộng vào REN_LUYEN
4. ✅ Nếu không nhập maxPoints → chỉ đếm số buổi, không cộng vào REN_LUYEN

---

## 6. FLOW HOÀN CHỈNH

```
1. Tạo Activity
   → type = CHUYEN_DE_DOANH_NGHIEP
   → scoreType = CHUYEN_DE
   → maxPoints = 5.0
   
2. Sinh viên đăng ký và check-out
   ↓
3. Hệ thống tự động:
   a) CHUYEN_DE score: +1 (đếm số buổi)
   b) REN_LUYEN score: +5.0 (từ maxPoints)
   
4. Sinh viên tham gia thêm buổi khác
   ↓
5. Hệ thống tự động:
   a) CHUYEN_DE score: Tính lại tổng số buổi (ví dụ: 2)
   b) REN_LUYEN score: Tính lại tổng điểm (ví dụ: 10.0)
```

---

## 7. TEST CASE

### Test Case 1: Có maxPoints

**Input:**
- Activity: type = CHUYEN_DE_DOANH_NGHIEP, maxPoints = 5.0
- Sinh viên check-out 3 lần

**Expected Output:**
- CHUYEN_DE score: 3.0
- REN_LUYEN score: 15.0

### Test Case 2: Không có maxPoints

**Input:**
- Activity: type = CHUYEN_DE_DOANH_NGHIEP, maxPoints = null
- Sinh viên check-out 3 lần

**Expected Output:**
- CHUYEN_DE score: 3.0
- REN_LUYEN score: 0.0 (không thay đổi)

---

## 8. FILES LIÊN QUAN

- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
  - Method: `checkIn()` (dòng 439-460)
  - Method: `updateChuyenDeScoreCount()` (dòng 1054-1124)
  - Method: `updateRenLuyenScoreFromParticipation()` (dòng 976-1048)

