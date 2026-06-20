# Báo Cáo Quản Lý Sự Kiện Hiện Tại

Tài liệu này tổng hợp phần quản lý sự kiện hiện tại của frontend CampusLife React, phục vụ đối chiếu với backend sau refactor phần tính điểm cho sự kiện.

Phạm vi gồm:

- Activity thường.
- Minigame gắn với activity.
- Series/chuỗi sự kiện.
- Activity thuộc series.
- API liên quan được dùng bởi `ADMIN`, `MANAGER`, `STUDENT`.
- Types/DTO liên quan trực tiếp đến activity, registration, participation, score, series và minigame.

## Nguồn Đọc Chính

- `src/services/eventAPI.ts`
- `src/services/registrationAPI.ts`
- `src/services/seriesAPI.ts`
- `src/services/minigameAPI.ts`
- `src/services/scoresAPI.ts`
- `src/services/taskAPI.ts`
- `src/services/activityPhotoAPI.ts`
- `src/types/activity.ts`
- `src/types/registration.ts`
- `src/types/series.ts`
- `src/types/minigame.ts`
- `src/types/score.ts`
- `src/types/task.ts`
- `src/App.tsx`

## Tổng Quan Module Sự Kiện

Frontend hiện xem `Activity` là thực thể trung tâm. Từ activity có thể mở rộng sang:

- Đăng ký/tham gia/check-in: `registrationAPI`.
- Điểm sự kiện: thông qua participation và score history.
- Minigame: activity có `type = MINIGAME`, quiz nằm ở `minigameAPI`.
- Series: activity thuộc series có `seriesId`, `seriesOrder`; nhiều thuộc tính tính điểm/đăng ký được kế thừa từ series.
- Task/submission: activity có các nhiệm vụ cho sinh viên.
- Photo gallery: activity có ảnh thư viện.

## Route Theo Role

### Student

| Route | Màn hình | API chính |
|---|---|---|
| `/student/events` | `StudentEvents` | `eventAPI.getEvents`, `registrationAPI.checkRegistrationStatus`, `registerForActivity`, `cancelRegistration`. |
| `/student/events/:id` | `StudentEventDetail` | `eventAPI.getEvent`, `minigameAPI.getMiniGameByActivity`, registration APIs. |
| `/student/series` | `StudentSeries` | `seriesAPI.getSeries`, `getMySeriesRegistrationStatus`, `getMySeriesProgress`, `registerForSeries`. |
| `/student/series/:id` | `StudentSeriesDetail` | `seriesAPI.getSeriesById`, `getSeriesActivities`, progress/registration status. |
| `/student/minigames` | `StudentMinigame` | `eventAPI.getEvents`, `minigameAPI.getMiniGameByActivity`, attempts. |
| `/student/minigames/:activityId/play` | `StudentMinigamePlay` | `getEvent`, `getMiniGameByActivity`, `getQuestions`, `startAttempt`, `submitAttempt`. |
| `/student/minigames/:activityId/history` | `StudentMinigameHistory` | `getMyAttempts`, `getAttemptDetail`. |
| `/student/registrations` | `StudentRegistrations` | `getMyRegistrations`, `getMyParticipations`, `cancelRegistration`. |
| `/student/participation-history` | `StudentParticipationHistory` | `getMyRegistrations`, `cancelRegistration`. |
| `/student/qr-checkin` | `QRCodeCheckIn` | `registrationAPI.checkInByQrCode`. |
| `/student/scores` | `ViewScores` | `scoresAPI.getSemesterScores`, `getScoreHistory`. |

### Manager/Admin

| Route | Màn hình | API chính |
|---|---|---|
| `/manager/events` | `EventList` | `eventAPI.getEvents`, `deleteEvent`, `copyActivity`. |
| `/manager/events/create` | `CreateEvent` | `eventAPI.createEvent`. |
| `/manager/events/:id` | `EventDetail` | `getEvent`, publish/unpublish/copy, registration, task, minigame, photo, check-in. |
| `/manager/events/:id/edit` | `EditEvent` | `getEvent`, `updateEvent`. |
| `/admin/events/:id/registrations` | `EventRegistrations` | `getEvent`, `getActivityRegistrations`, `updateRegistrationStatus`. |
| `/manager/registrations` | `ManagerRegistrations` | `getEvents`, `getActivityRegistrations`, ticket validation/check-in. |
| `/manager/series` | `SeriesManagement` | `seriesAPI.getSeries`, `deleteSeries`. |
| `/manager/series/create` | `CreateSeries` | `seriesAPI.createSeries`. |
| `/manager/series/:id` | `SeriesDetail` | `getSeriesById`, `getSeriesActivities`, create activity in series, overview/progress. |
| `/manager/series/:id/edit` | `EditSeries` | `getSeriesById`, `updateSeries`. |
| `/manager/minigames` | `MinigameManagement` | `getEvents`, `getMiniGameByActivity`, `deleteMiniGame`. |
| `/manager/minigames/create` | `CreateMinigameWizard` | Tạo activity `MINIGAME`, sau đó tạo quiz. |
| `/manager/minigames/create-quiz` | `CreateMinigame` | Tạo quiz cho activity có sẵn. |
| `/manager/minigames/edit/:miniGameId` | `EditQuiz` | `getQuestionsForEdit`, `updateMiniGame`. |
| `/manager/scores` | `ManagerScores` | `getStudentRanking`, `getScoreHistory`. |

## API Activity Thường

Service: `eventAPI`.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính | Ghi chú |
|---|---:|---|---|---|---|
| `getEvents` | GET | `/api/activities` | Student, Manager, Admin | `Response<ActivityResponse[]>` | Danh sách activity dùng ở hầu hết màn hình. |
| `getEvent` | GET | `/api/activities/{id}` | Student, Manager, Admin | `Response<ActivityResponse>` | Chi tiết activity. |
| `createEvent` | POST | `/api/activities` | Manager, Admin | `CreateActivityRequest` -> `ActivityResponse` | Tạo activity thường hoặc minigame activity qua wizard. |
| `updateEvent` | PUT | `/api/activities/{id}` | Manager, Admin | `Partial<CreateActivityRequest>` -> `ActivityResponse` | Sửa activity. |
| `publishActivity` | PUT | `/api/activities/{id}/publish` | Manager, Admin | `ActivityResponse` | Công bố activity. |
| `unpublishActivity` | PUT | `/api/activities/{id}/unpublish` | Manager, Admin | `ActivityResponse` | Gỡ công bố activity. |
| `copyActivity` | POST | `/api/activities/{id}/copy?offsetDays={n}` | Manager, Admin | `ActivityResponse` | Sao chép activity, offset ngày là optional. |
| `deleteEvent` | DELETE | `/api/activities/{id}` | Manager, Admin | `Response<void>` | Xóa activity. |
| `getEventsByDepartment` | GET | `/api/activities/department/{departmentId}` | Không thấy dùng trực tiếp trong page hiện tại | `ActivityResponse[]` | API có trong service. |
| `getEventsByScoreType` | GET | `/api/activities/score-type/{scoreType}` | Không thấy dùng trực tiếp trong page hiện tại | `ActivityResponse[]` | Liên quan lọc theo loại điểm. |
| `getEventsByMonth` | GET | `/api/activities/month?year=&month=` | Không thấy dùng trực tiếp trong page hiện tại | `ActivityResponse[]` | Lọc calendar/tháng. |
| `getMyEvents` | GET | `/api/activities/my` | Không thấy dùng trực tiếp trong page hiện tại | `ActivityResponse[]` | Activity của user hiện tại. |
| `registerForEvent` | POST | `/api/activities/{eventId}/register` | API cũ/ít dùng | `Response<void>` | Frontend hiện chủ yếu dùng `registrationAPI.registerForActivity`. |
| `unregisterFromEvent` | DELETE | `/api/activities/{eventId}/register` | API cũ/ít dùng | `Response<void>` | Frontend hiện chủ yếu dùng `registrationAPI.cancelRegistration`. |
| `getEventParticipants` | GET | `/api/activities/{eventId}/participants` | Không thấy dùng trực tiếp trong page hiện tại | `any[]` | Danh sách participant kiểu cũ. |
| `uploadBanner` | POST | `/api/activities/{eventId}/banner` | Không thấy dùng nhiều; form dùng `uploadAPI.uploadImage` | `FormData` -> `{ bannerUrl }` | Upload banner trực tiếp cho activity. |
| `debugUserInfo` | GET | `/api/activities/debug/user-info` | Debug | `any` | Nên tránh dùng production. |
| `backfillCheckInCodes` | POST | `/api/activities/backfill-checkin-codes` | Manager/Admin trong `EventDetail` | `{ updatedCount, totalActivities }` | Tạo bổ sung check-in code cho activity cũ. |

### Type: `CreateActivityRequest`

File: `src/types/activity.ts`.

Các field quan trọng:

- `name: string`
- `type: ActivityType`
- `scoreType: ScoreType`
- `description?: string`
- `startDate: string`
- `endDate: string`
- `requiresSubmission: boolean`
- `maxPoints?: string`
- `penaltyPointsIncomplete?: string`
- `registrationStartDate?: string`
- `registrationDeadline?: string`
- `shareLink?: string`
- `isImportant: boolean`
- `isDraft?: boolean`
- `bannerUrl?: string`
- `bannerFile?: File`
- `location: string`
- `ticketQuantity?: number`
- `benefits?: string`
- `requirements?: string`
- `contactInfo?: string`
- `requiresApproval?: boolean`
- `mandatoryForFacultyStudents: boolean`
- `organizerIds: number[]`

### Type: `ActivityResponse`

Các field quan trọng cho refactor điểm:

- `id: number`
- `name: string`
- `type: ActivityType | null`
- `scoreType: ScoreType | null`
- `requiresSubmission: boolean`
- `maxPoints?: string | null`
- `penaltyPointsIncomplete?: string`
- `registrationStartDate?: string | null`
- `registrationDeadline?: string | null`
- `ticketQuantity?: number | null`
- `requiresApproval: boolean`
- `mandatoryForFacultyStudents: boolean`
- `checkInCode?: string`
- `status?: string`
- `participantCount?: number`
- `seriesId?: number | null`
- `seriesOrder?: number | null`

Lưu ý quan trọng: activity thuộc series có thể có `type`, `scoreType`, `maxPoints`, `registrationStartDate`, `registrationDeadline`, `ticketQuantity` là `null`; các giá trị này được kế thừa từ series.

### Enum: `ActivityType`

- `SUKIEN`
- `MINIGAME`
- `CONG_TAC_XA_HOI`
- `CHUYEN_DE_DOANH_NGHIEP`

### Enum: `ScoreType`

- `REN_LUYEN`
- `CONG_TAC_XA_HOI`
- `CHUYEN_DE`

## API Đăng Ký, Tham Gia, Check-in

Service: `registrationAPI`.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính | Ghi chú |
|---|---:|---|---|---|---|
| `registerForActivity` | POST | `/api/registrations` | Student, Manager/Admin ở `EventDetail` khi tự đăng ký | `ActivityRegistrationRequest` -> `ActivityRegistrationResponse` | API chính để đăng ký activity. |
| `cancelRegistration` | DELETE | `/api/registrations/activity/{activityId}` | Student | `void` | Hủy đăng ký activity. |
| `getMyRegistrations` | GET | `/api/registrations/my` | Student | `ActivityRegistrationResponse[]` | Lịch sử/danh sách đăng ký. |
| `checkRegistrationStatus` | GET | `/api/registrations/check/{activityId}` | Student, Manager/Admin ở detail | `ActivityRegistrationResponse \| null` | Kiểm tra trạng thái đăng ký activity hiện tại. |
| `getMyParticipations` | GET | `/api/registrations/my/participations` | Student | `ActivityParticipationResponse[]` | Lịch sử tham gia/check-in/completed. |
| `getActivityRegistrations` | GET | `/api/registrations/activity/{activityId}` | Manager/Admin | `ActivityRegistrationResponse[]` | Danh sách đăng ký theo activity. |
| `updateRegistrationStatus` | PUT | `/api/registrations/{registrationId}/status?status={status}` | Manager/Admin | `RegistrationStatus` -> `ActivityRegistrationResponse` | Duyệt/từ chối/cập nhật trạng thái. |
| `getRegistrationById` | GET | `/api/registrations/{registrationId}` | Manager/Admin | `ActivityRegistrationResponse` | Lấy chi tiết registration. |
| `validateTicketCode` | GET | `/api/registrations/checkin/validate?ticketCode={code}` | Manager/Admin | `TicketCodeValidateResponse` | Kiểm tra vé trước check-in. |
| `checkIn` | POST | `/api/registrations/checkin` | Manager/Admin, một số màn detail | `{ ticketCode?, notes? }` -> `ActivityParticipationResponse` | Check-in bằng ticket code. |
| `checkInByQrCode` | POST | `/api/registrations/checkin/qr` | Student | `{ checkInCode }` -> `ActivityParticipationResponse` | QR check-in. |
| `getParticipationReport` | GET | `/api/registrations/activities/{activityId}/report` | Manager/Admin | Report body | Dùng trong `ApproveScoresForm`; liên quan duyệt điểm/tham gia. |

### Type: `ActivityRegistrationRequest`

- `activityId: number`
- `feedback?: string`

### Type: `ActivityRegistrationResponse`

- `id: number`
- `activityId: number`
- `activityName: string`
- `activityDescription?: string`
- `activityStartDate: string`
- `activityEndDate: string`
- `activityLocation?: string`
- `studentId: number`
- `studentName: string`
- `studentCode: string`
- `status: RegistrationStatus`
- `feedback?: string`
- `registeredDate: string`
- `createdAt: string`
- `ticketCode?: string`

### Enum: `RegistrationStatus`

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`
- `ATTENDED`

### Type: `ActivityParticipationResponse`

- `id: number`
- `activityId: number`
- `activityName: string`
- `studentId: number`
- `studentName: string`
- `studentCode: string`
- `participationType: ParticipationType`
- `pointsEarned?: number`
- `date: string`
- `notes?: string`

### Enum: `ParticipationType`

- `REGISTERED`
- `CHECKED_IN`
- `CHECKED_OUT`
- `ATTENDED`
- `COMPLETED`

### Type: `TicketCodeValidateResponse`

- `ticketCode: string`
- `studentId: number`
- `studentName: string`
- `studentCode: string`
- `activityId: number`
- `activityName: string`
- `currentStatus: RegistrationStatus`
- `canCheckIn: boolean`
- `canCheckOut: boolean`

## API Series / Chuỗi Sự Kiện

Service: `seriesAPI`.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính | Ghi chú |
|---|---:|---|---|---|---|
| `getSeries` | GET | `/api/series` | Student, Manager, Admin | `SeriesResponse[]` | Danh sách series. |
| `getSeriesById` | GET | `/api/series/{id}` | Student, Manager, Admin | `SeriesResponse` | Chi tiết series. |
| `getSeriesActivities` | GET | `/api/series/{seriesId}/activities` | Student, Manager/Admin | `ActivityResponse[]` | Activity trong series, sort theo `seriesOrder`. |
| `createSeries` | POST | `/api/series` | Manager/Admin | `CreateSeriesRequest` -> `SeriesResponse` | Tạo series. |
| `createActivityInSeries` | POST | `/api/series/{seriesId}/activities/create` | Manager/Admin | `CreateActivityInSeriesRequest` -> `ActivityResponse` | Tạo activity mới thuộc series. |
| `addActivityToSeries` | POST | `/api/series/{seriesId}/activities` | Manager/Admin | `AddActivityToSeriesRequest` -> `ActivityResponse` | Thêm activity có sẵn vào series. |
| `registerForSeries` | POST | `/api/series/{seriesId}/register` | Student | `SeriesRegistrationResponse` | Đăng ký series, response là mảng registration. |
| `getMySeriesRegistrationStatus` | GET | `/api/series/{seriesId}/registration/my` | Student | `SeriesRegistrationStatus` | Kiểm tra đã đăng ký series chưa. |
| `getMySeriesProgress` | GET | `/api/series/{seriesId}/progress/my` | Student | `StudentSeriesProgress` | Tiến độ và điểm milestone của sinh viên hiện tại. |
| `getStudentSeriesProgress` | GET | `/api/series/{seriesId}/students/{studentId}/progress` | Manager/Admin | `StudentSeriesProgress` | Tiến độ của một sinh viên. |
| `calculateMilestone` | POST | `/api/series/{seriesId}/students/{studentId}/calculate-milestone` | Admin/Manager theo service comment | `any` | Tính điểm milestone thủ công. |
| `updateSeries` | PUT | `/api/series/{id}` | Manager/Admin | `UpdateSeriesRequest` -> `SeriesResponse` | Cập nhật series. |
| `deleteSeries` | DELETE | `/api/series/{id}` | Manager/Admin | `null` | Soft delete series. |
| `getSeriesOverview` | GET | `/api/series/{seriesId}/overview` | Manager/Admin | `SeriesOverviewResponse` | Dashboard tổng quan series. |
| `getSeriesProgress` | GET | `/api/series/{seriesId}/progress?page=&size=&keyword=` | Manager/Admin | `SeriesProgressListResponse` | Danh sách tiến độ sinh viên trong series. |

### Type: `CreateSeriesRequest`

- `name: string`
- `description?: string`
- `milestonePoints: string`
- `scoreType: ScoreType`
- `mainActivityId?: number`
- `registrationStartDate?: string`
- `registrationDeadline?: string`
- `requiresApproval?: boolean`
- `ticketQuantity?: number`

`milestonePoints` là JSON string, ví dụ:

```json
{"3":5,"4":7,"5":10}
```

### Type: `SeriesResponse`

- `id: number`
- `name: string`
- `description?: string`
- `milestonePoints: string`
- `scoreType: ScoreType`
- `mainActivityId?: number`
- `registrationStartDate?: string`
- `registrationDeadline?: string`
- `requiresApproval: boolean`
- `ticketQuantity?: number`
- `createdAt: string`
- `activities?: ActivityResponse[]`
- `totalActivities?: number`
- `deleted?: boolean`

### Type: `CreateActivityInSeriesRequest`

- `name: string`
- `description?: string`
- `startDate?: string`
- `endDate?: string`
- `location?: string`
- `order?: number`
- `shareLink?: string`
- `bannerUrl?: string`
- `benefits?: string`
- `requirements?: string`
- `contactInfo?: string`
- `organizerIds?: number[]`
- `type?: "MINIGAME"`

Lưu ý: activity thuộc series không gửi `scoreType`, `maxPoints`, `registrationStartDate`, `registrationDeadline`, `ticketQuantity`, `requiresApproval`; các giá trị này được kế thừa từ series.

### Type: `StudentSeriesProgress`

- `id?: number`
- `studentId?: number`
- `seriesId?: number`
- `completedActivityIds?: string | number[]`
- `completedCount: number`
- `totalActivities?: number`
- `pointsEarned: string`
- `lastUpdated?: string`
- `currentMilestone?: number`
- `nextMilestoneCount?: number`
- `nextMilestonePoints?: string`
- `milestonePoints?: string | Record<number, number>`
- `scoreType?: ScoreType`

### Type: `SeriesOverviewResponse`

Các field quan trọng:

- `seriesId`
- `seriesName`
- `scoreType`
- `milestonePoints`
- `milestonePointsMap`
- `totalActivities`
- `totalRegisteredStudents`
- `totalCompletedStudents`
- `completionRate`
- `totalMilestonePointsAwarded`
- `milestoneProgress`
- `activityStats`

### Type: `SeriesProgressListResponse`

- `seriesId`
- `seriesName`
- `totalActivities`
- `totalRegistered`
- `progressList: SeriesProgressItemResponse[]`
- `page`
- `size`
- `totalPages`
- `totalElements`

`SeriesProgressItemResponse` có:

- `studentId`
- `studentCode`
- `studentName`
- `className?`
- `departmentName?`
- `completedCount`
- `totalActivities`
- `pointsEarned`
- `currentMilestone?`
- `completedActivityIds`
- `lastUpdated`
- `isRegistered`

## API Minigame

Service: `minigameAPI`.

Minigame hiện là quiz gắn với một activity. Activity có thể là:

- Activity minigame độc lập: tạo qua `/manager/minigames/create`, trước hết tạo activity `type = MINIGAME`, sau đó tạo quiz.
- Activity minigame thuộc series: tạo activity trong series với `type: "MINIGAME"`, sau đó tạo quiz cho activity đó.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính | Ghi chú |
|---|---:|---|---|---|---|
| `createMiniGame` | POST | `/api/minigames` | Manager/Admin | `CreateMiniGameRequest` -> `MiniGame` | Tạo quiz/minigame cho activity. |
| `checkActivityHasQuiz` | GET | `/api/minigames/activity/{activityId}/check` | Manager/Admin | `{ hasQuiz, miniGameId?, ... }` | Kiểm tra activity đã có quiz chưa. |
| `getMiniGameByActivity` | GET | `/api/minigames/activity/{activityId}` | Student, Manager/Admin | `MiniGame` | Lấy minigame theo activity. |
| `startAttempt` | POST | `/api/minigames/{miniGameId}/start` | Student | `StartAttemptResponse` | Bắt đầu làm quiz. |
| `submitAttempt` | POST | `/api/minigames/attempts/{attemptId}/submit` | Student | `SubmitAttemptRequest` -> `SubmitAttemptResponse` | Nộp quiz, có thể sinh participation và điểm. |
| `getMyAttempts` | GET | `/api/minigames/{miniGameId}/attempts/my` | Student | `MiniGameAttempt[]` | Lịch sử attempt của sinh viên hiện tại. |
| `getAllMiniGames` | GET | `/api/minigames` | Manager/Admin | `MiniGame[]` | Danh sách minigame. |
| `updateMiniGame` | PUT | `/api/minigames/{id}` | Manager/Admin | `UpdateMiniGameRequest` -> `MiniGame` | Cập nhật quiz. |
| `deleteMiniGame` | DELETE | `/api/minigames/{id}` | Manager/Admin | `void` | Xóa minigame. |
| `getQuestions` | GET | `/api/minigames/{miniGameId}/questions` | Student | `QuestionsResponse` | Lấy câu hỏi không kèm đáp án đúng. |
| `getAttemptDetail` | GET | `/api/minigames/attempts/{attemptId}` | Student, có xử lý 403/404 | `AttemptDetailResponse` | Chi tiết attempt, có đáp án đúng và đáp án chọn. |
| `getQuestionsForEdit` | GET | `/api/minigames/{miniGameId}/questions/edit` | Manager/Admin | `QuizQuestionsEditResponse` | Lấy câu hỏi có đáp án để sửa. |

### Type: `MiniGame`

- `id: number`
- `title: string`
- `description?: string`
- `questionCount: number`
- `timeLimit?: number`
- `isActive: boolean`
- `type: MiniGameType`
- `activityId: number`
- `requiredCorrectAnswers?: number`
- `rewardPoints?: string`
- `maxAttempts?: number | null`

### Type: `CreateMiniGameRequest`

- `activityId: number`
- `title: string`
- `description?: string`
- `questionCount: number`
- `timeLimit?: number`
- `requiredCorrectAnswers?: number`
- `rewardPoints?: string`
- `maxAttempts?: number | null`
- `questions: CreateQuestionRequest[]`

### Type: `SubmitAttemptResponse`

- `id: number`
- `status: string`
- `correctCount: number`
- `totalQuestions: number`
- `requiredCorrectAnswers?: number`
- `pointsEarned?: string`
- `participation?: { id, pointsEarned, isCompleted, participationType }`
- `attemptId?: number`
- `passed?: boolean`

Điểm minigame cần chú ý:

- `MiniGame.rewardPoints` là điểm cấu hình.
- `MiniGameAttempt.pointsEarned` là điểm attempt đã nhận.
- `SubmitAttemptResponse.participation.pointsEarned` xuất hiện khi `PASSED`.
- Score history có source `MINIGAME`.

## API Điểm Liên Quan Sự Kiện

Service: `scoresAPI`.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính | Ghi chú |
|---|---:|---|---|---|---|
| `calculateTrainingScore` | POST | `/api/scores/training/calculate?studentId=&semesterId=` | Tool cũ `TrainingScore` | `TrainingCalculateResponse` | Nhận body là `excludedCriterionIds`. |
| `getSemesterScores` | GET | `/api/scores/student/{studentId}/semester/{semesterId}` | Student, tool view | `ScoreViewResponse` | Tổng hợp điểm theo `ScoreType`. |
| `getStudentRanking` | GET | `/api/scores/ranking?semesterId=&scoreType=&departmentId=&classId=&sortOrder=` | Manager/Admin | `StudentRankingResponseData` | Ranking theo kỳ và loại điểm. |
| `getScoreHistory` | GET | `/api/scores/history/student/{studentId}?semesterId=&scoreType=&page=&size=` | Student, Manager/Admin | `ScoreHistoryViewResponse` | Lịch sử điểm và participation. |
| `getTotalScore` | GET | `/api/scores/student/{studentId}/semester/{semesterId}/total` | Không thấy dùng trực tiếp | `number` | Tổng điểm kỳ. |
| `listSemesterScores` | mock | Không gọi backend | Manager view cũ | mock | Deprecated, dùng `getStudentRanking`. |

### Type: `ScoreSourceType`

- `MANUAL`
- `ACTIVITY_CHECKIN`
- `ACTIVITY_SUBMISSION`
- `SERIES_MILESTONE`
- `MINIGAME`
- `CHUYEN_DE_COUNT`

### Type: `ScoreHistorySourceType`

- `ACTIVITY`
- `MINIGAME`
- `MILESTONE`
- `RECALCULATED`

### Type: `ScoreItem`

- `score: string`
- `sourceType: ScoreSourceType`
- `activityId?: number`
- `taskId?: number`
- `submissionId?: number`
- `sourceNote?: string`
- `criterionId?: number`

### Type: `ActivityParticipationDetailResponse`

- `id: number`
- `activityId: number | null`
- `activityName: string | null`
- `activityType: string | null`
- `seriesId: number | null`
- `seriesName: string | null`
- `pointsEarned: string`
- `participationType: string`
- `date: string`
- `isCompleted: boolean`
- `sourceType: 'ACTIVITY' | 'MINIGAME'`

### Type: `ScoreHistoryDetailResponse`

- `id`
- `oldScore`
- `newScore`
- `changeDate`
- `reason`
- `activityId`
- `activityName`
- `seriesId`
- `seriesName`
- `sourceType`
- `changedByUsername`
- `changedByFullName`

## API Task Liên Quan Activity

Service: `taskAPI`.

Task không phải core điểm activity thường, nhưng có liên quan `ACTIVITY_SUBMISSION` và `taskId/submissionId` trong `ScoreItem`.

| Method frontend | HTTP | Endpoint | Role dùng | Ghi chú |
|---|---:|---|---|---|
| `createTaskNew` | POST | `/api/tasks` | Manager/Admin | Tạo task theo activity. |
| `getTasksByActivity` | GET | `/api/tasks/activity/{activityId}` | Manager/Admin | Lấy task của activity. |
| `getTaskByIdNew` | GET | `/api/tasks/{taskId}` | Manager/Admin | Chi tiết task. |
| `updateTaskNew` | PUT | `/api/tasks/{taskId}` | Manager/Admin | Sửa task. |
| `deleteTaskNew` | DELETE | `/api/tasks/{taskId}` | Manager/Admin | Xóa task. |
| `assignTaskNew` | POST | `/api/tasks/assign` | Manager/Admin | Phân công task. |
| `getTaskAssignmentsNew` | GET | `/api/tasks/{taskId}/assignments` | Manager/Admin | Danh sách phân công. |
| `getStudentTasksNew` | GET | `/api/assignments/student/{studentId}` | Student | Task của sinh viên. |
| `updateTaskStatus` | PUT | `/api/assignments/{assignmentId}/status?status=` | Student/Manager | Cập nhật trạng thái assignment. |
| `removeTaskAssignment` | DELETE | `/api/assignments/{assignmentId}` | Manager/Admin | Hủy phân công. |
| `autoAssignMandatoryTasks` | POST | `/api/tasks/auto-assign/{activityId}` | Manager/Admin | Tự phân công task bắt buộc. |
| `getRegisteredStudentsForActivity` | GET | `/api/tasks/activity/{activityId}/registered-students` | Manager/Admin | Lấy sinh viên đã đăng ký để assign. |
| `assignTaskToRegisteredStudents` | POST | `/api/tasks/assign-to-registered/{activityId}?taskId=` | Manager/Admin | Assign task cho sinh viên đã đăng ký. |

## API Ảnh Activity

Service: `activityPhotoAPI`.

| Method frontend | HTTP | Endpoint | Role dùng | Type chính |
|---|---:|---|---|---|
| `uploadPhotos` | POST | `/api/activities/{activityId}/photos` | Manager/Admin | `FormData` -> `ActivityPhotoResponse[]` |
| `getActivityPhotos` | GET | `/api/activities/{activityId}/photos` | Student/Manager/Admin tùy màn detail | `ActivityPhotoResponse[]` |
| `deletePhoto` | DELETE | `/api/activities/{activityId}/photos/{photoId}` | Manager/Admin | `void` |
| `updatePhotoOrder` | PUT | `/api/activities/{activityId}/photos/{photoId}/order?order=` | Manager/Admin | `ActivityPhotoResponse` |

## Luồng Tạo Activity Thường

1. Manager/Admin vào `/manager/events/create`.
2. Form dùng `CreateActivityRequest`.
3. Nếu có ảnh banner, `BaseEventForm` upload qua `uploadAPI.uploadImage`, lấy `bannerUrl`.
4. Gọi `eventAPI.createEvent`.
5. Activity thường có `type`, `scoreType`, `maxPoints`, `penaltyPointsIncomplete`, registration fields và ticket fields riêng.
6. Sau khi tạo có thể publish, thêm task, bài viết, ảnh, check-in.

Field điểm cần chú ý:

- `scoreType`
- `requiresSubmission`
- `maxPoints`
- `penaltyPointsIncomplete`
- `mandatoryForFacultyStudents`
- `requiresApproval`

## Luồng Tạo Minigame Độc Lập

1. Manager/Admin vào `/manager/minigames/create`.
2. `CreateMinigameWizard` tạo activity với `type = MINIGAME` qua `eventAPI.createEvent`.
3. Sau đó tạo quiz qua `minigameAPI.createMiniGame`.
4. Điểm minigame không dùng `Activity.maxPoints`; điểm nằm ở `MiniGame.rewardPoints`.
5. Student đăng ký/check registration, lấy quiz, start attempt, submit attempt.
6. Khi submit pass, response có thể trả `participation` và `pointsEarned`.

Field điểm cần chú ý:

- `Activity.type = MINIGAME`
- `Activity.scoreType`
- `MiniGame.rewardPoints`
- `MiniGame.requiredCorrectAnswers`
- `MiniGame.maxAttempts`
- `SubmitAttemptResponse.pointsEarned`
- `SubmitAttemptResponse.participation.pointsEarned`

## Luồng Tạo Series

1. Manager/Admin vào `/manager/series/create`.
2. Gọi `seriesAPI.createSeries`.
3. Series giữ thông tin điểm/đăng ký cấp chuỗi:
   - `scoreType`
   - `milestonePoints`
   - `registrationStartDate`
   - `registrationDeadline`
   - `requiresApproval`
   - `ticketQuantity`
4. Student đăng ký series qua `registerForSeries`.
5. Tiến độ được tính qua completed activities trong series.
6. Điểm milestone nằm ở `StudentSeriesProgress.pointsEarned` và score history source `MILESTONE`/`SERIES_MILESTONE`.

## Luồng Tạo Activity Thuộc Series

1. Manager/Admin vào `/manager/series/:id`.
2. Gọi `seriesAPI.createActivityInSeries`.
3. Form dùng `CreateActivityInSeriesRequest`.
4. Activity thuộc series chỉ gửi thông tin cơ bản:
   - tên, mô tả, thời gian, địa điểm, thứ tự, banner, benefits, requirements, contactInfo, organizerIds.
5. Nếu là minigame thuộc series, request có `type: "MINIGAME"`.
6. Activity response có `seriesId`, `seriesOrder`.
7. Các field `type`, `scoreType`, `maxPoints`, registration date/deadline, ticketQuantity có thể `null` vì kế thừa từ series.

Điểm cần chú ý:

- Không lấy `scoreType` từ activity con nếu activity thuộc series; cần lấy từ series.
- Không lấy `maxPoints` từ activity con trong series nếu backend trả `null`.
- Điểm series dùng milestone, không giống điểm check-in activity thường.
- Minigame trong series vừa có `type = MINIGAME` ở request tạo activity, vừa có quiz riêng sau đó.

## Ma Trận Loại Sự Kiện Và Điểm

| Loại | Nơi cấu hình điểm | Nơi ghi nhận tham gia | Source score/history |
|---|---|---|---|
| Activity thường | `Activity.scoreType`, `Activity.maxPoints`, `penaltyPointsIncomplete` | `ActivityParticipationResponse` qua check-in/completed | `ACTIVITY`, `ACTIVITY_CHECKIN`, `ACTIVITY_SUBMISSION` |
| Minigame độc lập | `Activity.scoreType` + `MiniGame.rewardPoints` | `SubmitAttemptResponse.participation`, attempts | `MINIGAME` |
| Series | `Series.scoreType`, `Series.milestonePoints` | Completed activity IDs trong progress | `MILESTONE`, `SERIES_MILESTONE` |
| Activity thuộc series | Kế thừa từ series | Registration/participation theo activity con | Dùng series progress/milestone, activity con có `seriesId` |
| Minigame thuộc series | `Series.scoreType` + `MiniGame.rewardPoints` nếu backend thiết kế cộng riêng minigame | Attempt/participation của minigame activity con | Cần BE xác định ưu tiên: minigame riêng hay milestone series |

## Response Shape Cần Lưu Ý

Frontend hiện gặp nhiều dạng response:

- `{ status, message, body }`
- `{ status, message, data }`
- Một số service tự normalize sang `{ status, message, data }`
- `articleAPI` dùng `ApiResponse<T>` với field `body`
- `registrationAPI` nhiều method trả thẳng `response.data.body`

Khi backend refactor, cần giữ tương thích hoặc đồng bộ lại service normalize để tránh lỗi UI.

## Các Điểm Cần BE Xác Nhận Sau Refactor Tính Điểm

1. Activity thường khi check-in sẽ tạo participation với `pointsEarned` theo `maxPoints` hay chỉ khi `ParticipationType.COMPLETED`.
2. `requiresSubmission = true` thì điểm đến từ submission/task hay check-in vẫn có điểm?
3. `penaltyPointsIncomplete` được tính ở thời điểm nào và source history là gì?
4. Activity thuộc series có còn tạo điểm activity riêng không, hay chỉ đóng góp vào milestone?
5. Minigame thuộc series có cộng điểm `rewardPoints` riêng hay chỉ tính hoàn thành activity trong series?
6. `ScoreSourceType` frontend đang có `SERIES_MILESTONE`, còn score history dùng `MILESTONE`; backend nên thống nhất mapping.
7. `ActivityResponse.scoreType = null` cho activity thuộc series: các API điểm/history cần trả `seriesId`, `seriesName`, `scoreType` ở cấp series để UI hiển thị đúng.
8. `SubmitAttemptResponse.participation` cần ổn định shape vì UI dùng để biết pass và điểm nhận.
9. `ActivityParticipationResponse.pointsEarned` hiện là `number`, còn nhiều điểm khác là `string` BigDecimal; nên thống nhất hoặc frontend phải convert.
10. Registration status có `ATTENDED`, participation type có `COMPLETED`; cần quy ước rõ status nào kích hoạt điểm.

## Danh Sách File Frontend Đang Dùng API Liên Quan

### Activity/Event

- `src/pages/EventList.tsx`
- `src/pages/EventDetail.tsx`
- `src/pages/CreateEvent.tsx`
- `src/pages/EditEvent.tsx`
- `src/pages/StudentEvents.tsx`
- `src/pages/StudentEventDetail.tsx`
- `src/components/dashboard/StudentDashboard.tsx`
- `src/components/dashboard/ManagerDashboard.tsx`
- `src/pages/admin/EventRegistrations.tsx`
- `src/pages/ManagerRegistrations.tsx`

### Series

- `src/pages/admin/SeriesManagement.tsx`
- `src/pages/admin/CreateSeries.tsx`
- `src/pages/admin/EditSeries.tsx`
- `src/pages/admin/SeriesDetail.tsx`
- `src/pages/StudentSeries.tsx`
- `src/pages/StudentSeriesDetail.tsx`
- `src/components/series/*`

### Minigame

- `src/pages/admin/CreateMinigame.tsx`
- `src/pages/admin/CreateMinigameWizard.tsx`
- `src/pages/admin/EditQuiz.tsx`
- `src/pages/admin/MinigameManagement.tsx`
- `src/pages/StudentMinigame.tsx`
- `src/pages/StudentMinigamePlay.tsx`
- `src/pages/StudentMinigameHistory.tsx`
- `src/components/minigame/*`

### Score

- `src/pages/ViewScores.tsx`
- `src/pages/ManagerScores.tsx`
- `src/pages/TrainingScore.tsx`
- `src/components/registration/ApproveScoresForm.tsx`

### Task/Submission

- `src/pages/EventDetail.tsx`
- `src/pages/TaskManagement.tsx`
- `src/pages/admin/TaskManagement.tsx`
- `src/pages/StudentTasks.tsx`
- `src/pages/TaskSubmissions.tsx`
- `src/components/task/*`
- `src/components/tasks/*`

## Kết Luận Nhanh Cho BE

- Activity thường, minigame và series hiện dùng chung `ActivityResponse` nhưng cách tính điểm khác nhau.
- Activity thuộc series có nhiều field `null` và phải lấy rule từ `SeriesResponse`.
- Minigame có điểm riêng ở `MiniGame.rewardPoints` và attempt/participation khi pass.
- Series có điểm milestone ở `milestonePoints` và progress.
- Score history đang là nguồn quan trọng nhất để UI kiểm tra kết quả sau refactor.
- Nên thống nhất response shape và kiểu dữ liệu điểm (`string` BigDecimal vs `number`) để giảm lỗi hiển thị/tính toán ở FE.
