# Hướng Dẫn Frontend: Tạo Minigame Trong Series

## 📋 Tổng Quan

Khi tạo minigame (quiz) trong series, có 2 bước:
1. **Tạo Activity trong Series** với `type = "MINIGAME"`
2. **Tạo Minigame** cho activity đó (có thể không cần `rewardPoints`)

---

## 🔄 Flow Tạo Minigame Trong Series

### Bước 1: Tạo Activity Trong Series

**API:** `POST /api/series/{seriesId}/activities`

**Request Body:**
```json
{
  "name": "Quiz kiến thức IT - Bài 1",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "type": "MINIGAME",  // ⚠️ QUAN TRỌNG: Phải truyền type = "MINIGAME"
  "startDate": "2025-02-01T08:00:00",
  "endDate": "2025-02-01T23:59:59",
  "location": "Online",
  "order": 1,
  "shareLink": "https://example.com/quiz1",
  "bannerUrl": "https://example.com/banner.jpg",
  "benefits": "Nâng cao kiến thức IT",
  "requirements": "Đã học môn IT cơ bản",
  "contactInfo": "contact@example.com",
  "organizerIds": [1, 2]
}
```

**Response:**
```json
{
  "status": true,
  "message": "Activity created in series successfully",
  "data": {
    "id": 123,
    "name": "Quiz kiến thức IT - Bài 1",
    "type": "MINIGAME",
    "seriesId": 1,
    "seriesOrder": 1,
    "scoreType": null,  // Lấy từ series
    "maxPoints": null,
    ...
  }
}
```

**Lưu ý:**
- ✅ `type = "MINIGAME"` là **BẮT BUỘC** nếu muốn tạo quiz
- ✅ `scoreType` sẽ là `null` (lấy từ series)
- ✅ `maxPoints` sẽ là `null` (không dùng để tính điểm)
- ✅ Lưu lại `activityId` từ response để dùng ở bước 2

---

### Bước 2: Tạo Minigame Cho Activity

**API:** `POST /api/minigames`

**Request Body:**
```json
{
  "activityId": 123,  // ID từ bước 1
  "title": "Quiz kiến thức IT - Bài 1",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": null,  // ⚠️ QUAN TRỌNG: Có thể null nếu thuộc series
  "maxAttempts": null,  // null = không giới hạn
  "questions": [
    {
      "questionText": "HTML là viết tắt của gì?",
      "imageUrl": null,
      "options": [
        {"text": "HyperText Markup Language", "isCorrect": true},
        {"text": "High Tech Modern Language", "isCorrect": false},
        {"text": "Home Tool Markup Language", "isCorrect": false},
        {"text": "Hyperlink and Text Markup Language", "isCorrect": false}
      ]
    },
    {
      "questionText": "CSS dùng để làm gì?",
      "imageUrl": null,
      "options": [
        {"text": "Tạo cấu trúc trang web", "isCorrect": false},
        {"text": "Tạo style cho trang web", "isCorrect": true},
        {"text": "Xử lý logic", "isCorrect": false},
        {"text": "Lưu trữ dữ liệu", "isCorrect": false}
      ]
    }
    // ... thêm các câu hỏi khác
  ]
}
```

**Response:**
```json
{
  "status": true,
  "message": "MiniGame created successfully",
  "data": {
    "id": 45,
    "title": "Quiz kiến thức IT - Bài 1",
    "activityId": 123,
    "rewardPoints": null,
    "requiredCorrectAnswers": 3,
    "maxAttempts": null,
    ...
  }
}
```

**Lưu ý:**
- ✅ `rewardPoints` **có thể null** nếu activity thuộc series
- ✅ Điểm sẽ được tính từ **milestone points** của series
- ✅ Nếu `rewardPoints = null`, hệ thống sẽ chỉ tính milestone khi student pass quiz

---

## 📊 So Sánh: Quiz Trong Series vs Quiz Đơn Lẻ

### Bảng So Sánh Activity (Trước Khi Tạo Minigame)

| Field | Quiz Trong Series | Quiz Đơn Lẻ |
|-------|-------------------|-------------|
| `type` | `"MINIGAME"` (bắt buộc) | `"MINIGAME"` (bắt buộc) |
| `seriesId` | Có giá trị (từ series) | `null` |
| `scoreType` | `null` (lấy từ series) | `"REN_LUYEN"` hoặc khác (từ activity) |
| `maxPoints` | `null` | `null` |

### Bảng So Sánh Request Body Khi Tạo Minigame

| Field | Quiz Trong Series | Quiz Đơn Lẻ | Required | Ghi Chú |
|-------|-------------------|-------------|----------|---------|
| `activityId` | ✅ Có | ✅ Có | **Required** | ID của activity (phải có `type = "MINIGAME"`) |
| `title` | ✅ Có | ✅ Có | **Required** | Tiêu đề quiz |
| `description` | ✅ Có thể có | ✅ Có thể có | Optional | Mô tả quiz |
| `questionCount` | ✅ Có | ✅ Có | **Required** | Số câu hỏi |
| `timeLimit` | ✅ Có thể có | ✅ Có thể có | Optional | Thời gian làm bài (giây) |
| `requiredCorrectAnswers` | ✅ Có thể có | ✅ Có thể có | Optional | Số câu đúng tối thiểu để pass |
| **`rewardPoints`** | **`null` hoặc không truyền** | **`> 0` (nên có)** | **Conditional** | ⚠️ **KHÁC BIỆT CHÍNH** |
| `maxAttempts` | ✅ Có thể có | ✅ Có thể có | Optional | `null` = không giới hạn |
| `questions` | ✅ Có | ✅ Có | **Required** | Danh sách câu hỏi |

### Chi Tiết Sự Khác Biệt

#### 1. Field `rewardPoints` ⚠️ QUAN TRỌNG

**Quiz Trong Series:**
```json
{
  "activityId": 123,
  "rewardPoints": null,  // ✅ OK - có thể null
  // hoặc không truyền field này
  ...
}
```
- ✅ **Có thể `null`** hoặc không truyền field
- ✅ Backend sẽ **không warning**
- ✅ Điểm sẽ tính từ **milestone points** của series

**Quiz Đơn Lẻ:**
```json
{
  "activityId": 123,
  "rewardPoints": 10,  // ✅ Nên có giá trị > 0
  ...
}
```
- ⚠️ **Nên có giá trị > 0**
- ⚠️ Backend sẽ **warning** (không fail) nếu `null` hoặc `<= 0`
- ✅ Điểm sẽ tính từ **rewardPoints**

#### 2. Field `activityId` - Yêu Cầu Activity

**Quiz Trong Series:**
- Activity phải có `type = "MINIGAME"` **VÀ** `seriesId != null`
- Activity được tạo từ `POST /api/series/{seriesId}/activities` với `type: "MINIGAME"`

**Quiz Đơn Lẻ:**
- Activity phải có `type = "MINIGAME"` **VÀ** `seriesId == null`
- Activity được tạo từ `POST /api/activities` với `type: "MINIGAME"`

#### 3. Các Field Khác (Giống Nhau)

| Field | Giá Trị | Ghi Chú |
|-------|---------|---------|
| `title` | String | Bắt buộc, tiêu đề quiz |
| `description` | String \| null | Optional |
| `questionCount` | Integer | Bắt buộc, số câu hỏi |
| `timeLimit` | Integer \| null | Optional, thời gian (giây) |
| `requiredCorrectAnswers` | Integer \| null | Optional, số câu đúng tối thiểu |
| `maxAttempts` | Integer \| null | Optional, `null` = không giới hạn |
| `questions` | Array | Bắt buộc, danh sách câu hỏi |

### Ví Dụ Request Body

#### Quiz Trong Series
```json
{
  "activityId": 123,  // Activity có seriesId != null
  "title": "Quiz IT - Bài 1",
  "description": "Bài quiz về IT",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": null,  // ⚠️ null hoặc không truyền
  "maxAttempts": null,
  "questions": [...]
}
```

#### Quiz Đơn Lẻ
```json
{
  "activityId": 456,  // Activity có seriesId == null
  "title": "Quiz IT Standalone",
  "description": "Bài quiz về IT",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": 10,  // ⚠️ Nên có giá trị > 0
  "maxAttempts": 3,
  "questions": [...]
}
```

---

## 🎯 Logic Tính Điểm

### Quiz Trong Series

**Khi student pass quiz:**
1. Tạo `ActivityParticipation` với `pointsEarned = 0`
2. Tăng `completedCount` trong series progress
3. Tính lại milestone points (nếu đạt mốc)
4. Cộng milestone points vào `StudentScore` (scoreType từ series)

**Ví dụ:**
- Series có milestone: `{"2": 5, "3": 10}`
- Student pass quiz 1 → `completedCount = 1`, milestone = 0
- Student pass quiz 2 → `completedCount = 2`, milestone = 5đ
- Student pass quiz 3 → `completedCount = 3`, milestone = 10đ

### Quiz Đơn Lẻ

**Khi student pass quiz:**
1. Tạo `ActivityParticipation` với `pointsEarned = rewardPoints`
2. Cộng `rewardPoints` vào `StudentScore` (scoreType từ activity)

**Ví dụ:**
- Quiz có `rewardPoints = 10`
- Student pass quiz → `pointsEarned = 10`, StudentScore + 10đ

---

## 🔧 UI Components Gợi Ý

### Component: CreateActivityInSeriesForm

```typescript
interface CreateActivityInSeriesFormData {
  name: string;
  description?: string;
  type?: "MINIGAME" | null;  // null = activity thường, "MINIGAME" = quiz
  startDate?: string;
  endDate?: string;
  location?: string;
  order?: number;
  shareLink?: string;
  bannerUrl?: string;
  benefits?: string;
  requirements?: string;
  contactInfo?: string;
  organizerIds?: number[];
}

// Khi user chọn "Tạo Quiz"
const handleCreateQuiz = async (formData: CreateActivityInSeriesFormData) => {
  const response = await fetch(`/api/series/${seriesId}/activities`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      ...formData,
      type: "MINIGAME"  // ⚠️ Bắt buộc
    })
  });
  
  const result = await response.json();
  if (result.status) {
    const activityId = result.data.id;
    // Chuyển sang form tạo minigame
    navigate(`/series/${seriesId}/activities/${activityId}/create-quiz`);
  }
};
```

### Component: CreateMinigameForm

```typescript
interface CreateMinigameFormData {
  activityId: number;
  title: string;
  description?: string;
  questionCount: number;
  timeLimit?: number;
  requiredCorrectAnswers?: number;
  rewardPoints?: number | null;  // null nếu thuộc series
  maxAttempts?: number | null;  // null = không giới hạn
  questions: QuestionData[];
}

interface QuestionData {
  questionText: string;
  imageUrl?: string;
  options: OptionData[];
}

interface OptionData {
  text: string;
  isCorrect: boolean;
}

// Kiểm tra activity có thuộc series không
const isInSeries = activity?.seriesId != null;

// Nếu thuộc series, ẩn hoặc disable field rewardPoints
<FormField
  name="rewardPoints"
  label="Điểm thưởng"
  type="number"
  disabled={isInSeries}
  helperText={isInSeries 
    ? "Quiz trong series sẽ tính điểm từ milestone của series" 
    : "Nhập điểm thưởng khi pass quiz (nên > 0)"}
/>
```

---

## 📝 Ví Dụ Hoàn Chỉnh

### Tạo Series Với 3 Quiz

```typescript
// 1. Tạo series
const seriesResponse = await createSeries({
  name: "Chuỗi Quiz IT",
  description: "3 bài quiz về IT",
  milestonePoints: JSON.stringify({"2": 5, "3": 10}),
  scoreType: "REN_LUYEN",
  requiresApproval: false
});
const seriesId = seriesResponse.data.id;

// 2. Tạo activity 1 (quiz)
const activity1Response = await createActivityInSeries(seriesId, {
  name: "Quiz IT - Bài 1",
  type: "MINIGAME",
  order: 1
});
const activity1Id = activity1Response.data.id;

// 3. Tạo minigame cho activity 1
await createMinigame({
  activityId: activity1Id,
  title: "Quiz IT - Bài 1",
  questionCount: 5,
  requiredCorrectAnswers: 3,
  rewardPoints: null,  // ⚠️ null vì thuộc series
  questions: [...]
});

// 4. Tạo activity 2 (quiz)
const activity2Response = await createActivityInSeries(seriesId, {
  name: "Quiz IT - Bài 2",
  type: "MINIGAME",
  order: 2
});
const activity2Id = activity2Response.data.id;

// 5. Tạo minigame cho activity 2
await createMinigame({
  activityId: activity2Id,
  title: "Quiz IT - Bài 2",
  questionCount: 5,
  requiredCorrectAnswers: 3,
  rewardPoints: null,  // ⚠️ null vì thuộc series
  questions: [...]
});

// 6. Tạo activity 3 (quiz)
const activity3Response = await createActivityInSeries(seriesId, {
  name: "Quiz IT - Bài 3",
  type: "MINIGAME",
  order: 3
});
const activity3Id = activity3Response.data.id;

// 7. Tạo minigame cho activity 3
await createMinigame({
  activityId: activity3Id,
  title: "Quiz IT - Bài 3",
  questionCount: 5,
  requiredCorrectAnswers: 3,
  rewardPoints: null,  // ⚠️ null vì thuộc series
  questions: [...]
});
```

### Tạo Series Kết Hợp (Activity Thường + Quiz)

```typescript
// 1. Tạo series
const seriesId = await createSeries({...});

// 2. Tạo activity thường 1
const activity1Response = await createActivityInSeries(seriesId, {
  name: "Workshop IT",
  type: null,  // Activity thường
  order: 1
});

// 3. Tạo activity thường 2
const activity2Response = await createActivityInSeries(seriesId, {
  name: "Seminar IT",
  type: null,  // Activity thường
  order: 2
});

// 4. Tạo quiz
const quizActivityResponse = await createActivityInSeries(seriesId, {
  name: "Quiz IT",
  type: "MINIGAME",  // ⚠️ Quiz
  order: 3
});
const quizActivityId = quizActivityResponse.data.id;

// 5. Tạo minigame cho quiz
await createMinigame({
  activityId: quizActivityId,
  title: "Quiz IT",
  rewardPoints: null,  // ⚠️ null vì thuộc series
  questions: [...]
});
```

---

## ⚠️ Lưu Ý Quan Trọng

### 1. Type = "MINIGAME" là Bắt Buộc

**❌ SAI:**
```json
{
  "name": "Quiz 1",
  "type": null  // ❌ Không thể tạo quiz nếu type = null
}
```

**✅ ĐÚNG:**
```json
{
  "name": "Quiz 1",
  "type": "MINIGAME"  // ✅ Bắt buộc
}
```

### 2. RewardPoints Có Thể Null

**Quiz trong series:**
```json
{
  "activityId": 123,
  "rewardPoints": null  // ✅ OK - sẽ tính từ milestone
}
```

**Quiz đơn lẻ:**
```json
{
  "activityId": 123,
  "rewardPoints": 10  // ✅ Nên có giá trị > 0
}
```

### 3. Validation

**Backend sẽ:**
- ✅ Cho phép `rewardPoints = null` nếu activity thuộc series
- ⚠️ Warning (không fail) nếu quiz đơn lẻ không có `rewardPoints`
- ✅ Tự động set `pointsEarned = 0` cho quiz trong series
- ✅ Tự động update series progress khi student pass quiz

### 4. Điểm Tính Từ Đâu?

**Quiz trong series:**
- Điểm = **Milestone points** (từ series configuration)
- Không tính từ `rewardPoints` (dù có set hay không)
- `pointsEarned` trong `ActivityParticipation` = `0`

**Quiz đơn lẻ:**
- Điểm = **rewardPoints** (từ minigame configuration)
- Không có milestone
- `pointsEarned` trong `ActivityParticipation` = `rewardPoints`

### 5. Tóm Tắt Sự Khác Biệt Chính

| Tiêu Chí | Quiz Trong Series | Quiz Đơn Lẻ |
|----------|-------------------|-------------|
| **Activity trước khi tạo** | Phải có `type = "MINIGAME"` và `seriesId != null` | Phải có `type = "MINIGAME"` và `seriesId == null` |
| **Field `rewardPoints`** | `null` hoặc không truyền | Nên có giá trị `> 0` |
| **Validation `rewardPoints`** | ✅ Không warning | ⚠️ Warning nếu `null` hoặc `<= 0` |
| **Điểm tính từ** | Milestone points (series) | `rewardPoints` (minigame) |
| **`pointsEarned` trong participation** | `0` | `rewardPoints` |
| **Các field khác** | Giống nhau | Giống nhau |

---

## 🔍 API Endpoints

### 1. Tạo Activity Trong Series

```
POST /api/series/{seriesId}/activities
```

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Quiz 1",
  "type": "MINIGAME",
  ...
}
```

### 2. Tạo Minigame

```
POST /api/minigames
```

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Request:**
```json
{
  "activityId": 123,
  "rewardPoints": null,
  ...
}
```

### 3. Lấy Danh Sách Activities Trong Series

```
GET /api/series/{seriesId}/activities
```

**Response:**
```json
{
  "status": true,
  "data": [
    {
      "id": 123,
      "name": "Quiz 1",
      "type": "MINIGAME",
      "seriesId": 1,
      "seriesOrder": 1
    },
    {
      "id": 124,
      "name": "Activity 2",
      "type": null,
      "seriesId": 1,
      "seriesOrder": 2
    }
  ]
}
```

### 4. Kiểm Tra Activity Có Quiz Chưa

```
GET /api/minigames/activity/{activityId}/check
```

**Response:**
```json
{
  "status": true,
  "data": {
    "hasQuiz": true,
    "miniGameId": 45,
    "miniGameTitle": "Quiz IT - Bài 1",
    "isActive": true,
    "quizId": 12,
    "questionCount": 5
  }
}
```

---


## ✅ Checklist Khi Tạo Minigame

### Checklist: Quiz Trong Series

- [ ] Tạo activity trong series với `type = "MINIGAME"`
- [ ] Xác nhận activity có `seriesId != null`
- [ ] Lưu lại `activityId` từ response
- [ ] Tạo minigame với `activityId` đã lưu
- [ ] Set `rewardPoints = null` (hoặc không truyền field này)
- [ ] Kiểm tra activity có quiz chưa bằng API `/api/minigames/activity/{activityId}/check`
- [ ] Hiển thị thông báo cho user: "Điểm sẽ tính từ milestone của series"

### Checklist: Quiz Đơn Lẻ

- [ ] Tạo activity riêng với `type = "MINIGAME"`
- [ ] Xác nhận activity có `seriesId == null`
- [ ] Lưu lại `activityId` từ response
- [ ] Tạo minigame với `activityId` đã lưu
- [ ] Set `rewardPoints > 0` (nên có giá trị)
- [ ] Kiểm tra activity có quiz chưa bằng API `/api/minigames/activity/{activityId}/check`
- [ ] Hiển thị thông báo cho user: "Điểm sẽ tính từ rewardPoints"

---

## 🐛 Troubleshooting

### Lỗi: "Activity type must be MINIGAME"

**Nguyên nhân:** Activity không có `type = "MINIGAME"`

**Giải pháp:**
- Kiểm tra request body có `type: "MINIGAME"` không
- Nếu activity đã tạo với `type = null`, cần tạo lại hoặc update activity

### Lỗi: "No points to award"

**Nguyên nhân:** Quiz đơn lẻ không có `rewardPoints > 0`

**Giải pháp:**
- Nếu quiz đơn lẻ: thêm `rewardPoints > 0`
- Nếu quiz trong series: đảm bảo `activity.getSeriesId() != null`

### Quiz không cập nhật series progress

**Nguyên nhân:** Activity không có `seriesId` hoặc `type != "MINIGAME"`

**Giải pháp:**
- Kiểm tra activity có `seriesId` không
- Kiểm tra activity có `type = "MINIGAME"` không

### Lỗi: "Standalone minigame should have rewardPoints > 0"

**Nguyên nhân:** Quiz đơn lẻ không có `rewardPoints > 0`

**Giải pháp:**
- Thêm `rewardPoints > 0` vào request body
- Hoặc đảm bảo activity thuộc series (`seriesId != null`) nếu muốn `rewardPoints = null`

### Nhầm lẫn giữa Quiz Trong Series và Quiz Đơn Lẻ

**Nguyên nhân:** Không phân biệt được khi nào dùng `rewardPoints = null`

**Giải pháp:**
- **Quiz trong series:** `activity.seriesId != null` → `rewardPoints = null`
- **Quiz đơn lẻ:** `activity.seriesId == null` → `rewardPoints > 0`

### Lỗi: "Standalone minigame should have rewardPoints > 0"

**Nguyên nhân:** Quiz đơn lẻ không có `rewardPoints > 0`

**Giải pháp:**
- Thêm `rewardPoints > 0` vào request body
- Hoặc đảm bảo activity thuộc series (`seriesId != null`) nếu muốn `rewardPoints = null`

### Nhầm lẫn giữa Quiz Trong Series và Quiz Đơn Lẻ

**Nguyên nhân:** Không phân biệt được khi nào dùng `rewardPoints = null`

**Giải pháp:**
- **Quiz trong series:** `activity.seriesId != null` → `rewardPoints = null`
- **Quiz đơn lẻ:** `activity.seriesId == null` → `rewardPoints > 0`

---

