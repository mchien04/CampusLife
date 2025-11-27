# Flow Tạo Minigame và Các Entity

## 📋 TỔNG QUAN

Khi tạo minigame, hệ thống sẽ tạo ra **5 loại entity** theo thứ tự:
1. **Activity** (đã có sẵn, type = MINIGAME)
2. **MiniGame**
3. **MiniGameQuiz**
4. **MiniGameQuizQuestion** (nhiều câu hỏi)
5. **MiniGameQuizOption** (nhiều lựa chọn cho mỗi câu hỏi)

---

## 🔄 FLOW TẠO MINIGAME

### Bước 1: Tạo Activity (MINIGAME)

**API:** `POST /api/activities`

**Entity:** `Activity`

**Fields quan trọng:**
- `type = ActivityType.MINIGAME` (BẮT BUỘC)
- `scoreType` (ví dụ: `REN_LUYEN`)
- `penaltyPointsIncomplete` (tùy chọn - điểm trừ khi không đạt)
- `maxPoints` (KHÔNG dùng để tính điểm, có thể null)

**Lưu ý:**
- Activity phải được tạo TRƯỚC khi tạo minigame
- Lưu lại `activityId` từ response

**Ví dụ:**
```json
{
  "name": "Quiz kiến thức IT",
  "type": "MINIGAME",
  "scoreType": "REN_LUYEN",
  "penaltyPointsIncomplete": 2.0,
  "maxPoints": null  // Không dùng
}
```

---

### Bước 2: Tạo MiniGame với Quiz

**API:** `POST /api/minigames`

**Input:**
```json
{
  "activityId": 1,
  "title": "Quiz kiến thức IT",
  "description": "Bài quiz về kiến thức IT cơ bản",
  "questionCount": 5,
  "timeLimit": 300,
  "requiredCorrectAnswers": 3,
  "rewardPoints": 10.0,
  "questions": [
    {
      "questionText": "HTML là viết tắt của gì?",
      "options": [
        {"text": "HyperText Markup Language", "isCorrect": true},
        {"text": "High Tech Modern Language", "isCorrect": false},
        {"text": "Home Tool Markup Language", "isCorrect": false},
        {"text": "Hyperlink and Text Markup Language", "isCorrect": false}
      ]
    },
    ...
  ]
}
```

---

## 📦 CÁC ENTITY ĐƯỢC TẠO

### 1. MiniGame Entity

**Table:** `mini_games`

**Được tạo:** Bước đầu tiên trong flow tạo minigame

**Fields:**
- `id` (Long) - Auto-generated
- `title` (String) - Tiêu đề minigame
- `description` (String) - Mô tả
- `questionCount` (Integer) - Số lượng câu hỏi
- `timeLimit` (Integer) - Thời gian giới hạn (giây), có thể null
- `isActive` (boolean) - Mặc định = true
- `type` (MiniGameType) - Mặc định = QUIZ
- `activity` (Activity) - OneToOne với Activity (đã tạo ở bước 1)
- `requiredCorrectAnswers` (Integer) - Số câu đúng tối thiểu để đạt, có thể null
- `rewardPoints` (BigDecimal) - Điểm thưởng khi đạt, có thể null

**Mối quan hệ:**
- `@OneToOne` với `Activity` (unique constraint)
- `@OneToOne` với `MiniGameQuiz` (sẽ tạo ở bước tiếp theo)

**Code:**
```java
MiniGame miniGame = new MiniGame();
miniGame.setTitle(title);
miniGame.setDescription(description);
miniGame.setQuestionCount(questionCount);
miniGame.setTimeLimit(timeLimit);
miniGame.setActive(true);
miniGame.setType(MiniGameType.QUIZ);
miniGame.setActivity(activity);  // Activity đã có sẵn
miniGame.setRequiredCorrectAnswers(requiredCorrectAnswers);
miniGame.setRewardPoints(rewardPoints);
MiniGame savedMiniGame = miniGameRepository.save(miniGame);
```

---

### 2. MiniGameQuiz Entity

**Table:** `mini_game_quizzes`

**Được tạo:** Ngay sau khi tạo MiniGame

**Fields:**
- `id` (Long) - Auto-generated
- `miniGame` (MiniGame) - OneToOne với MiniGame

**Mối quan hệ:**
- `@OneToOne` với `MiniGame` (unique constraint)
- `@OneToMany` với `MiniGameQuizQuestion` (sẽ tạo ở bước tiếp theo)

**Code:**
```java
MiniGameQuiz quiz = new MiniGameQuiz();
quiz.setMiniGame(savedMiniGame);
MiniGameQuiz savedQuiz = quizRepository.save(quiz);
```

**Lưu ý:**
- Entity này chỉ là wrapper/container cho các câu hỏi
- Mỗi MiniGame chỉ có 1 MiniGameQuiz

---

### 3. MiniGameQuizQuestion Entity (Nhiều câu hỏi)

**Table:** `mini_game_quiz_questions`

**Được tạo:** Vòng lặp qua từng câu hỏi trong request

**Fields:**
- `id` (Long) - Auto-generated
- `questionText` (String) - Nội dung câu hỏi
- `miniGameQuiz` (MiniGameQuiz) - ManyToOne với MiniGameQuiz
- `displayOrder` (Integer) - Thứ tự hiển thị (0, 1, 2...)

**Mối quan hệ:**
- `@ManyToOne` với `MiniGameQuiz`
- `@OneToMany` với `MiniGameQuizOption` (sẽ tạo ở bước tiếp theo)

**Code:**
```java
int order = 0;
for (Map<String, Object> questionData : questions) {
    MiniGameQuizQuestion question = new MiniGameQuizQuestion();
    question.setQuestionText((String) questionData.get("questionText"));
    question.setMiniGameQuiz(savedQuiz);
    question.setDisplayOrder(order++);
    MiniGameQuizQuestion savedQuestion = questionRepository.save(question);
    
    // Tạo options cho câu hỏi này (bước tiếp theo)
    ...
}
```

**Lưu ý:**
- Mỗi MiniGameQuiz có nhiều MiniGameQuizQuestion
- `displayOrder` được tăng dần (0, 1, 2...) để sắp xếp

---

### 4. MiniGameQuizOption Entity (Nhiều lựa chọn cho mỗi câu hỏi)

**Table:** `mini_game_quiz_options`

**Được tạo:** Vòng lặp qua từng option trong mỗi câu hỏi

**Fields:**
- `id` (Long) - Auto-generated
- `text` (String) - Nội dung lựa chọn
- `isCorrect` (boolean) - Là đáp án đúng hay sai (mặc định = false)
- `question` (MiniGameQuizQuestion) - ManyToOne với MiniGameQuizQuestion

**Mối quan hệ:**
- `@ManyToOne` với `MiniGameQuizQuestion`

**Code:**
```java
@SuppressWarnings("unchecked")
List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");
if (options != null) {
    for (Map<String, Object> optionData : options) {
        MiniGameQuizOption option = new MiniGameQuizOption();
        option.setText((String) optionData.get("text"));
        option.setCorrect((Boolean) optionData.getOrDefault("isCorrect", false));
        option.setQuestion(savedQuestion);
        optionRepository.save(option);
    }
}
```

**Lưu ý:**
- Mỗi MiniGameQuizQuestion có nhiều MiniGameQuizOption (thường 4 options)
- Chỉ có 1 option có `isCorrect = true` (đáp án đúng)
- Các option khác có `isCorrect = false`

---

## 📊 SƠ ĐỒ MỐI QUAN HỆ

```
Activity (type = MINIGAME)
    │
    │ @OneToOne (unique)
    ↓
MiniGame
    │
    │ @OneToOne (unique)
    ↓
MiniGameQuiz
    │
    │ @OneToMany
    ↓
MiniGameQuizQuestion (nhiều câu hỏi)
    │
    │ @OneToMany
    ↓
MiniGameQuizOption (nhiều lựa chọn cho mỗi câu hỏi)
```

---

## 🔢 VÍ DỤ CỤ THỂ

### Input Request:
```json
{
  "activityId": 1,
  "title": "Quiz IT",
  "questionCount": 2,
  "questions": [
    {
      "questionText": "Câu hỏi 1?",
      "options": [
        {"text": "Đáp án A", "isCorrect": true},
        {"text": "Đáp án B", "isCorrect": false}
      ]
    },
    {
      "questionText": "Câu hỏi 2?",
      "options": [
        {"text": "Đáp án C", "isCorrect": false},
        {"text": "Đáp án D", "isCorrect": true}
      ]
    }
  ]
}
```

### Entities được tạo:

1. **MiniGame** (1 entity)
   - id = 1
   - title = "Quiz IT"
   - questionCount = 2
   - activity_id = 1

2. **MiniGameQuiz** (1 entity)
   - id = 1
   - mini_game_id = 1

3. **MiniGameQuizQuestion** (2 entities)
   - id = 1, questionText = "Câu hỏi 1?", displayOrder = 0, mini_game_quiz_id = 1
   - id = 2, questionText = "Câu hỏi 2?", displayOrder = 1, mini_game_quiz_id = 1

4. **MiniGameQuizOption** (4 entities)
   - id = 1, text = "Đáp án A", isCorrect = true, question_id = 1
   - id = 2, text = "Đáp án B", isCorrect = false, question_id = 1
   - id = 3, text = "Đáp án C", isCorrect = false, question_id = 2
   - id = 4, text = "Đáp án D", isCorrect = true, question_id = 2

**Tổng cộng:** 1 + 1 + 2 + 4 = **8 entities** được tạo

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Thứ tự tạo:**
   - Phải tạo Activity trước (type = MINIGAME)
   - Sau đó mới tạo MiniGame và các entity con

2. **Mối quan hệ:**
   - Activity ↔ MiniGame: OneToOne (unique)
   - MiniGame ↔ MiniGameQuiz: OneToOne (unique)
   - MiniGameQuiz ↔ MiniGameQuizQuestion: OneToMany
   - MiniGameQuizQuestion ↔ MiniGameQuizOption: OneToMany

3. **Cascade:**
   - Khi xóa Activity → Tự động xóa MiniGame → Tự động xóa MiniGameQuiz → Tự động xóa Questions → Tự động xóa Options
   - Khi xóa MiniGameQuiz → Tự động xóa Questions → Tự động xóa Options

4. **Điểm:**
   - Điểm cộng: Từ `MiniGame.rewardPoints` (khi đạt)
   - Điểm trừ: Từ `Activity.penaltyPointsIncomplete` (khi không đạt)
   - `Activity.maxPoints` KHÔNG được dùng để tính điểm

---

## 🎯 TÓM TẮT FLOW

```
1. Tạo Activity (type = MINIGAME)
   ↓
2. POST /api/minigames với activityId
   ↓
3. Tạo MiniGame (1 entity)
   ↓
4. Tạo MiniGameQuiz (1 entity)
   ↓
5. Vòng lặp questions:
   ├─ Tạo MiniGameQuizQuestion (n entities)
   └─ Vòng lặp options:
      └─ Tạo MiniGameQuizOption (m entities)
```

**Tổng số entities:** 1 (MiniGame) + 1 (MiniGameQuiz) + n (Questions) + m (Options)

