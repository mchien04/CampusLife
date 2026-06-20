# Báo Cáo Triển Khai Backend Refactor

## 1. Mục Đích Tài Liệu

Tài liệu này tổng hợp toàn bộ các thay đổi backend đã được triển khai trong conversation refactor gần đây, theo dạng báo cáo triển khai thực tế.

Mục tiêu của tài liệu:

- Tổng kết rõ backend đã làm gì qua 5 phase triển khai.
- Ghi nhận các thay đổi quan trọng về package, DTO, API, scoring engine và upload configuration.
- Tạo đầu vào đầy đủ để sau này viết tài liệu riêng cho frontend mà không phải rà lại toàn bộ lịch sử conversation.
- Xác định rõ các phần đã hoàn thành, các thay đổi contract cần FE nắm, và các điểm còn để mở cho các bước tiếp theo.

Lưu ý:

- Báo cáo này phản ánh trạng thái backend đã triển khai thực tế.
- Báo cáo này gom các phase trong plan gốc và các hạng mục phát sinh trong lúc implementation thành 5 phase báo cáo để dễ theo dõi.
- Backend được ưu tiên chỉnh theo kiến trúc mới; frontend sẽ adapt theo backend mới.

---

## 2. Bối Cảnh Refactor

### 2.1 Định Hướng Chốt

Trong quá trình làm việc, định hướng kỹ thuật đã được chốt như sau:

- Backend ưu tiên bám theo score engine mới.
- Không cố giữ tương thích ngược tối đa với frontend cũ nếu điều đó làm lệch kiến trúc mới.
- Frontend sẽ điều chỉnh theo backend sau khi backend ổn định.
- Chỉ gom lại `model` và sau đó mở rộng sang `controller`; không di chuyển `entity` và `service` theo domain package.

### 2.2 Kiến Trúc Điểm Mới Làm Gốc

Backend sau refactor sử dụng các nguyên tắc sau:

- `ActivityScoreRule` là cấu hình tính điểm cho từng activity/trigger.
- `ScoreRuleEngine` là nơi xử lý nghiệp vụ chấm điểm.
- `ScoreEntry` là ledger chuẩn, là nguồn dữ liệu điểm gốc.
- `StudentScore` là bảng tổng hợp/cache, không phải nguồn nghiệp vụ gốc.
- `ScoreHistory` không còn là nguồn diễn giải chính cho lịch sử điểm; lịch sử được chiếu từ `ScoreEntry`.

---

## 3. Tổng Quan 5 Phase

| Phase | Tên Phase | Trọng tâm |
|---|---|---|
| 1 | Activity Score Rules + Package DTO | Đồng bộ activity với scoreRules, gom model theo flow |
| 2 | DTO/API Standardization + Build Cleanup | Chuẩn hóa response DTO, dọn import/package consistency, compile clean |
| 3 | Series Milestone qua Engine | Đưa milestone series về đúng `ScoreRuleEngine` |
| 4 | MiniGame + Score History | Bỏ phụ thuộc runtime vào `rewardPoints`, chuẩn hóa lịch sử điểm từ ledger |
| 5 | Upload Configuration + Attachment + Controller Domainization | Gom cấu hình upload, mở rộng attachment bài nộp, gom toàn bộ controller theo domain |

---

## 4. Phase 1: Activity Score Rules + Package DTO

## 4.1 Mục Tiêu

- Đảm bảo Activity CRUD làm việc đúng với `scoreRules`.
- Tổ chức lại model/DTO theo domain flow để dễ theo dõi và phát triển tiếp.
- Tạo nền cho việc FE đọc response activity mới theo engine architecture.

## 4.2 Những Gì Đã Làm

### a. Persist `scoreRules` trong create/update/copy activity

Backend đã triển khai logic để:

- Lưu `scoreRules` khi tạo activity.
- Thay thế toàn bộ `scoreRules` khi update activity.
- Copy luôn `scoreRules` khi copy activity.

File chính:

- `src/main/java/vn/campuslife/service/impl/ActivityServiceImpl.java`

Kết quả:

- Activity không còn chỉ là metadata của sự kiện.
- Activity có thể trở thành “container nghiệp vụ” cho các rule tính điểm theo trigger.

### b. Map `scoreRules` vào `ActivityResponse`

Backend đã map đầy đủ danh sách rule vào response activity để frontend có thể đọc trực tiếp.

File chính:

- `src/main/java/vn/campuslife/model/activity/ActivityResponse.java`
- `src/main/java/vn/campuslife/service/impl/ActivityServiceImpl.java`

Kết quả:

- Các API activity trả về DTO có `scoreRules`.
- FE không cần suy diễn rule từ các field cũ như `maxPoints`, `penaltyPointsIncomplete`.

### c. Gom model/DTO theo flow

Các model đã được gom lại theo domain để dễ maintain:

- `model.activity`
- `model.activity.quiz`
- `model.activity.series`
- `model.activity.task`
- `model.score`

Các nhóm DTO đã được chuyển package:

#### Activity

- `CreateActivityRequest`
- `ActivityResponse`
- `ActivityRegistrationRequest`
- `ActivityRegistrationResponse`
- `ActivityParticipationRequest`
- `ActivityParticipationResponse`
- `ActivityParticipationDetailResponse`

#### Activity Quiz

- `CreateMiniGameRequest`
- `UpdateMiniGameRequest`
- `MiniGameResponse`
- `MiniGameAttemptResponse`
- `StartAttemptResponse`
- `SubmitAttemptResponse`
- `AttemptDetailResponse`
- `QuizQuestionsResponse`
- `QuizQuestionsEditResponse`
- `QuizQuestionResponse`
- `QuizQuestionEditResponse`
- `QuizQuestionDetailResponse`
- `QuizOptionResponse`
- `QuizOptionEditResponse`
- `QuizOptionDetailResponse`

#### Activity Series

- `SeriesOverviewResponse`
- `SeriesProgressItemResponse`
- `SeriesProgressListResponse`

#### Activity Task

- `CreateActivityTaskRequest`
- `ActivityTaskResponse`
- `TaskAssignmentRequest`
- `TaskAssignmentResponse`
- `TaskSubmissionResponse`

#### Score

- `ActivityScoreRuleRequest`
- `ActivityScoreRuleResponse`
- `ScoreEntryCommand`
- `ScoreHistoryDetailResponse`
- `ScoreHistoryViewResponse`
- `ScoreViewResponse`

## 4.3 Tác Động BE

- Model package rõ ràng hơn theo use case.
- Tầng API dễ đọc hơn vì DTO phản ánh đúng domain nghiệp vụ.
- Giảm độ lẫn giữa activity core, minigame, series, task và score.

## 4.4 Tác Động Đến FE

FE khi viết báo cáo riêng cần lưu ý:

- Activity-related DTO không còn nằm chung trong `model` phẳng.
- Response activity mới dựa vào `scoreRules`.
- Các flow `quiz`, `series`, `task` thuộc activity domain, nên tài liệu FE nên nhóm theo activity subflow thay vì coi là module rời rạc.

---

## 5. Phase 2: DTO/API Standardization + Build Cleanup

## 5.1 Mục Tiêu

- Chuẩn hóa các activity endpoint trả DTO thay vì trả entity trực tiếp.
- Dọn import/package consistency sau khi move model.
- Kiểm chứng refactor bằng compile clean.

## 5.2 Những Gì Đã Làm

### a. Chuẩn hóa activity endpoints trả `ActivityResponse`

Các endpoint activity trước đây trả `Activity` hoặc trả không đồng nhất đã được chuẩn hóa về DTO.

Các nhóm endpoint tiêu biểu:

- `GET /api/activities/score-type/{scoreType}`
- `GET /api/activities/department/{deptId}`
- `GET /api/activities/my`
- `GET /api/activities/upcoming`
- `GET /api/activities/month`

File chính:

- `src/main/java/vn/campuslife/controller/activity/ActivityController.java`
- `src/main/java/vn/campuslife/service/ActivityService.java`
- `src/main/java/vn/campuslife/service/impl/ActivityServiceImpl.java`

### b. Chuẩn hóa score history theo ledger

Lịch sử điểm không còn dựa vào parsing text từ `ScoreHistory.reason` như cũ.

Đã triển khai:

- Đọc từ `ScoreEntry`
- Tính running score theo ledger
- Trả `sourceType` trong `ScoreHistoryDetailResponse`

File chính:

- `src/main/java/vn/campuslife/repository/ScoreEntryRepository.java`
- `src/main/java/vn/campuslife/service/impl/ScoreServiceImpl.java`
- `src/main/java/vn/campuslife/model/score/ScoreHistoryDetailResponse.java`

### c. Semester policy được chỉnh đúng nghĩa nghiệp vụ

`CURRENT_OPEN_SEMESTER` đã được sửa để thực sự lấy học kỳ đang mở, thay vì suy theo ngày event.

File chính:

- `src/main/java/vn/campuslife/service/impl/ScoreSemesterResolverImpl.java`

### d. Cleanup import + package consistency

Sau khi move DTO/model, import đã được dọn lại ở các module đã chạm vào, gồm:

- activity
- score
- event article
- email
- student profile

Đồng thời đã xử lý các trường hợp import wildcard hoặc import sai class thực tế.

## 5.3 Kết Quả Xác Minh

- `.\mvnw.cmd compile` đã được chạy nhiều vòng để ổn định refactor.
- Các lỗi compile do import, package move, sai tên DTO đã được sửa xong.
- Diagnostics ở các file chính đã được kiểm tra sạch sau mỗi đợt chỉnh lớn.

## 5.4 Tác Động Đến FE

FE khi làm tài liệu hoặc adapter cần lưu ý:

- Không nên kỳ vọng BE trả JPA entity trực tiếp ở các activity list endpoint.
- Score history nên đọc theo `sourceType` và running balance từ ledger.
- `CURRENT_OPEN_SEMESTER` không còn là resolution theo ngày event.

---

## 6. Phase 3: Series Milestone qua Engine

## 6.1 Mục Tiêu

- Đưa logic milestone của series về đúng engine flow.
- Loại bỏ kiểu ghi ledger thủ công bên ngoài engine.

## 6.2 Những Gì Đã Làm

### a. Implement milestone scoring trong `ScoreRuleEngineImpl`

Backend đã bổ sung logic xử lý milestone của series theo nguyên tắc:

- Đọc `StudentSeriesProgress`
- Xác định milestone cao nhất đã đạt
- Ghi hoặc cập nhật `ScoreEntry` với `sourceType = SERIES_PROGRESS`
- Không cộng dồn mốc thấp hơn

File chính:

- `src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java`

### b. Chuyển `ActivitySeriesServiceImpl` sang gọi engine

Thay vì service tự ghi ledger, backend đã đổi sang:

- cập nhật tiến độ
- gọi `scoreRuleEngine.applySeriesMilestone(...)`

File chính:

- `src/main/java/vn/campuslife/service/impl/ActivitySeriesServiceImpl.java`

## 6.3 Quy Tắc Nghiệp Vụ Đã Chốt

- Series milestone chỉ lấy mốc cao nhất hiện đạt.
- Không cộng chồng các mốc.
- Điểm series được xem là một nguồn điểm engine-driven, không phải service-driven.

## 6.4 Tác Động Đến FE

FE cần hiểu:

- Điểm của series milestone là điểm riêng theo `SERIES_PROGRESS`.
- Các activity con trong series không mặc định cộng điểm riêng nếu business flow đang dùng milestone.
- UI history/ranking nên dựa vào ledger/sourceType thay vì suy diễn từ tên event.

---

## 7. Phase 4: MiniGame + Score History

## 7.1 Mục Tiêu

- Đưa mini game về đúng mô hình score rule.
- Bỏ phụ thuộc runtime vào `MiniGame.rewardPoints`.
- Chuẩn hóa response liên quan điểm và lịch sử điểm.

## 7.2 Những Gì Đã Làm

### a. Bỏ phụ thuộc runtime vào `rewardPoints`

Backend đã refactor để:

- Không dùng `MiniGame.rewardPoints` làm nguồn tính điểm runtime.
- Điểm minigame pass được lấy thông qua score rule và ledger.

File chính:

- `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`

### b. Chỉnh flow minigame pass

Đã triển khai:

- Standalone minigame: chấm qua `scoreRuleEngine.applyMiniGamePassed(...)`
- Minigame nằm trong series: không tự cộng điểm riêng, chỉ cập nhật tiến độ series

### c. Chỉnh DTO minigame

Đã loại `rewardPoints` khỏi request/response liên quan minigame ở tầng model.

Nhóm file chính:

- `src/main/java/vn/campuslife/model/activity/quiz/CreateMiniGameRequest.java`
- `src/main/java/vn/campuslife/model/activity/quiz/UpdateMiniGameRequest.java`
- `src/main/java/vn/campuslife/model/activity/quiz/MiniGameResponse.java`
- `src/main/java/vn/campuslife/model/activity/quiz/SubmitAttemptResponse.java`

### d. Chuẩn hóa điểm trả ra sau submit

`SubmitAttemptResponse` giờ nhận điểm từ ledger/resolved points thay vì từ field `rewardPoints`.

## 7.3 Điều Chưa Làm Trong Phase Này

Những phần sau chưa bị xóa vật lý hoàn toàn khỏi hệ thống dữ liệu:

- Field `MiniGame.rewardPoints` trong entity/database vẫn có thể còn tồn tại.
- Chưa triển khai migration DB để loại bỏ field cũ.

Điều này có nghĩa:

- Logic runtime đã chuyển sang kiến trúc mới.
- Cleanup schema vật lý nếu cần sẽ là bước tách riêng sau.

## 7.4 Tác Động Đến FE

FE cần lưu ý:

- Không dùng `rewardPoints` làm nguồn hiển thị/chấm điểm chính nữa.
- Điểm hiển thị sau khi pass minigame nên đọc từ response backend/ledger-driven flow.
- Với minigame nằm trong series, không nên giả định sẽ có điểm riêng ngay sau attempt.

---

## 8. Phase 5: Upload Configuration + Attachment + Controller Domainization

## 8.1 Mục Tiêu

- Gom cấu hình upload về một nơi để dễ đổi theo local/server.
- Chuẩn hóa lại các luồng lưu file/ảnh dùng chung storage config.
- Mở rộng bài nộp để hỗ trợ nộp file hoặc ảnh.
- Gom toàn bộ controller theo domain package.

## 8.2 Những Gì Đã Làm Về Upload

### a. Tạo cấu hình upload dùng chung

Đã tạo:

- `src/main/java/vn/campuslife/config/UploadProperties.java`

Mục đích:

- gom `upload dir`
- gom `public base url`
- gom các path con theo từng loại tài nguyên

### b. Tạo storage service dùng chung

Đã tạo:

- `src/main/java/vn/campuslife/service/UploadStorageService.java`
- `src/main/java/vn/campuslife/service/impl/UploadStorageServiceImpl.java`

Service này phụ trách:

- lưu file
- kiểm tra `imageOnly` khi cần
- tạo relative path
- convert relative path thành full public URL
- resolve path vật lý từ path đã lưu

### c. Chuẩn hóa cấu hình trong `application.properties`

Các key upload hiện tại:

```properties
app.upload.dir=${UPLOAD_DIR:uploads}
app.upload.public-url=${APP_BASE_URL:http://localhost:8080}
app.upload.paths.public-prefix=/uploads
app.upload.paths.general=
app.upload.paths.activity-photos=${UPLOAD_ACTIVITY_PHOTOS_DIR:activities}
app.upload.paths.submissions=${UPLOAD_SUBMISSIONS_DIR:submissions}
```

Ý nghĩa:

- `app.upload.dir`: root thư mục vật lý lưu file
- `app.upload.public-url`: base URL public để trả link file/ảnh
- `app.upload.paths.public-prefix`: prefix public mapping cho static resource
- `app.upload.paths.general`: thư mục con mặc định cho upload chung
- `app.upload.paths.activity-photos`: thư mục con cho ảnh sau sự kiện
- `app.upload.paths.submissions`: thư mục con cho file/ảnh bài nộp

Lợi ích:

- Dễ thay đổi khi chạy local.
- Dễ thay đổi khi deploy server.
- Không cần đi tìm hardcode ở nhiều service khác nhau.

### d. Chuyển các chỗ đang dùng `@Value("${app.upload...}")` sang config chung

Các service đã đổi sang `UploadProperties`:

- `ActivityServiceImpl`
- `ActivityRegistrationServiceImpl`
- `MiniGameServiceImpl`
- `StudentProfileServiceImpl`
- `EmailServiceImpl`
- `FileUploadServiceImpl`
- `ActivityPhotoServiceImpl`
- `TaskSubmissionServiceImpl`
- `WebConfig`

Kết quả:

- Toàn bộ cấu hình upload/public URL được thống nhất hơn.
- Giảm nguy cơ lệch path giữa các module.

## 8.3 Những Gì Đã Làm Về Attachment Bài Nộp

### a. Mở rộng API nộp bài

API bài nộp hiện hỗ trợ:

- `content`
- `files`
- `images`

Áp dụng cho:

- tạo bài nộp
- cập nhật bài nộp

File chính:

- `src/main/java/vn/campuslife/controller/activity/task/TaskSubmissionController.java`
- `src/main/java/vn/campuslife/service/TaskSubmissionService.java`
- `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`

### b. Tách response attachment

`TaskSubmissionResponse` đã được mở rộng với:

- `fileUrls` để giữ tương thích ngược
- `attachments` dạng typed response

Định dạng mới:

```json
{
  "fileUrls": [
    "http://localhost:8080/uploads/submissions/a.pdf",
    "http://localhost:8080/uploads/submissions/b.jpg"
  ],
  "attachments": [
    { "url": "http://localhost:8080/uploads/submissions/a.pdf", "type": "file" },
    { "url": "http://localhost:8080/uploads/submissions/b.jpg", "type": "image" }
  ]
}
```

File chính:

- `src/main/java/vn/campuslife/model/activity/task/TaskSubmissionResponse.java`
- `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`

### c. Chuẩn hóa upload ảnh sau sự kiện

Flow ảnh activity sau sự kiện đã chuyển sang storage/config chung.

File chính:

- `src/main/java/vn/campuslife/service/impl/ActivityPhotoServiceImpl.java`

### d. Chuẩn hóa upload ảnh chung

Flow `POST /api/upload/image` đã dùng storage/config chung.

File chính:

- `src/main/java/vn/campuslife/service/impl/FileUploadServiceImpl.java`
- `src/main/java/vn/campuslife/controller/internal/FileUploadController.java`

## 8.4 Những Gì Đã Làm Về Controller Package

Toàn bộ controller đã được gom theo domain:

```text
controller/
├─ academic/
├─ activity/
│  ├─ quiz/
│  ├─ series/
│  └─ task/
├─ article/
├─ auth/
├─ communication/
├─ internal/
├─ preparation/
├─ score/
└─ student/
```

### Cấu trúc controller hiện tại

#### academic

- `AcademicAdminController`
- `AcademicPublicController`
- `DepartmentAdminController`
- `DepartmentController`

#### activity

- `ActivityController`
- `ActivityParticipationController`
- `ActivityPhotoController`
- `ActivityRecommendationController`
- `ActivityRegistrationController`

##### activity.quiz

- `MiniGameController`

##### activity.series

- `ActivitySeriesController`

##### activity.task

- `ActivityTaskController`
- `TaskAssignmentController`
- `TaskSubmissionController`

#### article

- `EventArticleAdminController`
- `EventArticleController`

#### auth

- `AuthController`
- `UserManagementController`

#### communication

- `ChatbotController`
- `DeviceTokenController`
- `EmailController`
- `NotificationController`

#### internal

- `DebugController`
- `FileUploadController`
- `TestController`
- `TestFcmController`

#### preparation

- `PreparationController`
- `PreparationExportController`
- `PreparationFinanceController`

#### score

- `ScoreController`
- `StatisticsController`

#### student

- `AddressController`
- `StudentAccountManagementController`
- `StudentClassController`
- `StudentController`
- `StudentProfileController`

Lợi ích:

- Dễ định vị controller theo domain.
- Đồng bộ với model package đã refactor trước đó.
- Giảm tình trạng package controller phẳng.

## 8.5 Tác Động Đến FE

Các điểm FE nên ghi nhận:

- Bài nộp hiện có thể gửi file hoặc ảnh.
- Response bài nộp nên ưu tiên đọc `attachments` để render đúng kiểu.
- `fileUrls` vẫn tồn tại để hỗ trợ giai đoạn chuyển tiếp.
- URL ảnh/file trả về phụ thuộc `app.upload.public-url`.
- FE không nên hardcode đường dẫn vật lý hoặc giả định `/uploads` là duy nhất nếu môi trường deploy thay đổi config.

---

## 9. Danh Sách Thay Đổi Quan Trọng Cho Báo Cáo FE

Phần này được tách riêng để hỗ trợ viết tài liệu FE sau này.

## 9.1 API/DTO FE Cần Quan Tâm

### Activity

- Activity response đã chứa `scoreRules`
- Các activity list endpoint trả `ActivityResponse`

### Score

- Score history trả `sourceType`
- Dữ liệu điểm nên hiểu là ledger-driven

### MiniGame

- Không dựa vào `rewardPoints` runtime
- Điểm pass lấy từ rule/ledger

### Series

- Milestone ghi nhận bằng `SERIES_PROGRESS`
- Không giả định activity con luôn cộng điểm riêng

### Task Submission

- Request hỗ trợ `files` và `images`
- Response có `attachments = [{url, type}]`

### Upload/Image

- Public URL phụ thuộc `app.upload.public-url`
- Upload path đã được config hóa

## 9.2 Enum/Contract FE Nên Đồng Bộ Theo BE

Nguồn chuẩn hiện tại là backend.

Các enum/khái niệm FE nên bám:

- `ScoreRuleTrigger`
- `ScoreRuleCalculation`
- `ScoreRuleAudience`
- `ScoreSemesterPolicy`
- `ScoreEntrySourceType`

FE nên tránh duy trì các mapping cũ nếu không còn khớp với engine flow mới.

## 9.3 Những Chỗ FE Không Nên Dựa Theo Logic Cũ

- Không suy điểm từ `rewardPoints` của minigame.
- Không suy score history từ text lý do.
- Không giả định `CURRENT_OPEN_SEMESTER` chạy theo ngày event.
- Không giả định bài nộp chỉ có file thường.
- Không giả định package/controller cũ phản ánh domain hiện tại.

---

## 10. Các File/Module Nổi Bật Đã Thay Đổi

## 10.1 Service/Logic

- `service/impl/ActivityServiceImpl.java`
- `service/impl/ActivitySeriesServiceImpl.java`
- `service/impl/ScoreRuleEngineImpl.java`
- `service/impl/ScoreServiceImpl.java`
- `service/impl/MiniGameServiceImpl.java`
- `service/impl/TaskSubmissionServiceImpl.java`
- `service/impl/ActivityPhotoServiceImpl.java`
- `service/impl/FileUploadServiceImpl.java`
- `service/impl/StudentProfileServiceImpl.java`
- `service/impl/ActivityRegistrationServiceImpl.java`
- `service/impl/EmailServiceImpl.java`

## 10.2 Config

- `config/UploadProperties.java`
- `config/WebConfig.java`

## 10.3 Model

- `model/activity/...`
- `model/activity/quiz/...`
- `model/activity/series/...`
- `model/activity/task/...`
- `model/score/...`

## 10.4 Controller

- Toàn bộ controller đã được reorganize theo domain package

---

## 11. Những Kết Quả Đã Đạt Được

- Backend đã chuyển dịch rõ ràng sang engine-first score architecture.
- Activity, minigame, series, task và score đã liên kết tốt hơn theo cùng một flow nghiệp vụ.
- DTO/package và controller/package đã được tổ chức lại theo domain.
- Activity endpoints đã chuẩn hóa hơn theo DTO.
- Score history đã bám ledger.
- Series milestone đã đi qua engine.
- Minigame không còn phụ thuộc runtime vào `rewardPoints`.
- Upload path/public URL đã được gom về cấu hình tập trung.
- Bài nộp hiện hỗ trợ cả file lẫn ảnh với typed attachment response.
- Compile backend đã được xác minh xanh sau các đợt refactor chính.

---

## 12. Các Hạng Mục Chưa Làm Hoặc Có Thể Làm Sau

- Viết migration DB để xóa vật lý `MiniGame.rewardPoints` nếu team quyết định cleanup schema.
- Chuẩn hóa attachment DTO dùng chung giữa submission/email/các module upload khác.
- Nếu cần, tiếp tục siết security/rate-limit cho nhóm `internal` và `upload`.
- Viết tài liệu FE riêng dựa trên mục 9 của báo cáo này.

---

## 13. Khuyến Nghị Khi Viết Báo Cáo FE

Khi viết tài liệu dành cho frontend, nên tổ chức theo các nhóm sau:

1. Activity & Score Rules
2. Series & Milestone
3. MiniGame Scoring
4. Task Submission Attachments
5. Upload URL/Path Configuration
6. Score History & Source Types

Trình bày FE nên tập trung vào:

- Endpoint nào đổi response
- DTO nào thêm/bỏ field
- Enum nào là nguồn chuẩn
- Flow nào không còn đúng như frontend cũ
- Các giả định cũ nào cần bỏ

---

## 14. File Báo Cáo Liên Quan

- `docs/refactor/BE_REFACTOR_PLAN.md`
- `docs/refactor/score_system_report.md`
- `docs/refactor/EVENT_MANAGEMENT_API_REPORT.md`

---

## 15. Kết Luận

Qua conversation này, backend đã hoàn thành phần lớn các thay đổi cốt lõi để chuyển từ cách làm cũ sang kiến trúc score engine mới, đồng thời mở rộng thêm các hạng mục thực dụng phục vụ triển khai thật như:

- gom package theo domain
- chuẩn hóa DTO/API
- chuẩn hóa upload config
- hỗ trợ attachment typed cho bài nộp
- gom toàn bộ controller, bao gồm cả chatbot

Tài liệu này có thể được dùng làm mốc chốt backend implementation và làm nguồn đầu vào trực tiếp để viết báo cáo adaptation cho frontend ở bước tiếp theo.
