# CampusLife Test Plan - Comprehensive API Testing

## Mục tiêu
Chạy test toàn diện cho 6 luồng kịch bản chính của hệ thống CampusLife.

## Phân tích hiện trạng test

### Test đã có (Service-level, Mockito)
1. `ActivityRegistrationServiceImplTest` - checkin, gradeCompletion, cancelRegistration
2. `ActivitySeriesServiceImplTest` - updateStudentProgress, calculateMilestonePoints, checkMinimumRequirement, createSeries
3. `MiniGameServiceImplTest` - submitAttempt, startAttempt, calculateScoreAndCreateParticipation
4. `ScoreRuleEngineImplTest` - applyActivityCompleted, applySubmissionGraded, applyMiniGamePassed, applyMiniGameExhaustedAttempts, applySeriesMilestone, applySeriesMinimumRequirement, audience filtering, failScoreType
5. `ScoreEntryServiceImplTest` - upsertEntry, reverseEntries
6. `ScorePresetServiceImplTest` - previewActivityPreset, getActivityPresetDefinitions, getSeriesPresetDefinitions
7. `TaskSubmissionServiceImplTest` - gradeSubmission, submitTask
8. `ReminderDispatchServiceTest`, `ReminderScheduleServiceImplTest`

### Test CÒN THIẾU
1. **Controller tests** (MockMvc) - Chưa có bất kỳ controller test nào
2. **Integration tests** - Chưa có
3. **Security/Authorization tests** - Chưa có

## 6 Luồng kịch bản test

### Luồng 1: CRUD Sự kiện + Cấu hình Score
- `POST /api/activities` - Tạo sự kiện
- `PUT /api/activities/{id}` - Cập nhật sự kiện
- `GET /api/activities/{id}` - Xem chi tiết
- `DELETE /api/activities/{id}` - Xóa sự kiện
- `PUT /api/activities/{id}/publish` - Publish
- `PUT /api/activities/{id}/unpublish` - Unpublish
- `POST /api/activities/{id}/copy` - Copy
- `POST /api/activities/standard` - Tạo standard activity
- `POST /api/activities/minigame` - Tạo minigame activity
- `GET /api/activities/presets` - Lấy preset định nghĩa
- `POST /api/activities/presets/preview` - Preview preset
- Score rules: `replaceRules`, `getRuleResponses`

### Luồng 2: CRUD Mini Game
- `POST /api/minigames` - Tạo mini game (quiz)
- `PUT /api/minigames/{id}` - Cập nhật mini game
- `GET /api/minigames/{id}` - Xem chi tiết
- `GET /api/minigames/{id}/questions` - Xem câu hỏi
- `POST /api/minigames/{id}/start` - Bắt đầu attempt
- `POST /api/minigames/attempts/{id}/submit` - Nộp bài

### Luồng 3: CRUD Series + Sự kiện con
- `POST /api/series` - Tạo series
- `PUT /api/series/{id}` - Cập nhật series
- `GET /api/series/{id}` - Xem chi tiết
- `DELETE /api/series/{id}` - Xóa series
- `POST /api/series/{id}/activities` - Tạo activity con trong series
- `PUT /api/series/{id}/activities/{activityId}` - Cập nhật activity con
- `GET /api/series/{id}/activities` - Lấy danh sách activity con
- `POST /api/series/{id}/register` - Đăng ký series
- `GET /api/series/{id}/progress/my` - Xem tiến độ
- `POST /api/series/{id}/students/{studentId}/calculate-milestone` - Tính điểm milestone

### Luồng 4: Các loại điểm được cộng trừ
- ScoreRuleEngine triggers:
  - `PARTICIPATION_COMPLETED`: +điểm khi hoàn thành activity
  - `SUBMISSION_GRADED`: +điểm/-điểm khi chấm bài nộp
  - `MINIGAME_PASSED`: +điểm khi vượt qua mini game
  - `MINIGAME_EXHAUSTED_ATTEMPTS`: -điểm khi hết lượt thử mini game
  - `SERIES_MILESTONE`: +điểm khi đạt milestone trong series
  - `SERIES_MINIMUM_REQUIREMENT`: -điểm phạt khi không đạt yêu cầu tối thiểu
  - `NO_SHOW`: -điểm khi không điểm danh
  - `TASK_OVERDUE`: -điểm khi nộp bài trễ hạn
  - `BONUS_POINTS`: +điểm thưởng
- ScoreEntryService: upsert, reverse
- Score history: `GET /api/scores/history/student/{id}`

### Luồng 5: Đăng ký tham gia, Nộp bài, QR Checkin/Checkout
- `POST /api/registrations` - Đăng ký tham gia
- `DELETE /api/registrations/activity/{id}` - Hủy đăng ký
- `GET /api/registrations/check/{activityId}` - Kiểm tra trạng thái
- `POST /api/registrations/checkin` - Checkin bằng ticket code
- `POST /api/registrations/checkin/qr` - Checkin bằng QR code
- `GET /api/registrations/checkin/validate` - Validate ticket code
- `POST /api/submissions/task/{taskId}` - Nộp bài
- `PUT /api/submissions/{id}` - Cập nhật bài nộp
- `PUT /api/submissions/{id}/grade` - Chấm điểm bài nộp
- `GET /api/submissions/task/{taskId}/my` - Xem bài nộp của mình
- `PUT /api/registrations/participations/{id}/grade` - Chấm điểm completion

### Luồng 6: Xem điểm + Lịch sử
- `GET /api/scores/student/{studentId}/semester/{semesterId}` - Xem điểm theo học kỳ
- `GET /api/scores/student/{studentId}/semester/{semesterId}/total` - Tổng điểm
- `GET /api/scores/ranking` - Bảng xếp hạng
- `GET /api/scores/history/student/{studentId}` - Lịch sử điểm chi tiết
- `POST /api/scores/recalculate/student/{studentId}` - Rà soát lại điểm
- `POST /api/scores/recalculate/all` - Rà soát tất cả
- `GET /api/scores/recalculate/status/{jobId}` - Trạng thái rà soát

## Chiến lược test

### Tầng 1: Service Unit Tests (Mockito)
- Đã có ~90% coverage cho các service chính
- Cần bổ sung: ActivityServiceImplTest, StandardActivityServiceImplTest, MinigameActivityServiceImplTest, ActivityScoreRuleServiceImplTest

### Tầng 2: Controller Tests (MockMvc + @WebMvcTest)
- Cần triển khai mới cho TẤT CẢ controllers
- Sử dụng `@WebMvcTest`, `@MockBean`, `@WithMockUser`
- Test: happy path, validation error, 404, 403, 401

### Tầng 3: Integration Tests (@SpringBootTest + H2)
- Smoke test cho các luồng end-to-end quan trọng

## Triển khai

### Stage 1: Tạo Controller Tests (MockMvc)
- `ActivityControllerTest` - CRUD sự kiện, publish, unpublish, copy
- `StandardActivityControllerTest` - Standard activity CRUD
- `MinigameActivityControllerTest` - Minigame activity CRUD
- `ActivitySeriesControllerTest` - Series CRUD + child activities + register + progress
- `MiniGameControllerTest` - Quiz CRUD + start/submit attempt
- `ActivityRegistrationControllerTest` - Registration + checkin + QR + cancel
- `TaskSubmissionControllerTest` - Submit + grade + update
- `ScoreControllerTest` - View scores + ranking + history + recalculate

### Stage 2: Bổ sung Service Tests
- `ActivityServiceImplTest` - create, update, delete, copy, publish logic
- `StandardActivityServiceImplTest`
- `MinigameActivityServiceImplTest`
- `ActivityScoreRuleServiceImplTest`

### Stage 3: Integration Tests
- `ActivityEndToEndTest` - Tạo activity -> Đăng ký -> Checkin -> Xem điểm
- `SeriesEndToEndTest` - Tạo series -> Tạo child activities -> Đăng ký -> Hoàn thành -> Tính milestone
- `MiniGameEndToEndTest` - Tạo quiz -> Start attempt -> Submit -> Xem điểm

### Stage 4: Chạy và phân tích
- `mvn test`
- Phân tích kết quả, coverage, failures

## Deliverables
- Các file test Java trong `src/test/java/...`
- Báo cáo kết quả test (PASS/FAIL, coverage, issues found)
