# Vấn Đề Tính Điểm Và Giải Pháp

## VẤN ĐỀ 1: CTXH không được cộng điểm

**Hiện trạng:**
- ActivityParticipation đã có pointsEarned
- Nhưng StudentScore (CONG_TAC_XA_HOI) không được cập nhật

**Nguyên nhân:**
- Logic đã gọi `updateStudentScoreFromParticipation()` đúng
- Nhưng có thể do lỗi trong quá trình thực thi hoặc filter không khớp

## VẤN ĐỀ 2: REN_LUYEN bị reset về 0 khi tham gia CHUYEN_DE_DOANH_NGHIEP

**Hiện trạng:**
- Tham gia activity CHUYEN_DE_DOANH_NGHIEP
- Điểm CHUYEN_DE: OK (đếm số buổi đúng)
- Điểm REN_LUYEN: BỊ RESET về 0, chỉ có điểm từ CHUYEN_DE_DOANH_NGHIEP mới

**Nguyên nhân:**
- `updateRenLuyenScoreFromParticipation()` chỉ tính điểm REN_LUYEN từ CHUYEN_DE_DOANH_NGHIEP
- Không tính điểm REN_LUYEN từ các activity khác có scoreType = REN_LUYEN
- Dòng 1007-1019: Chỉ filter `activity.type == CHUYEN_DE_DOANH_NGHIEP`
- `score.setScore(total)` ghi đè toàn bộ điểm REN_LUYEN hiện có

**Code có vấn đề:**
```java
// Chỉ lấy participations của CHUYEN_DE_DOANH_NGHIEP
List<ActivityParticipation> allParticipations = participationRepository
    .findAll()
    .stream()
    .filter(p -> p.getRegistration().getStudent().getId().equals(student.getId())
            && p.getRegistration().getActivity().getType() == ActivityType.CHUYEN_DE_DOANH_NGHIEP  // ← Chỉ lấy CHUYEN_DE_DOANH_NGHIEP
            && p.getRegistration().getActivity().getMaxPoints() != null
            && p.getParticipationType().equals(ParticipationType.COMPLETED))
    .collect(Collectors.toList());

BigDecimal total = allParticipations.stream()
    .map(p -> p.getPointsEarned() != null ? p.getPointsEarned() : BigDecimal.ZERO)
    .reduce(BigDecimal.ZERO, BigDecimal::add);

score.setScore(total);  // ← GHI ĐÈ toàn bộ điểm REN_LUYEN!
```

## GIẢI PHÁP

### Giải pháp cho REN_LUYEN:
Method `updateRenLuyenScoreFromParticipation()` cần tính TẤT CẢ điểm REN_LUYEN từ:
1. Activity có `scoreType = REN_LUYEN`
2. Activity có `type = CHUYEN_DE_DOANH_NGHIEP` (dual score)

### Giải pháp cho CTXH:
Kiểm tra lại logic và đảm bảo `updateStudentScoreFromParticipation()` được gọi đúng.


