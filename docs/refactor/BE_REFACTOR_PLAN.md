# Backend Refactor Plan: Activity Score Rules + Score Ledger

## Mục Tiêu

Backend giữ kiến trúc mới. Frontend sẽ adapt theo backend mới.

```
Activity metadata
  -> ActivityScoreRule cấu hình điểm
  -> ScoreRuleEngine xử lý sự kiện nghiệp vụ
  -> ScoreEntry ledger là nguồn điểm chuẩn
  -> StudentScore là bảng tổng hợp/cache
```

---

## Cấu Trúc Package Đề Xuất

```
src/main/java/vn/campuslife/
├── model/
│   └── score/                          # [MỚI] DTOs cho score system
│       ├── ActivityScoreRuleRequest.java
│       ├── ActivityScoreRuleResponse.java
│       ├── ScoreEntryCommand.java
│       ├── ScoreEntryResponse.java
│       └── ...
│
├── service/
│   └── score/                          # [MỚI] Services cho score system
│       ├── ScoreRuleEngine.java
│       ├── ScoreRuleEngineImpl.java
│       ├── ScoreEntryService.java
│       ├── ScoreEntryServiceImpl.java
│       ├── ScoreService.java
│       ├── ScoreServiceImpl.java
│       ├── ActivityScoreRuleService.java
│       └── ActivityScoreRuleServiceImpl.java
│
├── entity/
│   └── score/                          # [MỚI] Entities cho score system
│       ├── ActivityScoreRule.java
│       ├── ScoreEntry.java
│       ├── StudentScore.java
│       └── ScoreHistory.java
│
├── enumeration/
│   ├── ScoreType.java                  # [ĐÃ CÓ]
│   ├── ScoreRuleTrigger.java           # [ĐÃ CÓ]
│   ├── ScoreRuleCalculation.java       # [ĐÃ CÓ]
│   ├── ScoreRuleAudience.java          # [ĐÃ CÓ]
│   ├── ScoreSemesterPolicy.java        # [ĐÃ CÓ]
│   ├── ScoreEntrySourceType.java       # [ĐÃ CÓ]
│   └── ScoreEntryStatus.java           # [ĐÃ CÓ]
```

**Nguyên tắc:**
- Entity `ActivityScoreRule` và `ScoreEntry` nên tách vào `entity/score/`
- DTOs cho score system nên vào `model/score/`
- Services cho score system nên vào `service/score/`
- Enums đã có sẵn trong `enumeration/`

---

## Các Phase Thực Hiện

---

### PHASE 1: Activity Score Rules Core (P0)

**Mục tiêu:** Đảm bảo Activity CRUD đúng với scoreRules.

#### 1.1 Persist `scoreRules` Khi Create/Update Activity

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `service/impl/ActivityServiceImpl.java` | Gọi `ActivityScoreRuleService.replaceRules()` sau khi lưu Activity |

**Logic:**
```
POST /api/activities
  -> save Activity
  -> if request.scoreRules != null: replaceRules(activityId, request.scoreRules)
  -> return ActivityResponse có scoreRules

PUT /api/activities/{id}
  -> update Activity
  -> replaceRules(activityId, request.scoreRules)
  -> return ActivityResponse có scoreRules
```

**Chú ý:**
- Nếu `scoreRules` rỗng/null: tạo activity không tính điểm (không sinh ScoreEntry)
- `ActivityScoreRuleService.replaceRules()` đã tồn tại - verify đã đúng

#### 1.2 Map `scoreRules` Vào `ActivityResponse`

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `model/ActivityResponse.java` | Verify có field `scoreRules: List<ActivityScoreRuleResponse>` |
| `service/impl/ActivityServiceImpl.java` | Mapper gọi `ActivityScoreRuleService.getRuleResponses(activityId)` |

**Chú ý:**
- Tất cả Activity endpoints (`GET /api/activities`, `GET /api/activities/{id}`, `POST`, `PUT`) đều trả `scoreRules`

#### 1.3 Copy Activity Phải Copy Score Rules

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `service/impl/ActivityServiceImpl.java` | Trong `copyActivity()`, gọi `ActivityScoreRuleService.copyRules()` |

**Logic:**
```
POST /api/activities/{id}/copy
  -> copy Activity metadata
  -> copy organizers
  -> copy all enabled scoreRules (với semesterPolicy = CURRENT_OPEN_SEMESTER, explicitSemester = null)
  -> return ActivityResponse có scoreRules
```

---

### PHASE 2: Build Verification & Enum Contract (P0)

**Mục tiêu:** Backend compile clean, enum BE/FE khớp nhau.

#### 2.1 Build Backend - Sửa Usage Field Điểm Cũ

**Kiểm tra các usage cũ:**
- `activity.getScoreType()` → phải thay bằng đọc từ `ActivityScoreRule`
- `activity.getMaxPoints()` → phải thay bằng đọc từ `ActivityScoreRule`
- `activity.getPenaltyPointsIncomplete()` → phải thay bằng đọc từ `ActivityScoreRule`

**Các vùng cần kiểm:**
- `StatisticsService`
- `RecommendationService` (chatbot)
- `EventArticleResponse` (nếu embed activity score type)
- `ActivityRepository` (các query filter theo scoreType)

**Acceptance:** `./mvnw compile` clean, không còn references đến field cũ.

#### 2.2 Thống Nhất Enum Contract BE/FE

**Enum chuẩn BE (source of truth):**

| Enum | Giá trị |
|---|---|
| `ScoreRuleTrigger` | `PARTICIPATION_COMPLETED`, `SUBMISSION_GRADED`, `MINIGAME_PASSED`, `SERIES_MILESTONE_REACHED` |
| `ScoreRuleCalculation` | `FIXED_POINTS`, `DYNAMIC` |
| `ScoreRuleAudience` | `ALL_PARTICIPANTS`, `DEPARTMENT_ONLY`, `OUTSIDE_DEPARTMENTS_ONLY` |
| `ScoreSemesterPolicy` | `ACTIVITY_SEMESTER`, `CURRENT_OPEN_SEMESTER`, `EXPLICIT_SEMESTER` |
| `ScoreEntrySourceType` | `ACTIVITY_PARTICIPATION`, `TASK_SUBMISSION`, `MINIGAME_ATTEMPT`, `SERIES_PROGRESS`, `MANUAL_ADJUSTMENT`, `RECALCULATION` |

**Ghi chú cho FE integration:**
- FE cũ có `TARGET_DEPARTMENTS`, `SPECIFIC_ROLES` → bỏ, dùng enum BE
- FE cũ có `ACTIVITY_CHECKIN`, `ACTIVITY_SUBMISSION` → map sang `ACTIVITY_PARTICIPATION`, `TASK_SUBMISSION`

---

### PHASE 3: Series Milestone qua ScoreRuleEngine (P1)

**Mục tiêu:** Series milestone đi qua engine, không tự ghi ledger.

#### 3.1 Implement `applySeriesMilestone()`

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `service/score/ScoreRuleEngineImpl.java` | Implement method `applySeriesMilestone(StudentSeriesProgress progress, User actor)` |

**Logic:**
```
applySeriesMilestone(progress, actor)
  -> lấy ActivitySeries từ progress.seriesId
  -> parse milestonePoints JSON
  -> tìm milestone cao nhất đạt được (completedCount >= milestone threshold)
  -> upsertEntry với:
     - sourceType = SERIES_PROGRESS
     - points = milestonePoints[milestone]
     - sourceId = progress.id
```

**Chú ý:**
- Chỉ upsert mốc cao nhất, không cộng dồn
- Milestone đạt rồi không giảm điểm nếu completedCount giảm

#### 3.2 Verify ActivitySeriesService Gọi Engine

**Files cần kiểm tra:**

| File | Kiểm tra |
|---|---|
| `service/impl/ActivitySeriesServiceImpl.java` | `updateStudentProgress()` gọi `scoreRuleEngine.applySeriesMilestone()` khi đạt milestone mới |

---

### PHASE 4: MiniGame & Score History Cleanup (P1)

**Mục tiêu:** MiniGame dùng scoreRules hoàn toàn, score history chuẩn hóa.

#### 4.1 Bỏ `MiniGame.rewardPoints` - Dùng Score Rules

**Decision:**
- Xóa `MiniGame.rewardPoints` (sau khi migrate dữ liệu)
- Điểm minigame cấu hình ở `ActivityScoreRule` với `triggerType = MINIGAME_PASSED`

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `entity/MiniGame.java` | Xóa field `rewardPoints` |
| `model/MiniGameResponse.java` | Xóa field `rewardPoints` |
| `model/CreateMiniGameRequest.java` | Xóa field `rewardPoints` |

**Chú ý:**
- Minigame độc lập: tạo Activity + ActivityScoreRule(`MINIGAME_PASSED`, `points=...`)
- Minigame thuộc series: không cộng điểm riêng, chỉ contribute vào series progress

#### 4.2 Chuẩn Hóa Score History Response

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `model/ScoreHistoryDetailResponse.java` | Thêm/verify field `sourceType: ScoreEntrySourceType` |
| `service/impl/ScoreServiceImpl.java` | `getScoreHistory()` trả `sourceType` từ `ScoreEntry.sourceType` |

**Mapping BE→FE (nếu FE cần UI-friendly):**
```
ACTIVITY_PARTICIPATION -> "ACTIVITY"
TASK_SUBMISSION        -> "ACTIVITY"
MINIGAME_ATTEMPT       -> "MINIGAME"
SERIES_PROGRESS        -> "MILESTONE"
```

---

### PHASE 5: Activity Endpoints DTO & Semester Policy (P1)

**Mục tiêu:** Tất cả Activity endpoints trả `ActivityResponse` (DTO), semester policy chính xác.

#### 5.1 Chuẩn Hóa Activity Endpoints Trả DTO

**Endpoints cần sửa:**

| Endpoint | Hiện tại | Cần đổi thành |
|---|---|---|
| `GET /api/activities/score-type/{scoreType}` | Trả `List<Activity>` | Trả `List<ActivityResponse>` |
| `GET /api/activities/department/{deptId}` | Trả `List<Activity>` | Trả `List<ActivityResponse>` |
| `GET /api/activities/my` | Trả `List<Activity>` | Trả `List<ActivityResponse>` |
| `GET /api/activities/upcoming` | Trả `List<Activity>` | Trả `List<ActivityResponse>` |
| `GET /api/activities/month` | Trả `List<Activity>` | Trả `List<ActivityResponse>` |

**Files cần sửa:**

| File | Thay đổi |
|---|---|
| `controller/ActivityController.java` | Đổi return type sang `ResponseEntity<Response>` với body `List<ActivityResponse>` |
| `service/impl/ActivityServiceImpl.java` | Các method trả `List<Activity>` → đổi thành `List<ActivityResponse>` |

#### 5.2 Verify Semester Policy Resolution

**Kiểm tra `ScoreSemesterResolver`:**

| Policy | Resolution |
|---|---|
| `ACTIVITY_SEMESTER` | `SemesterHelperService.getSemesterForActivity(activity)` |
| `CURRENT_OPEN_SEMESTER` | `semesterRepository.findOpenSemester()` |
| `EXPLICIT_SEMESTER` | `rule.getExplicitSemester()` |

**Chú ý:** `CURRENT_OPEN_SEMESTER` không dùng ngày event, mà dùng học kỳ đang open.

---

## Tổng Hợp Files Cần Tạo/Sửa Theo Phase

### PHASE 1: Activity Score Rules Core

```
SỬA:
- service/impl/ActivityServiceImpl.java        [Persist scoreRules, copy rules, mapper]
```

### PHASE 2: Build Verification & Enum Contract

```
SỬA:
- (nhiều files)                                 [Xóa usage field điểm cũ]
- Không cần sửa nếu enum đã đúng
```

### PHASE 3: Series Milestone

```
SỬA:
- service/score/ScoreRuleEngineImpl.java        [Implement applySeriesMilestone]
- service/impl/ActivitySeriesServiceImpl.java    [Gọi engine khi đạt milestone]
```

### PHASE 4: MiniGame & Score History

```
SỬA:
- entity/MiniGame.java                          [Xóa rewardPoints]
- model/MiniGameResponse.java                   [Xóa rewardPoints]
- model/CreateMiniGameRequest.java               [Xóa rewardPoints]
- model/ScoreHistoryDetailResponse.java          [Thêm sourceType]
- service/impl/ScoreServiceImpl.java             [Map sourceType]
```

### PHASE 5: Activity Endpoints DTO

```
SỬA:
- controller/ActivityController.java             [Đổi return type]
- service/impl/ActivityServiceImpl.java          [Đổi return type]
- service/ScoreSemesterResolver.java             [Verify resolution]
```

### Tái cấu trúc Package (Đề xuất, có thể làm riêng)

```
TẠO MỚI:
- entity/score/ActivityScoreRule.java            [Di chuyển từ entity/]
- entity/score/ScoreEntry.java                   [Di chuyển từ entity/]
- model/score/ActivityScoreRuleRequest.java       [Di chuyển từ model/]
- model/score/ActivityScoreRuleResponse.java      [Di chuyển từ model/]
- model/score/ScoreEntryCommand.java              [Di chuyển từ model/]
- model/score/ScoreEntryResponse.java             [Tạo mới]
- service/score/ScoreRuleEngine.java              [Di chuyển từ service/]
- service/score/ScoreRuleEngineImpl.java         [Di chuyển từ service/]
- service/score/ScoreEntryService.java            [Di chuyển từ service/]
- service/score/ScoreEntryServiceImpl.java        [Di chuyển từ service/]
- service/score/ActivityScoreRuleService.java    [Di chuyển từ service/]
- service/score/ActivityScoreRuleServiceImpl.java [Di chuyển từ service/]

CẬP NHẬT:
- import statements trong tất cả files referencing các class đã di chuyển
```

---

## Thứ Tự Thực Hiện

| Phase | Tên | Ưu tiên | Mô tả |
|---|---|---|---|
| **0** | **Package Restructure** | Tùy chọn | Di chuyển score-related classes vào package riêng |
| **1** | **Activity Score Rules Core** | **P0** | CRUD Activity với scoreRules |
| **2** | **Build Verification** | **P0** | Compile clean, enum khớp |
| **3** | **Series Milestone** | P1 | Engine xử lý milestone |
| **4** | **MiniGame & Score History** | P1 | Dọn dẹp rewardPoints, chuẩn hóa history |
| **5** | **Activity Endpoints DTO** | P1 | Chuẩn hóa return type |

**Khuyến nghị:** Thực hiện theo thứ tự 1→5. Phase 0 (package restructure) có thể làm trước hoặc sau, tùy preference team.

---

## Test Checklist

### Backend

- [ ] Tạo activity có 1 rule `PARTICIPATION_COMPLETED`
- [ ] Tạo activity có nhiều rules cùng lúc
- [ ] Update activity thay toàn bộ rules
- [ ] Copy activity giữ rules (semesterPolicy = CURRENT_OPEN_SEMESTER)
- [ ] Check-out activity tạo `ScoreEntry` và update `StudentScore`
- [ ] Submission đạt tạo điểm `points`
- [ ] Submission không đạt tạo điểm `failPoints`, mặc định `0`
- [ ] Chấm lại submission không cộng dồn
- [ ] Minigame pass không cộng trùng (qua scoreRules)
- [ ] Activity thuộc series không tạo điểm riêng
- [ ] Series milestone tạo/update `ScoreEntry` đúng source
- [ ] Ranking đọc từ `StudentScore` đã aggregate từ ledger
- [ ] `./mvnw compile` clean

---

## Xác Nhận

1. Bạn đồng ý với các phase trên?
2. Có muốn tôi bắt đầu implementation từ Phase nào trước?
3. Phase 0 (package restructure) có cần làm không, hay giữ nguyên cấu trúc hiện tại?
