# Backend Contract Audit Report

> **Generated from actual backend source code (HEAD).**  
> **Commit baseline:** `c848ee6` → **Current HEAD**  
> **Source of truth:** Java source under `src/main/java/vn/campuslife/`

---

## Part 1 – Activity APIs Inventory

### 1.1 Standard Activity APIs

| # | HTTP Method | Endpoint | Request DTO | Response DTO | Auth | Notes |
|---|-------------|----------|-------------|--------------|------|-------|
| 1 | `POST` | `/api/activities/standard` | `StandardActivityCreateRequest` | `Response<StandardActivityResponse>` | Admin/Manager | Creates a standard activity (SUKIEN, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP). |
| 2 | `PUT` | `/api/activities/standard/{id}` | `StandardActivityUpdateRequest` | `Response<StandardActivityResponse>` | Admin/Manager | Full update. `type` cannot be changed (omitted from request). |
| 3 | `GET` | `/api/activities/standard/{id}` | — | `Response<StandardActivityResponse>` | Any | Retrieves standard activity detail. |

### 1.2 Minigame Activity APIs

| # | HTTP Method | Endpoint | Request DTO | Response DTO | Auth | Notes |
|---|-------------|----------|-------------|--------------|------|-------|
| 1 | `POST` | `/api/activities/minigame` | `MinigameActivityCreateRequest` | `Response<MinigameActivityResponse>` | Admin/Manager | Creates a minigame activity shell + quiz. |
| 2 | `PATCH` | `/api/activities/minigame/{id}` | `MinigameActivityUpdateRequest` | `Response<MinigameActivityResponse>` | Admin/Manager | Partial update of shell + quiz. |
| 3 | `GET` | `/api/activities/minigame/{id}` | — | `Response<MinigameActivityResponse>` | Any | Retrieves minigame activity detail. |

### 1.3 Series Child Activity APIs

| # | HTTP Method | Endpoint | Request DTO | Response DTO | Auth | Notes |
|---|-------------|----------|-------------|--------------|------|-------|
| 1 | `POST` | `/api/series/{seriesId}/activities` | `SeriesChildActivityCreateRequest` | `Response<SeriesChildActivityResponse>` | Admin/Manager | Creates a new child activity inside a series. |
| 2 | `PUT` | `/api/series/{seriesId}/activities/{activityId}` | `SeriesChildActivityUpdateRequest` | `Response<SeriesChildActivityResponse>` | Admin/Manager | Updates a child activity. |
| 3 | `GET` | `/api/series/{seriesId}/activities/{activityId}` | — | `Response<SeriesChildActivityResponse>` | Any | Retrieves a child activity detail. |
| 4 | `POST` | `/api/series/{seriesId}/activities/attach` | `AddActivityToSeriesRequest` | `Response<Activity>` | Admin/Manager | Attaches an existing activity to a series. |
| 5 | `POST` | `/api/series/{seriesId}/activities/create` | `CreateSeriesActivityRequest` | `Response<Activity>` | Admin/Manager | **Deprecated.** Use `POST /api/series/{seriesId}/activities` instead. |

### 1.4 Legacy Activity APIs (`/api/activities`)

| # | HTTP Method | Endpoint | Request DTO | Response DTO | Auth | Notes |
|---|-------------|----------|-------------|--------------|------|-------|
| 1 | `POST` | `/api/activities` | `CreateActivityRequest` | `Response<ActivityResponse>` | Admin/Manager | **Legacy.** Still fully supported. |
| 2 | `PUT` | `/api/activities/{id}` | `CreateActivityRequest` | `Response<ActivityResponse>` | Admin/Manager | **Legacy.** Full update. |
| 3 | `GET` | `/api/activities` | — | `Response<List<ActivityResponse>>` | Any | Returns all activities (drafts filtered for students). |
| 4 | `GET` | `/api/activities/{id}` | — | `Response<ActivityResponse>` | Any | Activity detail. Drafts blocked for students. |
| 5 | `DELETE` | `/api/activities/{id}` | — | `Response<?>` | Admin/Manager | Soft delete. |
| 6 | `PUT` | `/api/activities/{id}/publish` | — | `Response<ActivityResponse>` | Admin/Manager | Publishes a draft activity. |
| 7 | `PUT` | `/api/activities/{id}/unpublish` | — | `Response<ActivityResponse>` | Admin/Manager | Unpublishes an activity. |
| 8 | `POST` | `/api/activities/{id}/copy` | `offsetDays` (query) | `Response<ActivityResponse>` | Admin/Manager | Copies an activity. |
| 9 | `GET` | `/api/activities/score-type/{scoreType}` | — | `List<ActivityResponse>` (raw) | Any | No wrapper response. |
| 10 | `GET` | `/api/activities/department/{deptId}` | — | `List<ActivityResponse>` (raw) | Any | No wrapper response. |
| 11 | `GET` | `/api/activities/my` | — | `List<ActivityResponse>` (raw) | Student | No wrapper response. Upcoming activities for current user. |
| 12 | `GET` | `/api/activities/upcoming` | `keyword` (query, opt) | `List<ActivityResponse>` (raw) | Any | No wrapper response. Search upcoming events. |
| 13 | `GET` | `/api/activities/month` | `year`, `month` (query, opt) | `List<ActivityResponse>` (raw) | Any | No wrapper response. Calendar view. |
| 14 | `GET` | `/api/activities/photos/all` | — | `Response<?>` | Any | All activity photos. |
| 15 | `POST` | `/api/activities/backfill-checkin-codes` | — | `Response<?>` | Admin/Manager | Auto-generates missing check-in codes. |
| 16 | `GET` | `/api/activities/{activityId}/requires-submission` | — | `Response<Map>` | Any | Returns `requiresSubmission` flag. |
| 17 | `GET` | `/api/activities/{activityId}/registration-status` | — | `Response<Map>` | Student | Returns registration status for current student. |
| 18 | `GET` | `/api/activities/debug/user-info` | — | `Response<Map>` | Any | Debug auth info. |

> **Deprecation status:** The legacy `/api/activities` CRUD endpoints remain fully functional. New frontend development should prefer the specialized endpoints (`/standard`, `/minigame`, `/series/{id}/activities`) for clearer contracts and better validation.

---

## Part 2 – DTO Verification

### 2.1 Standard Activity DTOs

#### `StandardActivityCreateRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | No | |
| `type` | `ActivityType` | No | `SUKIEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE_DOANH_NGHIEP` |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `location` | `String` | Yes | **Required by validator.** |
| `organizerIds` | `List<Long>` | Yes | **Required by validator.** |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `requiresSubmission` | `Boolean` | Yes | Defaults `false` in entity. |
| `requiresApproval` | `Boolean` | Yes | Defaults `true` in entity. |
| `ticketQuantity` | `Integer` | Yes | |
| `isImportant` | `Boolean` | Yes | Auto-registers all students when published. |
| `mandatoryForFacultyStudents` | `Boolean` | Yes | Auto-registers faculty students when published. |
| `isDraft` | `Boolean` | Yes | Defaults `true` in entity. |
| `bannerUrl` | `String` | Yes | |
| `shareLink` | `String` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `scoreRules` | `List<ActivityScoreRuleRequest>` | Yes | |
| `presetCode` | `ActivityPresetCode` | Yes | |
| `presetConfig` | `ActivityPresetConfig` | Yes | |

#### `StandardActivityUpdateRequest`

> **Code claim:** This is a **standalone class** in Java. It does **not** extend `StandardActivityCreateRequest`. The `type` field is simply omitted.

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | Yes | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `location` | `String` | Yes | |
| `organizerIds` | `List<Long>` | Yes | |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `requiresSubmission` | `Boolean` | Yes | |
| `requiresApproval` | `Boolean` | Yes | |
| `ticketQuantity` | `Integer` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `mandatoryForFacultyStudents` | `Boolean` | Yes | |
| `isDraft` | `Boolean` | Yes | |
| `bannerUrl` | `String` | Yes | |
| `shareLink` | `String` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `scoreRules` | `List<ActivityScoreRuleRequest>` | Yes | |
| `presetCode` | `ActivityPresetCode` | Yes | |
| `presetConfig` | `ActivityPresetConfig` | Yes | |

#### `StandardActivityResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `type` | `ActivityType` | Yes | Can be null for series children. |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `location` | `String` | Yes | |
| `organizerIds` | `List<Long>` | Yes | Empty list if no organizers. |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `hasPreparation` | `Boolean` | Yes | Always `false` currently. |
| `requiresSubmission` | `Boolean` | Yes | |
| `requiresApproval` | `Boolean` | Yes | |
| `ticketQuantity` | `Integer` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `mandatoryForFacultyStudents` | `Boolean` | Yes | |
| `isDraft` | `Boolean` | Yes | |
| `bannerUrl` | `String` | Yes | Full public URL applied by mapper. |
| `shareLink` | `String` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `checkInCode` | `String` | Yes | Format: `ACT-{000000}-{8 chars}` |
| `scoreRules` | `List<ActivityScoreRuleResponse>` | Yes | Fetched from `ActivityScoreRuleService`. |
| `createdAt` | `LocalDateTime` | Yes | |
| `updatedAt` | `LocalDateTime` | Yes | |
| `createdBy` | `String` | Yes | |
| `lastModifiedBy` | `String` | Yes | |

---

### 2.2 Minigame Activity DTOs

#### `MinigameActivityCreateRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | No | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `organizerIds` | `List<Long>` | Yes | |
| `requiresApproval` | `Boolean` | Yes | |
| `ticketQuantity` | `Integer` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `mandatoryForFacultyStudents` | `Boolean` | Yes | |
| `isDraft` | `Boolean` | Yes | |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `bannerUrl` | `String` | Yes | |
| `shareLink` | `String` | Yes | |
| `scoreRules` | `List<ActivityScoreRuleRequest>` | Yes | |
| `quiz` | `QuizConfigRequest` | Yes | **Required by validator.** |

**`QuizConfigRequest` (nested)**

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `title` | `String` | No | |
| `questionCount` | `Integer` | No | |
| `timeLimit` | `Integer` | No | Seconds. |
| `requiredCorrectAnswers` | `Integer` | No | |
| `maxAttempts` | `Integer` | No | |
| `showAnswers` | `Boolean` | Yes | |
| `questions` | `List<CreateMiniGameRequest.QuestionRequest>` | No | **Required by validator.** Min 1 question, each with ≥2 options and ≥1 correct option. |

#### `MinigameActivityUpdateRequest`

> **Code claim:** Standalone class. Does **not** extend `MinigameActivityCreateRequest`. Same fields as create.

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| *(Same as `MinigameActivityCreateRequest`)* | | | |

#### `MinigameActivityResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `type` | `ActivityType` | No | Always `MINIGAME`. |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `isDraft` | `Boolean` | Yes | |
| `bannerUrl` | `String` | Yes | Full public URL. |
| `shareLink` | `String` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `checkInCode` | `String` | Yes | |
| `scoreRules` | `List<ActivityScoreRuleResponse>` | Yes | |
| `createdAt` | `LocalDateTime` | Yes | |
| `updatedAt` | `LocalDateTime` | Yes | |
| `quiz` | `QuizConfigResponse` | Yes | |

**`QuizConfigResponse` (nested)**

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `title` | `String` | No | |
| `questionCount` | `Integer` | No | |
| `timeLimit` | `Integer` | No | |
| `requiredCorrectAnswers` | `Integer` | No | |
| `maxAttempts` | `Integer` | No | |
| `showAnswers` | `Boolean` | Yes | |
| `isActive` | `Boolean` | Yes | |

---

### 2.3 Series Child Activity DTOs

#### `SeriesChildActivityCreateRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | No | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `location` | `String` | Yes | |
| `order` | `Integer` | Yes | Maps to `seriesOrder`. |
| `bannerUrl` | `String` | Yes | |
| `shareLink` | `String` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `organizerIds` | `List<Long>` | Yes | |
| `type` | `ActivityType` | Yes | Can be null or any type (including `MINIGAME`). |

#### `SeriesChildActivityUpdateRequest`

> **Code claim:** Standalone class. Does **not** extend `SeriesChildActivityCreateRequest`. Same fields.

#### `SeriesChildActivityResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `type` | `ActivityType` | Yes | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `hasPreparation` | `boolean` | No | Always `false`. |
| `requiresSubmission` | `boolean` | No | Always `false` for series children. |
| `scoreRules` | `List<ActivityScoreRuleResponse>` | Yes | Typically empty for series children. |
| `registrationStartDate` | `LocalDateTime` | Yes | Inherited from series. |
| `registrationDeadline` | `LocalDateTime` | Yes | Inherited from series. |
| `shareLink` | `String` | Yes | |
| `isImportant` | `boolean` | No | Always `false`. |
| `isDraft` | `boolean` | No | Always `false` (auto-published). |
| `bannerUrl` | `String` | Yes | Full public URL. |
| `location` | `String` | Yes | |
| `ticketQuantity` | `Integer` | Yes | Inherited from series. |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `checkInCode` | `String` | Yes | Auto-generated. |
| `requiresApproval` | `boolean` | No | Inherited from series. |
| `mandatoryForFacultyStudents` | `boolean` | No | Always `false`. |
| `organizerIds` | `List<Long>` | Yes | |
| `seriesId` | `Long` | Yes | |
| `seriesOrder` | `Integer` | Yes | |
| `createdAt` | `LocalDateTime` | Yes | |
| `updatedAt` | `LocalDateTime` | Yes | |
| `createdBy` | `String` | Yes | |
| `lastModifiedBy` | `String` | Yes | |
| `seriesName` | `String` | Yes | Context field added by mapper. |

---

### 2.4 Legacy Activity DTOs

#### `CreateActivityRequest` (Legacy)

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | No | |
| `type` | `ActivityType` | No | |
| `presetCode` | `ActivityPresetCode` | Yes | |
| `presetConfig` | `ActivityPresetConfig` | Yes | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `requiresSubmission` | `Boolean` | Yes | |
| `scoreRules` | `List<ActivityScoreRuleRequest>` | Yes | |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `shareLink` | `String` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `isDraft` | `Boolean` | Yes | |
| `bannerUrl` | `String` | Yes | |
| `location` | `String` | Yes | **Required by validator.** |
| `ticketQuantity` | `Integer` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `requiresApproval` | `Boolean` | Yes | |
| `mandatoryForFacultyStudents` | `Boolean` | Yes | |
| `organizerIds` | `List<Long>` | Yes | **Required by validator.** |

#### `ActivityResponse` (Legacy)

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `type` | `ActivityType` | Yes | |
| `description` | `String` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `hasPreparation` | `boolean` | No | |
| `requiresSubmission` | `boolean` | No | |
| `scoreRules` | `List<ActivityScoreRuleResponse>` | Yes | |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `shareLink` | `String` | Yes | |
| `isImportant` | `boolean` | No | |
| `isDraft` | `boolean` | No | |
| `bannerUrl` | `String` | Yes | Full public URL. |
| `location` | `String` | Yes | |
| `ticketQuantity` | `Integer` | Yes | |
| `benefits` | `String` | Yes | |
| `requirements` | `String` | Yes | |
| `contactInfo` | `String` | Yes | |
| `checkInCode` | `String` | Yes | |
| `requiresApproval` | `boolean` | No | |
| `mandatoryForFacultyStudents` | `boolean` | No | |
| `organizerIds` | `List<Long>` | Yes | |
| `seriesId` | `Long` | Yes | |
| `seriesOrder` | `Integer` | Yes | |
| `createdAt` | `LocalDateTime` | Yes | |
| `updatedAt` | `LocalDateTime` | Yes | |
| `createdBy` | `String` | Yes | |
| `lastModifiedBy` | `String` | Yes | |

---

### 2.5 Activity Summary DTO

#### `ActivitySummaryResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `type` | `ActivityType` | Yes | |
| `startDate` | `LocalDateTime` | Yes | |
| `endDate` | `LocalDateTime` | Yes | |
| `bannerUrl` | `String` | Yes | Full public URL. |
| `isDraft` | `Boolean` | Yes | |
| `isImportant` | `Boolean` | Yes | |
| `location` | `String` | Yes | |
| `variantTag` | `String` | No | `"STANDARD"`, `"MINIGAME"`, `"SERIES_CHILD"` |
| `seriesId` | `Long` | Yes | |

---

### 2.6 Series DTOs

#### `CreateSeriesRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `name` | `String` | No | |
| `description` | `String` | Yes | |
| `milestonePoints` | `Map<Integer, Integer>` | Yes | Default `LinkedHashMap<>`. Key = min activities, Value = points. |
| `scoreType` | `ScoreType` | Yes | `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE` |
| `targetSemesterId` | `Long` | Yes | Explicit semester for milestone scoring. |
| `mainActivityId` | `Long` | Yes | Optional parent activity. |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `requiresApproval` | `Boolean` | Yes | Defaults `true` in controller logic. |
| `ticketQuantity` | `Integer` | Yes | |
| `minimumRequirementEnabled` | `Boolean` | Yes | |
| `minimumRequiredEvents` | `Integer` | Yes | Required > 0 when enabled. |
| `minimumPenaltyPoints` | `Integer` | Yes | Required > 0 when enabled. |
| `presetCode` | `SeriesPresetCode` | Yes | `SERIES_MILESTONE_BASIC`, `ENTERPRISE_SERIES`, `CUSTOM` |
| `presetConfig` | `SeriesPresetConfig` | Yes | |

#### `UpdateSeriesRequest`

> **Code claim:** Standalone class. Does **not** extend `CreateSeriesRequest`. Same fields.

#### `SeriesResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `name` | `String` | No | |
| `description` | `String` | Yes | |
| `milestonePoints` | `Map<Integer, Integer>` | Yes | Parsed from JSON string. |
| `scoreType` | `ScoreType` | No | |
| `mainActivityId` | `Long` | Yes | |
| `targetSemesterId` | `Long` | Yes | Added in fix. FK to `semesters` table. |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `requiresApproval` | `boolean` | No | |
| `ticketQuantity` | `Integer` | Yes | |
| `minimumRequirementEnabled` | `boolean` | No | |
| `minimumRequiredEvents` | `Integer` | Yes | |
| `minimumPenaltyPoints` | `Integer` | Yes | |
| `createdAt` | `LocalDateTime` | Yes | |

> **✅ FIXED:** `SeriesResponse` previously **missing** `targetSemesterId` in the actual source code. The `toSeriesResponse` method in `ActivitySeriesServiceImpl` now populates it (field added to `SeriesResponse` DTO and set in `toSeriesResponse`). `getAllSeries` still returns an ad-hoc `Map` that includes `targetSemesterId`.

#### `SeriesOverviewResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `seriesId` | `Long` | No | |
| `seriesName` | `String` | No | |
| `description` | `String` | Yes | |
| `scoreType` | `ScoreType` | No | |
| `targetSemesterId` | `Long` | Yes | Added manually in controller. |
| `milestonePoints` | `String` | Yes | Raw JSON string. |
| `milestonePointsMap` | `Map<String, Integer>` | Yes | Parsed map. |
| `registrationStartDate` | `LocalDateTime` | Yes | |
| `registrationDeadline` | `LocalDateTime` | Yes | |
| `requiresApproval` | `Boolean` | Yes | |
| `ticketQuantity` | `Integer` | Yes | |
| `minimumRequirementEnabled` | `Boolean` | Yes | |
| `minimumRequiredEvents` | `Integer` | Yes | |
| `minimumPenaltyPoints` | `Integer` | Yes | |
| `createdAt` | `LocalDateTime` | Yes | |
| `totalActivities` | `Integer` | Yes | Computed. |
| `totalRegisteredStudents` | `Long` | Yes | Computed. |
| `totalCompletedStudents` | `Long` | Yes | Computed. |
| `completionRate` | `Double` | Yes | Computed. |
| `totalMilestonePointsAwarded` | `BigDecimal` | Yes | Computed. |
| `minimumRequirementMetCount` | `Integer` | Yes | Computed. |
| `milestoneProgress` | `List<MilestoneProgressItem>` | Yes | |
| `activityStats` | `List<ActivityStatItem>` | Yes | |

**`MilestoneProgressItem`**

| Field | Java Type | Notes |
|-------|-----------|-------|
| `milestoneKey` | `String` | e.g., `"3"` |
| `milestoneCount` | `Integer` | Parsed from key. |
| `milestonePoints` | `Integer` | |
| `studentCount` | `Long` | |
| `percentage` | `Double` | % of registered students. |

**`ActivityStatItem`**

| Field | Java Type | Notes |
|-------|-----------|-------|
| `activityId` | `Long` | |
| `activityName` | `String` | |
| `order` | `Integer` | `seriesOrder` |
| `registrationCount` | `Long` | |
| `participationCount` | `Long` | `COMPLETED` count. |
| `participationRate` | `Double` | `participationCount / registrationCount` |

#### `SeriesProgressListResponse`

| Field | Java Type | Notes |
|-------|-----------|-------|
| `seriesId` | `Long` | |
| `seriesName` | `String` | |
| `totalActivities` | `Integer` | |
| `totalRegistered` | `Long` | |
| `progressList` | `List<SeriesProgressItemResponse>` | |
| `page` | `Integer` | |
| `size` | `Integer` | |
| `totalPages` | `Integer` | |
| `totalElements` | `Long` | |

#### `SeriesProgressItemResponse`

| Field | Java Type | Notes |
|-------|-----------|-------|
| `studentId` | `Long` | |
| `studentCode` | `String` | |
| `studentName` | `String` | |
| `className` | `String` | Optional. |
| `departmentName` | `String` | Optional. |
| `completedCount` | `Integer` | |
| `totalActivities` | `Integer` | |
| `pointsEarned` | `BigDecimal` | Milestone points. |
| `currentMilestone` | `String` | Key of highest reached milestone. |
| `completedActivityIds` | `List<Long>` | |
| `lastUpdated` | `LocalDateTime` | |
| `isRegistered` | `Boolean` | |

> **⚠️ CRITICAL:** There is **no** `SeriesStudentProgressView` DTO in the backend. The `GET /api/series/{seriesId}/progress/my` endpoint returns a `Map<String, Object>` built dynamically in `ActivitySeriesServiceImpl.getStudentProgress()`. The fields returned in the map are:
> - `studentId`, `seriesId`, `seriesName`, `completedCount`, `totalActivities`, `completedActivityIds`, `pointsEarned` (`BigDecimal`), `lastUpdated`, `currentMilestone`, `nextMilestoneCount`, `nextMilestonePoints`, `milestonePoints`, `scoreType`, `minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints`, `minimumRequirementMet`, `remainingToAvoidPenalty`.

---

### 2.7 Score Rule DTOs

#### `ActivityScoreRuleRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `scoreType` | `ScoreType` | No | |
| `triggerType` | `ScoreRuleTrigger` | No | |
| `calculation` | `ScoreRuleCalculation` | No | |
| `points` | `BigDecimal` | No | |
| `failPoints` | `BigDecimal` | Yes | |
| `audience` | `ScoreRuleAudience` | No | |
| `semesterPolicy` | `ScoreSemesterPolicy` | No | |
| `explicitSemesterId` | `Long` | Yes | |
| `departmentIds` | `List<Long>` | Yes | |
| `enabled` | `Boolean` | Yes | |

#### `ActivityScoreRuleResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `activityId` | `Long` | No | |
| `scoreType` | `ScoreType` | No | |
| `triggerType` | `ScoreRuleTrigger` | No | |
| `calculation` | `ScoreRuleCalculation` | No | |
| `points` | `BigDecimal` | No | |
| `failPoints` | `BigDecimal` | Yes | |
| `audience` | `ScoreRuleAudience` | No | |
| `semesterPolicy` | `ScoreSemesterPolicy` | No | |
| `explicitSemesterId` | `Long` | Yes | |
| `targetDepartmentIds` | `List<Long>` | Yes | |
| `enabled` | `Boolean` | Yes | |

> **⚠️ NOTE:** `points` and `failPoints` are `BigDecimal` in Java. Depending on Jackson configuration, they may serialize as JSON **numbers** or **strings**. The backend code does not use `@JsonFormat` to force string serialization. Frontend should handle both safely (e.g., `number | string`).

---

### 2.8 Preset DTOs

#### `ActivityPresetConfig`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `primaryScoreType` | `ScoreType` | Yes | |
| `participationPoints` | `BigDecimal` | Yes | |
| `participationFailPoints` | `BigDecimal` | Yes | |
| `noShowPenaltyEnabled` | `Boolean` | Yes | |
| `noShowPenaltyPoints` | `BigDecimal` | Yes | |
| `noShowPenaltyScoreType` | `ScoreType` | Yes | |
| `submissionPassPoints` | `BigDecimal` | Yes | |
| `submissionFailPoints` | `BigDecimal` | Yes | |
| `taskOverduePenaltyPoints` | `BigDecimal` | Yes | |
| `minigameExhaustedPenaltyPoints` | `BigDecimal` | Yes | |
| `bonusScoreType` | `ScoreType` | Yes | |
| `bonusPoints` | `BigDecimal` | Yes | |

> **⚠️ NOTE:** All point fields are `BigDecimal`, not `String`.

#### `ActivityPresetDefinitionResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `code` | `ActivityPresetCode` | No | |
| `displayName` | `String` | No | |
| `description` | `String` | No | |
| `recommendedActivityTypes` | `List<ActivityType>` | Yes | |
| `defaultRequiresSubmission` | `Boolean` | Yes | |
| `notes` | `List<String>` | Yes | |

#### `ActivityPresetPreviewResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `presetCode` | `ActivityPresetCode` | No | |
| `activityType` | `ActivityType` | No | |
| `requiresSubmission` | `boolean` | No | |
| `scoreRules` | `List<ActivityScoreRuleRequest>` | Yes | |
| `notes` | `List<String>` | Yes | |

#### `SeriesPresetDefinitionResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `code` | `SeriesPresetCode` | No | |
| `displayName` | `String` | No | |
| `description` | `String` | No | |
| `notes` | `List<String>` | Yes | |

#### `SeriesPresetPreviewResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `presetCode` | `SeriesPresetCode` | No | |
| `scoreType` | `ScoreType` | Yes | |
| `milestonePoints` | `Map<Integer, Integer>` | Yes | |
| `minimumRequirementEnabled` | `Boolean` | Yes | |
| `minimumRequiredEvents` | `Integer` | Yes | |
| `minimumPenaltyPoints` | `Integer` | Yes | |
| `notes` | `List<String>` | Yes | |

> **⚠️ NOTE:** `SeriesPresetPreviewResponse` does **not** contain `targetSemesterId`.

#### `SeriesPresetConfig`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `primaryScoreType` | `ScoreType` | Yes | |
| `milestonePoints` | `Map<Integer, Integer>` | Yes | |
| `minimumRequirementEnabled` | `Boolean` | Yes | |
| `minimumRequiredEvents` | `Integer` | Yes | |
| `minimumPenaltyPoints` | `Integer` | Yes | |

---

### 2.9 MiniGame (Quiz) DTOs

#### `CreateMiniGameRequest`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `activityId` | `Long` | Yes | Links to existing activity (for standalone minigame API). |
| `title` | `String` | No | |
| `description` | `String` | Yes | |
| `questionCount` | `Integer` | No | |
| `timeLimit` | `Integer` | No | |
| `requiredCorrectAnswers` | `Integer` | No | |
| `maxAttempts` | `Integer` | No | |
| `showAnswers` | `Boolean` | Yes | |
| `questions` | `List<QuestionRequest>` | No | |

#### `UpdateMiniGameRequest`

Same fields as `CreateMiniGameRequest` except `activityId` is omitted.

#### `MiniGameResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `title` | `String` | No | |
| `description` | `String` | Yes | |
| `questionCount` | `Integer` | No | |
| `timeLimit` | `Integer` | No | |
| `requiredCorrectAnswers` | `Integer` | No | |
| `maxAttempts` | `Integer` | No | |
| `isActive` | `Boolean` | Yes | |
| `showAnswers` | `Boolean` | Yes | |
| `type` | `MiniGameType` | Yes | Always `QUIZ`. |
| `activityId` | `Long` | Yes | |

#### `StartAttemptResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | Attempt ID. |
| `miniGameId` | `Long` | Yes | |
| `studentId` | `Long` | Yes | |
| `status` | `String` | No | e.g., `"IN_PROGRESS"` |
| `startedAt` | `LocalDateTime` | No | |
| `timeLimit` | `Integer` | Yes | From MiniGame. |

#### `SubmitAttemptResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `status` | `String` | No | e.g., `"PASSED"`, `"FAILED"` |
| `correctCount` | `Integer` | No | |
| `totalQuestions` | `Integer` | No | |
| `pointsEarned` | `BigDecimal` | Yes | |
| `startedAt` | `LocalDateTime` | No | |
| `submittedAt` | `LocalDateTime` | Yes | |
| `requiredCorrectAnswers` | `Integer` | No | |
| `participation` | `Object` | Yes | `ActivityParticipation` if passed, else null. |

#### `AttemptDetailResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `status` | `String` | No | |
| `correctCount` | `Integer` | No | |
| `totalQuestions` | `Integer` | No | |
| `pointsEarned` | `BigDecimal` | Yes | |
| `startedAt` | `LocalDateTime` | No | |
| `submittedAt` | `LocalDateTime` | Yes | |
| `requiredCorrectAnswers` | `Integer` | No | |
| `showAnswers` | `Boolean` | Yes | |
| `questions` | `List<QuizQuestionDetailResponse>` | Yes | |

#### `QuizQuestionDetailResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `questionText` | `String` | No | |
| `imageUrl` | `String` | Yes | Full public URL. |
| `displayOrder` | `Integer` | Yes | |
| `options` | `List<QuizOptionDetailResponse>` | Yes | |
| `correctOptionId` | `Long` | Yes | Only if `showAnswers` is true. |
| `selectedOptionId` | `Long` | Yes | Student's answer. |
| `isCorrect` | `Boolean` | Yes | Only meaningful if `showAnswers` is true. |

#### `QuizOptionDetailResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `text` | `String` | No | |
| `isCorrect` | `Boolean` | Yes | `null` if `showAnswers` is false. |
| `isSelected` | `Boolean` | Yes | |

---

### 2.10 Score & Ranking DTOs

#### `ScoreHistoryViewResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `studentId` | `Long` | No | |
| `studentCode` | `String` | No | |
| `studentName` | `String` | No | |
| `semesterId` | `Long` | No | |
| `semesterName` | `String` | No | |
| `scoreType` | `ScoreType` | Yes | |
| `currentScore` | `BigDecimal` | Yes | |
| `scoreHistories` | `List<ScoreHistoryDetailResponse>` | Yes | |
| `activityParticipations` | `List<ActivityParticipationDetailResponse>` | Yes | |
| `totalRecords` | `Long` | Yes | |
| `page` | `Integer` | Yes | |
| `size` | `Integer` | Yes | |
| `totalPages` | `Integer` | Yes | |

#### `ScoreHistoryDetailResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `oldScore` | `BigDecimal` | Yes | Running total before this entry. |
| `newScore` | `BigDecimal` | Yes | Running total after this entry. |
| `changeDate` | `LocalDateTime` | Yes | |
| `reason` | `String` | Yes | |
| `activityId` | `Long` | Yes | |
| `activityName` | `String` | Yes | |
| `seriesId` | `Long` | Yes | |
| `seriesName` | `String` | Yes | |
| `sourceType` | `String` | Yes | Enum name as string. |
| `changedByUsername` | `String` | Yes | |
| `changedByFullName` | `String` | Yes | Always null currently. |

#### `StudentRankingResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `rank` | `Integer` | No | |
| `studentId` | `Long` | No | |
| `studentCode` | `String` | No | |
| `studentName` | `String` | No | |
| `departmentId` | `Long` | Yes | |
| `departmentName` | `String` | Yes | |
| `classId` | `Long` | Yes | |
| `className` | `String` | Yes | |
| `semesterId` | `Long` | Yes | |
| `semesterName` | `String` | Yes | |
| `scoreType` | `ScoreType` | Yes | Null for total ranking. |
| `score` | `BigDecimal` | Yes | |
| `scoreTypeLabel` | `String` | Yes | e.g., "Điểm rèn luyện", "Tổng điểm" |

#### `ActivityParticipationDetailResponse`

| Field | Java Type | Nullable | Notes |
|-------|-----------|----------|-------|
| `id` | `Long` | No | |
| `activityId` | `Long` | No | |
| `activityName` | `String` | No | |
| `activityType` | `ActivityType` | Yes | |
| `seriesId` | `Long` | Yes | |
| `seriesName` | `String` | Yes | |
| `pointsEarned` | `BigDecimal` | Yes | |
| `participationType` | `ParticipationType` | No | |
| `date` | `LocalDateTime` | Yes | |
| `isCompleted` | `Boolean` | Yes | |
| `sourceType` | `String` | Yes | `"ACTIVITY"` or `"MINIGAME"` |

---

## Part 3 – Endpoint Verification (Definitive API Reference)

### 3.1 Activity Endpoints

| Method | Endpoint | Request Body / Query | Response | Auth |
|--------|----------|----------------------|----------|------|
| `POST` | `/api/activities` | `CreateActivityRequest` | `Response<ActivityResponse>` | Admin/Manager |
| `PUT` | `/api/activities/{id}` | `CreateActivityRequest` | `Response<ActivityResponse>` | Admin/Manager |
| `GET` | `/api/activities` | — | `Response<List<ActivityResponse>>` | Any (students see non-draft only) |
| `GET` | `/api/activities/{id}` | — | `Response<ActivityResponse>` | Any (students blocked from drafts) |
| `DELETE` | `/api/activities/{id}` | — | `Response<?>` | Admin/Manager |
| `PUT` | `/api/activities/{id}/publish` | — | `Response<ActivityResponse>` | Admin/Manager |
| `PUT` | `/api/activities/{id}/unpublish` | — | `Response<ActivityResponse>` | Admin/Manager |
| `POST` | `/api/activities/{id}/copy` | `offsetDays` (query, opt) | `Response<ActivityResponse>` | Admin/Manager |
| `GET` | `/api/activities/presets` | — | `Response<List<ActivityPresetDefinitionResponse>>` | Admin/Manager |
| `POST` | `/api/activities/presets/preview` | `ActivityPresetPreviewRequest` | `Response<ActivityPresetPreviewResponse>` | Admin/Manager |
| `GET` | `/api/activities/score-type/{scoreType}` | — | `List<ActivityResponse>` (raw) | Any |
| `GET` | `/api/activities/department/{deptId}` | — | `List<ActivityResponse>` (raw) | Any |
| `GET` | `/api/activities/my` | — | `List<ActivityResponse>` (raw) | Student |
| `GET` | `/api/activities/upcoming` | `keyword` (query, opt) | `List<ActivityResponse>` (raw) | Any |
| `GET` | `/api/activities/month` | `year`, `month` (query, opt) | `List<ActivityResponse>` (raw) | Any |
| `GET` | `/api/activities/photos/all` | — | `Response<?>` | Any |
| `POST` | `/api/activities/backfill-checkin-codes` | — | `Response<Map>` | Admin/Manager |
| `GET` | `/api/activities/{activityId}/requires-submission` | — | `Response<Map>` | Any |
| `GET` | `/api/activities/{activityId}/registration-status` | — | `Response<Map>` | Student |
| `GET` | `/api/activities/debug/user-info` | — | `Response<Map>` | Any |
| `POST` | `/api/activities/standard` | `StandardActivityCreateRequest` | `Response<StandardActivityResponse>` | Admin/Manager |
| `PUT` | `/api/activities/standard/{id}` | `StandardActivityUpdateRequest` | `Response<StandardActivityResponse>` | Admin/Manager |
| `GET` | `/api/activities/standard/{id}` | — | `Response<StandardActivityResponse>` | Any |
| `POST` | `/api/activities/minigame` | `MinigameActivityCreateRequest` | `Response<MinigameActivityResponse>` | Admin/Manager |
| `PATCH` | `/api/activities/minigame/{id}` | `MinigameActivityUpdateRequest` | `Response<MinigameActivityResponse>` | Admin/Manager |
| `GET` | `/api/activities/minigame/{id}` | — | `Response<MinigameActivityResponse>` | Any |

### 3.2 Series Endpoints

| Method | Endpoint | Request Body / Query | Response | Auth |
|--------|----------|----------------------|----------|------|
| `GET` | `/api/series/presets` | — | `Response<List<SeriesPresetDefinitionResponse>>` | Admin/Manager |
| `POST` | `/api/series/presets/preview` | `SeriesPresetPreviewRequest` | `Response<SeriesPresetPreviewResponse>` | Admin/Manager |
| `POST` | `/api/series` | `CreateSeriesRequest` | `Response<SeriesResponse>` | Admin/Manager |
| `GET` | `/api/series` | — | `Response<List<Map>>` | Any |
| `GET` | `/api/series/{seriesId}` | — | `Response<SeriesResponse>` | Any |
| `PUT` | `/api/series/{seriesId}` | `UpdateSeriesRequest` | `Response<SeriesResponse>` | Admin/Manager |
| `DELETE` | `/api/series/{seriesId}` | — | `Response<?>` | Admin/Manager |
| `POST` | `/api/series/{seriesId}/activities` | `SeriesChildActivityCreateRequest` | `Response<SeriesChildActivityResponse>` | Admin/Manager |
| `PUT` | `/api/series/{seriesId}/activities/{activityId}` | `SeriesChildActivityUpdateRequest` | `Response<SeriesChildActivityResponse>` | Admin/Manager |
| `GET` | `/api/series/{seriesId}/activities/{activityId}` | — | `Response<SeriesChildActivityResponse>` | Any |
| `GET` | `/api/series/{seriesId}/activities` | — | `Response<List<SeriesChildActivityResponse>>` | Any |
| `POST` | `/api/series/{seriesId}/activities/attach` | `AddActivityToSeriesRequest` | `Response<Activity>` | Admin/Manager |
| `POST` | `/api/series/{seriesId}/activities/create` | `CreateSeriesActivityRequest` | `Response<Activity>` | Admin/Manager | **Deprecated** |
| `POST` | `/api/series/{seriesId}/register` | — | `Response<List<ActivityRegistration>>` | Student |
| `GET` | `/api/series/{seriesId}/progress/my` | — | `Response<Map<String, Object>>` | Student |
| `GET` | `/api/series/{seriesId}/registration/my` | — | `Response<Map>` | Student |
| `GET` | `/api/series/{seriesId}/students/{studentId}/progress` | — | `Response<Map<String, Object>>` | Admin/Manager |
| `GET` | `/api/series/{seriesId}/progress` | `page`, `size`, `keyword` | `Response<SeriesProgressListResponse>` | Admin/Manager |
| `GET` | `/api/series/{seriesId}/overview` | — | `Response<SeriesOverviewResponse>` | Admin/Manager |
| `POST` | `/api/series/{seriesId}/students/{studentId}/calculate-milestone` | — | `Response<StudentSeriesProgress>` | Admin/Manager |

### 3.3 MiniGame (Quiz) Endpoints

| Method | Endpoint | Request Body / Query | Response | Auth |
|--------|----------|----------------------|----------|------|
| `POST` | `/api/minigames` | `CreateMiniGameRequest` | `Response<MiniGameResponse>` | Admin/Manager |
| `GET` | `/api/minigames` | — | `Response<List<MiniGameResponse>>` | Admin/Manager |
| `GET` | `/api/minigames/activity/{activityId}` | — | `Response<MiniGameResponse>` | Any |
| `GET` | `/api/minigames/activity/{activityId}/check` | — | `Response<Boolean>` | Any |
| `POST` | `/api/minigames/{miniGameId}/start` | — | `Response<StartAttemptResponse>` | Student |
| `POST` | `/api/minigames/attempts/{attemptId}/submit` | `Map<String, Object>` (answers map) | `Response<SubmitAttemptResponse>` | Student |
| `GET` | `/api/minigames/{miniGameId}/attempts/my` | — | `Response<List<AttemptDetailResponse>>` | Student |
| `GET` | `/api/minigames/{miniGameId}/questions` | — | `Response<List<QuizQuestionDetailResponse>>` | Any |
| `GET` | `/api/minigames/{miniGameId}/questions/edit` | — | `Response<List<QuizQuestionEditResponse>>` | Admin/Manager |
| `GET` | `/api/minigames/attempts/{attemptId}` | — | `Response<AttemptDetailResponse>` | Student/Admin |
| `PUT` | `/api/minigames/{miniGameId}` | `UpdateMiniGameRequest` | `Response<MiniGameResponse>` | Admin/Manager |
| `DELETE` | `/api/minigames/{miniGameId}` | — | `Response<?>` | Admin/Manager |

### 3.4 Registration & Check-in Endpoints

| Method | Endpoint | Request Body / Query | Response | Auth |
|--------|----------|----------------------|----------|------|
| `POST` | `/api/registrations` | `ActivityRegistrationRequest` | `Response<ActivityRegistration>` | Student |
| `DELETE` | `/api/registrations/activity/{activityId}` | — | `Response<?>` | Student |
| `GET` | `/api/registrations/my` | — | `Response<List<ActivityRegistration>>` | Student |
| `GET` | `/api/registrations/activity/{activityId}` | — | `Response<List<ActivityRegistration>>` | Admin/Manager |
| `GET` | `/api/registrations/series/{seriesId}` | — | `Response<List<ActivityRegistration>>` | Admin/Manager |
| `PUT` | `/api/registrations/{registrationId}/status` | `status` (query) | `Response<ActivityRegistration>` | Admin/Manager |
| `GET` | `/api/registrations/{registrationId}` | — | `Response<ActivityRegistration>` | Admin/Manager |
| `GET` | `/api/registrations/check/{activityId}` | — | `Response<Map>` | Student |
| `GET` | `/api/registrations/checkin/validate` | `ticketCode` (query) | `Response<Map>` | Admin/Manager |
| `POST` | `/api/registrations/checkin` | `ActivityParticipationRequest` | `Response<ActivityParticipation>` | Admin/Manager |
| `POST` | `/api/registrations/checkin/qr` | `Map<String, String>` (`checkInCode`) | `Response<?>` | Student |
| `GET` | `/api/registrations/personal-calendar` | — | `Response<Map>` | Student |
| `GET` | `/api/registrations/activities/{activityId}/report` | — | `Response<Map>` | Admin/Manager |
| `PUT` | `/api/registrations/participations/{participationId}/grade` | `isCompleted`, `notes` (query) | `Response<ActivityParticipation>` | Admin/Manager |
| `POST` | `/api/registrations/backfill/participations` | — | `Response<Map>` | Admin/Manager |
| `GET` | `/api/registrations/activities/{activityId}/participations` | — | `Response<List<ActivityParticipation>>` | Admin/Manager |
| `GET` | `/api/registrations/my/{status}` | — | `Response<List<ActivityRegistration>>` | Student |
| `GET` | `/api/registrations/search` | `keyword`, `status` | `Response<List<ActivityRegistration>>` | Admin/Manager |

### 3.5 Score Endpoints

| Method | Endpoint | Request Body / Query | Response | Auth |
|--------|----------|----------------------|----------|------|
| `GET` | `/api/scores/student/{studentId}/semester/{semesterId}` | — | `Response<ScoreViewResponse>` | Any |
| `GET` | `/api/scores/student/{studentId}/semester/{semesterId}/total` | — | `Response<Map>` | Any |
| `GET` | `/api/scores/ranking` | `semesterId` (req), `scoreType`, `departmentId`, `classId`, `sortOrder` | `Response<Map>` | Any |
| `POST` | `/api/scores/recalculate/student/{studentId}` | `semesterId` (query, opt) | `Response<?>` | Admin/Manager |
| `POST` | `/api/scores/recalculate/all` | `semesterId` (query, opt) | `Response<Map>` | Admin/Manager |
| `GET` | `/api/scores/history/student/{studentId}` | `semesterId` (req), `scoreType`, `page`, `size` | `Response<ScoreHistoryViewResponse>` | Any (students can only view own) |

---

## Part 4 – Series Audit

### 4.1 Field Verification Matrix

| Field | Exists in Entity? | Exposed in DTO? | Read API? | Write API? | Java Type | Business Meaning |
|-------|-------------------|-----------------|-----------|------------|-----------|------------------|
| `targetSemester` | ✅ Yes (`ActivitySeries.targetSemester`) | ⚠️ Partial | ✅ `getAllSeries` (Map), `getSeriesOverview` | ✅ `createSeries`, `updateSeries` | `Semester` (entity) | Semester to which milestone points are credited. If null, backend infers from first activity's startDate. |
| `targetSemesterId` | ⚠️ Via `targetSemester.getId()` | ❌ **Missing from `SeriesResponse`** | ✅ In ad-hoc Map from `getAllSeries`; in `SeriesOverviewResponse` | ✅ `CreateSeriesRequest`, `UpdateSeriesRequest` | `Long` | FK to `semesters` table. |
| `minimumRequirementEnabled` | ✅ Yes | ✅ `SeriesResponse`, `SeriesOverviewResponse`, `CreateSeriesRequest`, `UpdateSeriesRequest` | ✅ `GET /api/series/{id}`, `GET /api/series/{id}/overview` | ✅ `POST /api/series`, `PUT /api/series/{id}` | `boolean` | Enables penalty if student attends fewer than `minimumRequiredEvents`. |
| `minimumRequiredEvents` | ✅ Yes | ✅ Same as above | ✅ Same | ✅ Same | `Integer` | Minimum number of activities a student must complete to avoid penalty. Must be > 0 when enabled. |
| `minimumPenaltyPoints` | ✅ Yes | ✅ Same as above | ✅ Same | ✅ Same | `Integer` | Positive number of points deducted if requirement is not met. Backend negates it automatically. |
| `minimumRequirementMetCount` | ⚠️ Computed | ✅ `SeriesOverviewResponse` | ✅ `GET /api/series/{id}/overview` | ❌ N/A | `Integer` | Count of students who have completed >= `minimumRequiredEvents`. |
| `milestonePoints` | ✅ Yes (JSON string) | ✅ `SeriesResponse` (as Map), `SeriesOverviewResponse` (as String + Map) | ✅ `GET /api/series/{id}`, `GET /api/series/{id}/overview` | ✅ `POST /api/series`, `PUT /api/series/{id}` | `String` (entity), `Map<Integer,Integer>` (DTO) | Map of {minActivities: pointsAwarded}. |

### 4.2 Key Findings

1. **`targetSemesterId` is missing from `SeriesResponse`**
   - The DTO `SeriesResponse` (returned by `GET /api/series/{seriesId}` and `POST/PUT /api/series`) does **not** contain `targetSemesterId`.
   - The `toSeriesResponse` method in `ActivitySeriesServiceImpl` (line ~1362) does not map it.
   - `getAllSeries` returns an ad-hoc `Map<String, Object>` that **does** include `targetSemesterId`, so the list API is more complete than the detail API.

2. **`SeriesPresetPreviewResponse` lacks `targetSemesterId`**
   - The preview endpoint (`POST /api/series/presets/preview`) returns a DTO without `targetSemesterId`, even though the existing documentation shows it in an example JSON.

3. **`SeriesStudentProgressView` DTO does not exist**
   - The `GET /api/series/{seriesId}/progress/my` endpoint returns `Response<Map<String, Object>>` built dynamically.
   - Frontend must consume a loosely-typed Map, not a strongly-typed DTO.

---

## Part 5 – Documentation Discrepancy Report

### 5.1 Critical Discrepancies (Code vs. Existing Docs)

| # | Documentation Claim | Actual Backend | Correction Required |
|---|---------------------|----------------|---------------------|
| 1 | `StandardActivityUpdateRequest extends Omit<StandardActivityCreateRequest, 'type'>` | **Standalone class.** No inheritance. `type` is simply omitted. | Remove `extends` claim. Document as standalone DTO with same fields minus `type`. |
| 2 | `MinigameActivityUpdateRequest extends MinigameActivityCreateRequest` | **Standalone class.** No inheritance. | Remove `extends` claim. |
| 3 | `SeriesChildActivityUpdateRequest extends SeriesChildActivityCreateRequest` | **Standalone class.** No inheritance. | Remove `extends` claim. |
| 4 | `UpdateSeriesRequest extends CreateSeriesRequest` | **Standalone class.** No inheritance. | Remove `extends` claim. |
| 5 | `SeriesResponse` contains `targetSemesterId` | **`SeriesResponse` does NOT have this field.** | **Backend bug or docs bug.** Either add `targetSemesterId` to `SeriesResponse` and `toSeriesResponse`, or remove from docs. |
| 6 | `SeriesPresetPreviewResponse` contains `targetSemesterId` | **Does NOT exist.** Only `scoreType`, `milestonePoints`, `minimumRequirementEnabled`, `minimumRequiredEvents`, `minimumPenaltyPoints`, `notes`. | Remove `targetSemesterId` from docs. |
| 7 | `GET /api/series/{seriesId}/progress/my` returns `SeriesStudentProgressView` | **Returns `Map<String, Object>`.** No such DTO exists. | Replace typed DTO with Map contract. Document exact keys. |
| 8 | `ActivityPresetConfig` points fields are `string` (TypeScript) | **`BigDecimal`** in Java (`participationPoints`, `noShowPenaltyPoints`, etc.). | Document as `BigDecimal` / JSON number. Frontend should treat as `number` or `string` depending on Jackson config. |
| 9 | `ActivityScoreRuleRequest.points` is `string` | **`BigDecimal`** in Java. | Document as `BigDecimal` / JSON number. |
| 10 | `ActivityScoreRuleResponse.points` is `string` | **`BigDecimal`** in Java. | Document as `BigDecimal` / JSON number. |
| 11 | `SubmitAttemptResponse.pointsEarned` is `string` | **`BigDecimal`** in Java. | Document as `BigDecimal` / JSON number. |
| 12 | `StandardActivityResponse` boolean fields are `boolean` primitives | **`Boolean` wrappers** (`isImportant`, `isDraft`, `hasPreparation`, etc.). | Document as `boolean` (JSON has no difference), but note they are nullable wrappers in Java. |
| 13 | `MinigameActivityResponse` boolean fields are `boolean` primitives | **`Boolean` wrappers** (`isDraft`, `isImportant`). | Same as above. |
| 14 | `ActivitySummaryResponse` boolean fields are `boolean` primitives | **`Boolean` wrappers** (`isDraft`, `isImportant`). | Same as above. |
| 15 | `GET /api/activities/upcoming` takes no params | **Accepts `keyword` query parameter.** | Document optional `keyword` param. |
| 16 | `GET /api/activities/month` takes no params | **Accepts `year` and `month` query parameters.** | Document optional `year`, `month` params. Defaults to current month. |
| 17 | `POST /api/activities/{id}/copy` not documented | **Exists.** Accepts `offsetDays` query param. | Add to docs. |
| 18 | `SeriesOverviewResponse.minimumRequirementMetCount` documented | **Field exists.** But `totalCompletedStudents` is "completed ALL activities", not "completed at least one". | Clarify definitions: `totalCompletedStudents` = completed all activities; `minimumRequirementMetCount` = met minimum required events. |
| 19 | `minimumPenaltyPoints` docs say "FE truyền số dương, BE tự chuyển thành điểm trừ" | **Partially correct.** The `ScoreRuleEngineImpl.applySignForFailure` negates `failPoints` for `PENALTY_POINTS` and `PASS_FAIL_POINTS`. However, for series minimum requirement, the engine uses `BigDecimal.valueOf(minimumPenaltyPoints).negate()` directly. | Document that series minimum penalty is always negated by the engine. Frontend should still send positive integer. |
| 20 | `GET /api/series/{seriesId}/students/{studentId}/progress` returns `SeriesStudentProgressView` | **Returns `Map<String, Object>`** identical to `/progress/my`. | Update docs to reflect actual Map response. |
| 21 | `POST /api/minigames` docs say request is `CreateMiniGameRequest` | **Correct**, but note that `MinigameActivityCreateRequest` (used by `/api/activities/minigame`) is a **different** DTO that wraps shell fields + quiz. | Clarify distinction between standalone minigame API and activity-shell minigame API. |
| 22 | `PUT /api/minigames/{miniGameId}` docs say request is `UpdateMiniGameRequest` | **Correct.** | No change. |
| 23 | `GET /api/minigames/attempts/{attemptId}` docs say response includes `showAnswers` and filters `isCorrect` | **Correct.** `AttemptDetailResponse` has `showAnswers`. `QuizOptionDetailResponse.isCorrect` is set to `null` when `showAnswers=false`. | No change. |
| 24 | `GET /api/series/{seriesId}/progress` (admin paginated list) | **Exists.** Returns `SeriesProgressListResponse`. | Missing from original docs; should be added. |
| 25 | `GET /api/series/{seriesId}/registration/my` | **Exists.** Returns `Map` with `isRegistered`. | Missing from original docs; should be added. |
| 26 | `POST /api/series/{seriesId}/students/{studentId}/calculate-milestone` | **Exists.** Triggers milestone calculation for a single student. | Missing from original docs; should be added. |
| 27 | `ActivityType` enum | **Missing `MINIGAME` in some docs?** Actually present. | `ActivityType` = `SUKIEN`, `MINIGAME`, `CONG_TAC_XA_HOI`, `CHUYEN_DE_DOANH_NGHIEP`. |
| 28 | `ScoreType` enum | **Missing `KHAC`**. | `ScoreType` = `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`. No `KHAC` value exists. |
| 29 | `ScoreEntrySourceType` enum | **Docs missing `RECALCULATION`**. | Full list: `ACTIVITY_PARTICIPATION`, `ACTIVITY_REGISTRATION`, `TASK_SUBMISSION`, `TASK_ASSIGNMENT`, `MINIGAME_ATTEMPT`, `SERIES_PROGRESS`, `SERIES_MINIMUM_REQUIREMENT`, `MANUAL_ADJUSTMENT`, `RECALCULATION`. |
| 30 | `SeriesResponse` has `targetSemesterName` | **Does NOT exist.** | Remove from docs. |

### 5.2 Minor Discrepancies

| # | Documentation Claim | Actual Backend | Correction Required |
|---|---------------------|----------------|---------------------|
| 31 | `StandardActivityResponse.organizerIds` marked optional in docs | **Not nullable in response** (always present, may be empty). | Mark as required in response. |
| 32 | `StandardActivityCreateRequest.scoreRules` uses `ActivityScoreRuleRequest` | **Correct.** | No change. |
| 33 | `CreateActivityRequest` (legacy) still works | **Confirmed.** All legacy endpoints remain functional. | No change; recommend migration. |
| 34 | `GET /api/activities/photos/all` | **Exists.** Returns all activity photos. | Missing from docs. |
| 35 | `POST /api/activities/backfill-checkin-codes` | **Exists.** Admin-only. | Missing from docs. |
| 36 | `GET /api/activities/{activityId}/requires-submission` | **Exists.** | Missing from docs. |
| 37 | `GET /api/activities/{activityId}/registration-status` | **Exists.** Student-only. | Missing from docs. |
| 38 | `GET /api/activities/debug/user-info` | **Exists.** Debug auth. | Missing from docs. |
| 39 | `GET /api/registrations/checkin/validate` | **Exists.** Validates ticket code. | Missing from docs. |
| 40 | `GET /api/registrations/checkin/test` | **Exists.** Debug endpoint. | Missing from docs. |
| 41 | `GET /api/registrations/personal-calendar` | **Exists.** Student joined events. | Missing from docs. |
| 42 | `GET /api/registrations/activities/{activityId}/report` | **Exists.** Participation report. | Missing from docs. |
| 43 | `POST /api/registrations/backfill/participations` | **Exists.** Creates missing participations. | Missing from docs. |
| 44 | `GET /api/registrations/activities/{activityId}/participations` | **Exists.** List participations. | Missing from docs. |
| 45 | `GET /api/registrations/my/{status}` | **Exists.** Filtered by status. | Missing from docs. |
| 46 | `GET /api/registrations/search` | **Exists.** Keyword + status search. | Missing from docs. |
| 47 | `GET /api/minigames/activity/{activityId}/check` | **Exists.** Checks if activity has quiz. | Missing from docs. |
| 48 | `GET /api/minigames/{miniGameId}/questions/edit` | **Exists.** Admin-only edit view with correct answers. | Missing from docs. |
| 49 | `GET /api/minigames/{miniGameId}/questions` | **Exists.** Public question list (no correct answers). | Missing from docs. |
| 50 | `GET /api/minigames/{miniGameId}/attempts/my` | **Exists.** Student's own attempts. | Missing from docs. |

---

## Appendix – Enumerations (Verified from Code)

### `ActivityType`
```java
SUKIEN, MINIGAME, CONG_TAC_XA_HOI, CHUYEN_DE_DOANH_NGHIEP
```

### `ScoreType`
```java
REN_LUYEN, CONG_TAC_XA_HOI, CHUYEN_DE
```

### `ScoreRuleTrigger`
```java
PARTICIPATION_COMPLETED, NO_SHOW, SUBMISSION_GRADED, MINIGAME_PASSED,
MINIGAME_EXHAUSTED_ATTEMPTS, SERIES_MILESTONE_REACHED, TASK_OVERDUE
```

### `ScoreRuleCalculation`
```java
FIXED_POINTS, COUNT_COMPLETION, PASS_FAIL_POINTS, PENALTY_POINTS, SERIES_MILESTONE
```

### `ScoreRuleAudience`
```java
ALL_PARTICIPANTS, DEPARTMENT_ONLY, OUTSIDE_DEPARTMENTS_ONLY
```

### `ScoreSemesterPolicy`
```java
ACTIVITY_SEMESTER, EXPLICIT_SEMESTER
```

### `ScoreEntrySourceType`
```java
ACTIVITY_PARTICIPATION, ACTIVITY_REGISTRATION, TASK_SUBMISSION, TASK_ASSIGNMENT,
MINIGAME_ATTEMPT, SERIES_PROGRESS, SERIES_MINIMUM_REQUIREMENT, MANUAL_ADJUSTMENT, RECALCULATION
```

### `ActivityPresetCode`
```java
EVENT_BASIC, EVENT_WITH_SUBMISSION, ENTERPRISE_SEMINAR_BASIC,
ENTERPRISE_SEMINAR_WITH_BONUS, MINIGAME_PASS_ONLY, CUSTOM
```

### `SeriesPresetCode`
```java
SERIES_MILESTONE_BASIC, ENTERPRISE_SERIES, CUSTOM
```

### `MiniGameType`
```java
QUIZ
```

### `RegistrationStatus`
```java
PENDING, APPROVED, REJECTED, CANCELLED, ATTENDED, WAITLIST
```

### `ParticipationType`
```java
REGISTERED, CHECKED_IN, ATTENDED, COMPLETED
```

### `SubmissionStatus`
```java
SUBMITTED, GRADED, RETURNED, LATE, MISSING
```

---

*End of Backend Contract Audit Report*
