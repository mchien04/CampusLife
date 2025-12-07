# Hướng Dẫn Cập Nhật Frontend - Quiz và Series

## Tổng Quan

Tài liệu này mô tả các thay đổi API và model cần cập nhật ở Frontend sau khi backend đã được cập nhật.

---

## 1. Thay Đổi Model MiniGame

### 1.1. Thêm Field `maxAttempts`

**Model:** `MiniGameResponse`, `CreateMiniGameRequest`, `UpdateMiniGameRequest`

**Thay đổi:**
- Thêm field `maxAttempts: number | null` vào tất cả các model liên quan đến MiniGame
- `null` = không giới hạn số lần làm quiz
- Số dương = số lần tối đa được phép làm quiz

**Ví dụ:**
```typescript
interface MiniGameResponse {
  id: number;
  title: string;
  description?: string;
  questionCount: number;
  timeLimit?: number;
  requiredCorrectAnswers?: number;
  rewardPoints?: number;
  maxAttempts?: number | null;  // ← MỚI
  isActive: boolean;
  type: string;
  activityId: number;
}
```

---

## 2. Thay Đổi Model Quiz Question

### 2.1. Thêm Field `imageUrl`

**Model:** `QuizQuestionResponse`, `QuizQuestionEditResponse`, `QuizQuestionDetailResponse`, `QuestionRequest`

**Thay đổi:**
- Thêm field `imageUrl: string | null` vào tất cả các model liên quan đến câu hỏi quiz
- `null` = câu hỏi không có ảnh
- URL ảnh từ upload service (giống như `bannerUrl` của Activity)

**Ví dụ:**
```typescript
interface QuizQuestionResponse {
  id: number;
  questionText: string;
  imageUrl?: string | null;  // ← MỚI
  displayOrder: number;
  options: QuizOptionResponse[];
}
```

---

## 3. Thay Đổi API Create/Update MiniGame

### 3.1. API `POST /api/minigames`

**Request Body:**
```json
{
  "activityId": 1,
  "title": "Quiz kiến thức IT",
  "description": "Mô tả quiz",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": 10.0,
  "maxAttempts": 3,  // ← MỚI (optional, null = không giới hạn)
  "questions": [
    {
      "questionText": "Câu hỏi 1?",
      "imageUrl": "https://example.com/image.jpg",  // ← MỚI (optional)
      "options": [
        {"text": "Đáp án A", "isCorrect": true},
        {"text": "Đáp án B", "isCorrect": false}
      ]
    }
  ]
}
```

### 3.2. API `PUT /api/minigames/{miniGameId}`

**Request Body:** Tương tự như `POST`, tất cả fields đều optional (trừ `questions` nếu muốn cập nhật)

---

## 4. Thay Đổi Logic Start Attempt

### 4.1. API `POST /api/minigames/{miniGameId}/start`

**Thay đổi:**
- Backend sẽ kiểm tra `maxAttempts` trước khi cho phép start attempt
- Nếu đã đạt số lần tối đa → trả về lỗi: `"Bạn đã đạt số lần làm quiz tối đa (X lần)"`

**Frontend cần:**
- Hiển thị thông báo lỗi khi user đã đạt số lần tối đa
- Disable nút "Bắt đầu làm quiz" nếu đã đạt maxAttempts
- Hiển thị số lần đã làm / số lần tối đa (nếu có)

**Ví dụ UI:**
```
[Đã làm: 2/3 lần] [Bắt đầu làm quiz]
```

---

## 5. Thay Đổi Logic Submit Attempt

### 5.1. Re-attempt (Làm lại sau khi PASSED)

**Thay đổi:**
- Backend cho phép làm lại quiz ngay cả sau khi đã PASSED
- Khi PASSED lại, điểm cũ sẽ bị ghi đè bằng điểm mới
- Đếm tổng số lần (PASSED + FAILED) để kiểm tra maxAttempts

**Frontend cần:**
- Cho phép hiển thị nút "Làm lại" sau khi đã PASSED
- Hiển thị cảnh báo: "Làm lại sẽ ghi đè điểm cũ"
- Cập nhật lại điểm sau khi submit thành công

---

## 6. Thay Đổi ActivityRegistration Status

### 6.1. Status Tự Động = ATTENDED

**Thay đổi:**
- Sau khi quiz PASSED, `ActivityRegistration.status` sẽ tự động được set = `ATTENDED`
- Quiz không có check-in/check-out, nên status sẽ tự động chuyển sang ATTENDED khi hoàn thành

**Frontend cần:**
- Kiểm tra `registration.status === "ATTENDED"` để hiển thị "Đã tham gia"
- Không cần hiển thị nút check-in/check-out cho quiz
- Có thể hiển thị badge "Đã hoàn thành" khi status = ATTENDED

---

## 7. Hiển Thị Ảnh Cho Câu Hỏi

### 7.1. UI Component

**Frontend cần:**
- Hiển thị ảnh (nếu có) trong component câu hỏi quiz
- Upload ảnh khi tạo/chỉnh sửa quiz (dùng upload service có sẵn)
- Validate URL ảnh (nếu cần)

**Ví dụ UI:**
```
[Câu hỏi 1: HTML là viết tắt của gì?]
[Ảnh: [Image.jpg] (nếu có)]
[ ] HyperText Markup Language
[ ] High Tech Modern Language
[ ] Home Tool Markup Language
[ ] Hyperlink and Text Markup Language
```

---

## 8. Checklist Cập Nhật Frontend

### 8.1. Models/Interfaces
- [ ] Thêm `maxAttempts?: number | null` vào `MiniGameResponse`
- [ ] Thêm `maxAttempts?: number | null` vào `CreateMiniGameRequest`
- [ ] Thêm `maxAttempts?: number | null` vào `UpdateMiniGameRequest`
- [ ] Thêm `imageUrl?: string | null` vào `QuizQuestionResponse`
- [ ] Thêm `imageUrl?: string | null` vào `QuizQuestionEditResponse`
- [ ] Thêm `imageUrl?: string | null` vào `QuizQuestionDetailResponse`
- [ ] Thêm `imageUrl?: string | null` vào `QuestionRequest`

### 8.2. API Calls
- [ ] Cập nhật `POST /api/minigames` để gửi `maxAttempts` và `imageUrl`
- [ ] Cập nhật `PUT /api/minigames/{miniGameId}` để gửi `maxAttempts` và `imageUrl`
- [ ] Xử lý error khi start attempt đã đạt maxAttempts

### 8.3. UI Components
- [ ] Form tạo/chỉnh sửa quiz: Thêm input `maxAttempts` (number, optional)
- [ ] Form tạo/chỉnh sửa quiz: Thêm upload ảnh cho mỗi câu hỏi
- [ ] Component hiển thị câu hỏi: Hiển thị ảnh (nếu có)
- [ ] Component start quiz: Hiển thị số lần đã làm / maxAttempts
- [ ] Component start quiz: Disable nút nếu đã đạt maxAttempts
- [ ] Component kết quả quiz: Hiển thị nút "Làm lại" (nếu còn lượt)
- [ ] Component registration: Kiểm tra status = ATTENDED để hiển thị "Đã tham gia"

### 8.4. Error Handling
- [ ] Xử lý error: "Bạn đã đạt số lần làm quiz tối đa (X lần)"
- [ ] Hiển thị thông báo khi làm lại quiz (ghi đè điểm cũ)

---
