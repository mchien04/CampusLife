# Sửa Logic Tính Điểm Milestone Points

## 🔍 Vấn Đề

### Logic Milestone Hiện Tại (SAI)

**Yêu cầu:**
- Mốc 1: 5đ
- Mốc 2: 10đ
- Khi đạt mốc 1 → tổng = 5đ
- Khi đạt mốc 2 → tổng = 10đ (KHÔNG phải 5+10=15đ)
- **Tính theo mốc cuối đạt, KHÔNG cộng dồn**

**Logic cũ (SAI):**
```java
// Cộng milestone mới vào tổng điểm
BigDecimal updatedScore = totalFromParticipations.add(newPoints);
```

**Vấn đề:**
- Nếu đã có milestone cũ (5đ), khi đạt mốc 2 (10đ), sẽ cộng 10đ vào → tổng = 15đ (SAI)
- Cần trừ milestone cũ trước khi cộng milestone mới

---

## ✅ Giải Pháp

### Logic Mới (ĐÚNG)

```java
// 1. Lấy điểm milestone cũ
BigDecimal oldMilestonePoints = progress.getPointsEarned(); // Ví dụ: 5đ

// 2. Tính lại tổng điểm
// Công thức: newTotal = (oldTotal - oldMilestone) + newMilestone
BigDecimal updatedScore = oldTotalScore.subtract(oldMilestonePoints).add(newPoints);
```

**Ví dụ:**
- Ban đầu: `oldTotalScore = 20đ` (có 15đ từ participations + 5đ milestone cũ)
- Đạt mốc 2: `newPoints = 10đ`, `oldMilestonePoints = 5đ`
- Tính: `updatedScore = 20 - 5 + 10 = 25đ`
- Kết quả: 15đ participations + 10đ milestone mới = 25đ ✅

---

## 📋 Các Thay Đổi Khác

### 1. Bỏ Validation Strict Cho Type

**Lý do:** Cho phép chỉnh sửa type sau khi tạo activity

**Thay đổi:**
- ❌ Bỏ validation: "Activity trong series chỉ cho phép type = null hoặc MINIGAME"
- ✅ Cho phép tất cả các type (SUKIEN, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP, etc.)
- ✅ Validation chỉ ở `MiniGameServiceImpl` khi tạo minigame

### 2. ScoreType Không Cần Truyền

**Logic hiện tại (ĐÚNG):**
- `activity.setScoreType(null)` → lấy từ series
- Series có `scoreType` → dùng khi tính milestone
- **Không cần truyền scoreType vào activity** ✅

---

## 🎯 Kết Luận

### Logic Milestone (ĐÃ SỬA):
- ✅ Tính theo mốc cuối đạt, không cộng dồn
- ✅ Trừ milestone cũ trước khi cộng milestone mới
- ✅ Công thức: `newTotal = (oldTotal - oldMilestone) + newMilestone`

### Type Validation (ĐÃ SỬA):
- ✅ Cho phép tất cả các type khi tạo activity trong series
- ✅ Validation chỉ ở `MiniGameServiceImpl` khi tạo minigame

### ScoreType (KHÔNG CẦN THAY ĐỔI):
- ✅ Logic đúng: `activity.setScoreType(null)` → lấy từ series
- ✅ Không cần truyền scoreType vào activity

