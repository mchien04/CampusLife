# Activity Score Refactor Plan

## Mục Tiêu

Refactor phần `Activity` và logic tính điểm để:

- Tách cấu hình điểm ra khỏi entity `Activity`.
- Hỗ trợ một activity có nhiều rule điểm, ví dụ chuyên đề doanh nghiệp vừa tính `CHUYEN_DE +1`, vừa tùy chọn cộng `REN_LUYEN` hoặc `CONG_TAC_XA_HOI`.
- Hỗ trợ ràng buộc theo khoa cho từng rule điểm.
- Giảm hard-code theo `ActivityType`.
- Có nguồn điểm rõ ràng bằng ledger `score_entries`.
- Giữ `StudentScore` như bảng tổng hợp/cache để phục vụ xem điểm, ranking, thống kê nhanh.
- Hỗ trợ chính sách tính điểm theo học kỳ hiện tại hoặc theo học kỳ của activity.
- Cho phép rule chấm bài nộp cộng điểm khi đạt và tùy chọn không trừ/trừ điểm khi không đạt.

## Hiện Trạng

`Activity` hiện đang vừa mô tả sự kiện vừa chứa một phần công thức điểm:

- `type`
- `scoreType`
- `maxPoints`
- `penaltyPointsIncomplete`
- `requiresSubmission`
- `organizers`

Các service đang tự cập nhật `StudentScore` trực tiếp:

- `ActivityRegistrationServiceImpl`: check-in/check-out, chuyên đề doanh nghiệp dual-score.
- `TaskSubmissionServiceImpl`: chấm submission rồi cập nhật participation và tổng điểm.
- `MiniGameServiceImpl`: pass quiz rồi tạo participation và cập nhật điểm.
- `ActivitySeriesServiceImpl`: milestone progress và milestone points.
- `ScoreServiceImpl`: recalculate từ participation và milestone.

Vấn đề chính:

- Công thức điểm rải ở nhiều nơi.
- `ActivityType.CHUYEN_DE_DOANH_NGHIEP` đang hard-code dual-score.
- `Activity.organizers` đang là khoa tổ chức, không nên dùng làm khoa được cộng điểm.
- `StudentScore` bị cập nhật trực tiếp từ nhiều flow, dễ lệch dữ liệu nếu có lỗi giữa chừng.
- `ScoreHistory` đang lưu diễn giải bằng text, khó truy vấn nguồn điểm chuẩn.

## Thiết Kế Mới

### 1. Activity Chỉ Là Metadata

`Activity` vẫn giữ các field mô tả sự kiện:

- `type`
- `name`
- `description`
- `startDate`
- `endDate`
- `requiresSubmission`
- `registrationStartDate`
- `registrationDeadline`
- `ticketQuantity`
- `requiresApproval`
- `mandatoryForFacultyStudents`
- `organizers`
- `seriesId`
- `seriesOrder`
- `isDraft`
- `isDeleted`

Các field điểm cũ sẽ được migrate nhanh sang rule rồi loại bỏ khỏi `Activity`, vì sản phẩm chưa chạy chính thức và không cần giữ dữ liệu legacy lâu:

- `scoreType`
- `maxPoints`
- `penaltyPointsIncomplete`

Migration nhanh:

- Trước khi drop cột, tạo rule tương ứng từ dữ liệu cũ.
- Sau khi backfill rule thành công, xóa field khỏi entity, request/response và migration drop cột trong DB.
- Code mới không đọc `Activity.scoreType`, `Activity.maxPoints`, `Activity.penaltyPointsIncomplete` nữa.

Các chức năng khác chủ yếu map qua `activity_id`, `registration_id`, `participation_id`, nên không bị ảnh hưởng lớn nếu response/API được cập nhật rõ. Phần cần kiểm tra kỹ là các màn hình/endpoint đang hiển thị `scoreType`, `maxPoints`, `penaltyPointsIncomplete` trực tiếp từ activity.

### 2. ActivityScoreRule

Tạo entity `ActivityScoreRule` để mô tả một rule cộng/trừ điểm.

```text
activity_score_rules
- id
- activity_id
- score_type
- trigger
- calculation
- points
- fail_points
- audience
- semester_policy
- enabled
- created_at
- updated_at
```

Ý nghĩa:

- `score_type`: loại điểm được ghi nhận, ví dụ `REN_LUYEN`, `CONG_TAC_XA_HOI`, `CHUYEN_DE`.
- `trigger`: thời điểm rule được áp dụng.
- `calculation`: cách tính điểm.
- `points`: số điểm của rule. Với `COUNT_COMPLETION`, mặc định có thể là `1`.
- `fail_points`: điểm áp dụng khi trigger là chấm bài không đạt hoặc rule cần nhánh fail. Giá trị có thể là `0`, số âm, hoặc số dương nếu nghiệp vụ cần.
- `audience`: đối tượng được áp dụng rule.
- `semester_policy`: chọn học kỳ ghi điểm.
- `enabled`: bật/tắt rule.

Enum đề xuất:

```java
public enum ScoreRuleTrigger {
    PARTICIPATION_COMPLETED,
    SUBMISSION_GRADED,
    MINIGAME_PASSED,
    SERIES_MILESTONE_REACHED
}
```

```java
public enum ScoreRuleCalculation {
    FIXED_POINTS,
    COUNT_COMPLETION,
    PASS_FAIL_POINTS,
    PENALTY_POINTS,
    SERIES_MILESTONE
}
```

```java
public enum ScoreRuleAudience {
    ALL_PARTICIPANTS,
    DEPARTMENT_ONLY,
    OUTSIDE_DEPARTMENTS_ONLY
}
```

```java
public enum ScoreSemesterPolicy {
    ACTIVITY_SEMESTER,
    CURRENT_OPEN_SEMESTER,
    EXPLICIT_SEMESTER
}
```

Mặc định đề xuất:

- Activity thường, chuyên đề, minigame: `ACTIVITY_SEMESTER`, dựa trên `activity.startDate`/`endDate`.
- Nếu nghiệp vụ yêu cầu "tính điểm vào học kỳ hiện tại" tại thời điểm chấm/check-out: dùng `CURRENT_OPEN_SEMESTER`.
- Trường hợp admin muốn ép học kỳ cụ thể: dùng `EXPLICIT_SEMESTER` và lưu thêm `semester_id` trên rule hoặc trên request áp dụng điểm.

Nếu muốn hỗ trợ cả "theo học kỳ hiện tại" lẫn "theo học kỳ activity" linh hoạt, nên để policy nằm trên từng rule, không hard-code toàn hệ thống.

### 3. ActivityScoreRuleDepartment

Tạo bảng trung gian để rule có danh sách khoa áp dụng riêng.

```text
activity_score_rule_departments
- rule_id
- department_id
```

Không dùng `activity_departments`/`organizers` cho điều kiện tính điểm, vì đó là quan hệ "khoa tổ chức".

### 4. ScoreEntry Ledger

Tạo ledger `score_entries` làm nguồn sự thật của điểm.

```text
score_entries
- id
- student_id
- semester_id
- score_type
- activity_id
- rule_id
- source_type
- source_id
- points
- status
- reason
- created_by_user_id
- created_at
- updated_at
```

Enum đề xuất:

```java
public enum ScoreEntrySourceType {
    ACTIVITY_PARTICIPATION,
    TASK_SUBMISSION,
    MINIGAME_ATTEMPT,
    SERIES_PROGRESS,
    MANUAL_ADJUSTMENT,
    RECALCULATION
}
```

```java
public enum ScoreEntryStatus {
    ACTIVE,
    REVERSED
}
```

Unique key đề xuất:

```text
unique(student_id, score_type, source_type, source_id, rule_id, status_active_marker)
```

Với MySQL có thể dùng cách thực dụng hơn:

```text
unique(student_id, score_type, source_type, source_id, rule_id)
```

Khi cần đảo điểm, update entry cũ thành `REVERSED` và tạo entry mới, hoặc update điểm theo cùng key nếu muốn idempotent đơn giản hơn. Giai đoạn đầu nên chọn idempotent upsert để giảm độ phức tạp.

### 5. StudentScore Là Aggregate Cache

`student_scores` vẫn giữ để:

- Xem điểm nhanh.
- Ranking.
- Thống kê.

Nhưng `StudentScore.score` sẽ được tính từ:

```text
SUM(score_entries.points)
WHERE student_id = ?
  AND semester_id = ?
  AND score_type = ?
  AND status = ACTIVE
```

Không service nghiệp vụ nào được cộng/trừ `StudentScore` trực tiếp nữa.

### 6. Semester Resolution

Tất cả điểm ghi vào `score_entries.semester_id` thông qua một service duy nhất, ví dụ `ScoreSemesterResolver`.

Method gợi ý:

```java
Semester resolveSemester(Activity activity, ActivityScoreRule rule, LocalDateTime eventTime);
```

Luồng resolve:

1. Nếu `rule.semesterPolicy = EXPLICIT_SEMESTER`, dùng `rule.semester`.
2. Nếu `rule.semesterPolicy = CURRENT_OPEN_SEMESTER`, dùng học kỳ đang mở tại thời điểm phát sinh event.
3. Nếu `rule.semesterPolicy = ACTIVITY_SEMESTER`, dùng `SemesterHelperService.getSemesterForActivity(activity)`.
4. Nếu không tìm được semester, trả lỗi rõ ràng hoặc fallback theo cấu hình hệ thống. Không nên âm thầm ghi sai học kỳ.

Khuyến nghị:

- Không để từng service tự gọi `SemesterRepository.findAll().stream().filter(Semester::isOpen)`.
- Không để `ScoreRuleEngine` tự suy luận rải rác; chỉ gọi `ScoreSemesterResolver`.
- `score_entries` phải lưu `semester_id` cố định tại thời điểm ghi điểm để ranking/history ổn định kể cả sau này sửa ngày activity.

## Ví Dụ Rule Cho Chuyên Đề Doanh Nghiệp

### Chỉ Khoa A, B Được Tính Điểm Chuyên Đề

```text
Rule 1:
activity_id = 100
score_type = CHUYEN_DE
trigger = PARTICIPATION_COMPLETED
calculation = COUNT_COMPLETION
points = 1
audience = DEPARTMENT_ONLY
departments = [A, B]
```

### Cộng Thêm Điểm Rèn Luyện Cho Tất Cả Sinh Viên

```text
Rule 2:
activity_id = 100
score_type = REN_LUYEN
trigger = PARTICIPATION_COMPLETED
calculation = FIXED_POINTS
points = 5
audience = ALL_PARTICIPANTS
departments = []
```

### Cộng Thêm Điểm CTXH Chỉ Cho Khoa A, B

```text
Rule 2:
activity_id = 100
score_type = CONG_TAC_XA_HOI
trigger = PARTICIPATION_COMPLETED
calculation = FIXED_POINTS
points = 3
audience = DEPARTMENT_ONLY
departments = [A, B]
```

Như vậy chuyên đề không còn bị khóa vào `REN_LUYEN`; rule phụ có thể là bất kỳ `ScoreType` nào.

### Chấm Bài Nộp Đạt/Không Đạt

```text
Rule:
activity_id = 101
score_type = REN_LUYEN
trigger = SUBMISSION_GRADED
calculation = PASS_FAIL_POINTS
points = 5
fail_points = 0
audience = ALL_PARTICIPANTS
semester_policy = ACTIVITY_SEMESTER
```

Nếu muốn không đạt bị trừ:

```text
points = 5
fail_points = -2
```

Nếu muốn không đạt vẫn được cộng một phần:

```text
points = 5
fail_points = 1
```

Như vậy không cần field `penaltyPointsIncomplete` trên `Activity`; rule tự biểu diễn đủ các trường hợp.

## Service Mới

### ActivityScoreRuleService

Trách nhiệm:

- Tạo/sửa/xóa rule điểm cho activity.
- Validate rule theo activity.
- Resolve danh sách khoa áp dụng.
- Trả rule trong `ActivityResponse`.

Method gợi ý:

```java
List<ActivityScoreRule> getEnabledRules(Long activityId, ScoreRuleTrigger trigger);
void replaceRules(Long activityId, List<ActivityScoreRuleRequest> requests);
```

### ScoreRuleEngine

Trách nhiệm:

- Nhận event nghiệp vụ: activity completed, submission graded, minigame passed, series milestone reached.
- Lấy rule phù hợp theo `activity_id + trigger`.
- Kiểm tra audience theo khoa sinh viên.
- Tính điểm.
- Gọi `ScoreEntryService` để upsert ledger entry.

Method gợi ý:

```java
void applyActivityCompleted(ActivityParticipation participation, User actor);
void applySubmissionGraded(TaskSubmission submission, User actor);
void applyMiniGamePassed(MiniGameAttempt attempt, User actor);
void applySeriesMilestone(StudentSeriesProgress progress, User actor);
```

Khi apply submission:

- Nếu submission đạt, dùng `rule.points`.
- Nếu submission không đạt, dùng `rule.failPoints`, mặc định `0` nếu không cấu hình.
- Nếu submission được chấm lại, upsert theo cùng `source_type + source_id + rule_id` để thay điểm cũ, sau đó refresh `StudentScore`.

### ScoreEntryService

Trách nhiệm:

- Tạo/upsert `score_entries`.
- Reverse entry khi participation/submission bị đổi trạng thái.
- Tính tổng theo `student + semester + scoreType`.
- Cập nhật `StudentScore`.
- Ghi `ScoreHistory` hoặc thay thế dần bằng view từ ledger.

Method gợi ý:

```java
ScoreEntry upsertEntry(ScoreEntryCommand command);
void reverseEntries(SourceType sourceType, Long sourceId, String reason, User actor);
void refreshStudentScore(Long studentId, Long semesterId, ScoreType scoreType);
void refreshStudentScores(Long studentId, Long semesterId);
```

`ScoreEntryCommand` nên chứa:

```java
public class ScoreEntryCommand {
    private Long studentId;
    private Long activityId;
    private Long ruleId;
    private Long semesterId;
    private ScoreType scoreType;
    private ScoreEntrySourceType sourceType;
    private Long sourceId;
    private BigDecimal points;
    private String reason;
    private User actor;
}
```

### ScoreAggregationService

Nếu muốn tách rõ hơn, `ScoreEntryService` chỉ xử lý ledger, còn `ScoreAggregationService` xử lý aggregate `StudentScore`.

```java
void recalculateFromLedger(Long studentId, Long semesterId, ScoreType scoreType);
void recalculateAllFromLedger(Long semesterId);
```

## Thay Đổi Trong Flow Hiện Tại

### Check-In / Check-Out

Hiện tại:

```text
check-out -> update participation -> update StudentScore trực tiếp
```

Sau refactor:

```text
check-out -> update participation COMPLETED
          -> ScoreRuleEngine.applyActivityCompleted(participation)
          -> score_entries upsert
          -> StudentScore refresh
```

### Grade Submission

Hiện tại:

```text
grade submission -> set participation points -> update StudentScore trực tiếp
```

Sau refactor:

```text
grade submission -> update submission + participation
                 -> ScoreRuleEngine.applySubmissionGraded(submission)
                 -> score_entries upsert/reverse
                 -> StudentScore refresh
```

Lưu ý: `gradeSubmission` cần bật lại `@Transactional`.

Chấm lại bài nộp không tạo cộng dồn. Cùng một submission và cùng một rule chỉ có một active score entry. Khi đổi từ đạt sang không đạt hoặc ngược lại, entry được update hoặc reverse/recreate theo policy đã chọn.

### MiniGame

Hiện tại:

```text
pass lần đầu -> tạo participation -> update StudentScore hoặc series progress
```

Sau refactor:

```text
pass lần đầu -> tạo participation/attempt
             -> ScoreRuleEngine.applyMiniGamePassed(attempt)
             -> score_entries upsert
             -> StudentScore refresh
```

### Activity Series

Hiện tại:

```text
update progress -> calculate milestone -> update StudentScore trực tiếp
```

Sau refactor:

```text
update progress -> calculate milestone progress
                -> ScoreRuleEngine.applySeriesMilestone(progress)
                -> score_entries upsert
                -> StudentScore refresh
```

## API/DTO Thay Đổi

### CreateActivityRequest

Thêm:

```java
private List<ActivityScoreRuleRequest> scoreRules;
```

DTO rule:

```java
public class ActivityScoreRuleRequest {
    private ScoreType scoreType;
    private ScoreRuleTrigger trigger;
    private ScoreRuleCalculation calculation;
    private BigDecimal points;
    private BigDecimal failPoints;
    private ScoreRuleAudience audience;
    private ScoreSemesterPolicy semesterPolicy;
    private Long explicitSemesterId;
    private List<Long> departmentIds;
    private Boolean enabled;
}
```

### ActivityResponse

Thêm:

```java
private List<ActivityScoreRuleResponse> scoreRules;
```

### Migration Nhanh Thay Vì Backward Compatibility Dài Hạn

Vì sản phẩm chưa chạy chính thức, không cần giữ song song logic cũ quá lâu. Ưu tiên:

- Tạo migration/routine chuyển `Activity.scoreType`, `Activity.maxPoints`, `Activity.penaltyPointsIncomplete` thành `activity_score_rules`.
- Cập nhật FE/API sang `scoreRules`.
- Drop field điểm cũ khỏi entity và DB.
- Xóa code đọc field điểm cũ trong service.

Mapping migration đề xuất:

- Activity thường: `scoreType + maxPoints -> FIXED_POINTS / PARTICIPATION_COMPLETED`.
- Chuyên đề doanh nghiệp:
  - `CHUYEN_DE + COUNT_COMPLETION + points = 1`.
  - Nếu `maxPoints != null`: thêm rule `REN_LUYEN + FIXED_POINTS + points = maxPoints`.
- Minigame: rule theo `MINIGAME_PASSED`.
- Series: rule theo `SERIES_MILESTONE_REACHED`.
- Submission:
  - Nếu `penaltyPointsIncomplete` cũ đang là null hoặc hệ thống hiện không trừ: `fail_points = 0`.
  - Nếu muốn giữ hành vi trừ cũ: `fail_points = -penaltyPointsIncomplete`.
  - Sau refactor, admin cấu hình trực tiếp `failPoints` trên rule.

## Migration Plan

### Phase 1: Thêm Schema Mới

- Tạo enum mới nếu dùng Java enum.
- Tạo bảng `activity_score_rules`.
- Tạo bảng `activity_score_rule_departments`.
- Tạo bảng `score_entries`.
- Thêm cột `fail_points`, `semester_policy`, `explicit_semester_id` vào `activity_score_rules`.
- Thêm index cho truy vấn tổng điểm:
  - `(student_id, semester_id, score_type, status)`
  - `(source_type, source_id)`
  - `(activity_id, rule_id)`

Chưa xóa cột cũ trong phase này để migration có nguồn đọc.

### Phase 2: Entity/Repository/DTO

- Thêm entity:
  - `ActivityScoreRule`
  - `ActivityScoreRuleDepartment` hoặc `@ManyToMany departments`
  - `ScoreEntry`
- Thêm repository:
  - `ActivityScoreRuleRepository`
  - `ScoreEntryRepository`
- Thêm DTO request/response cho score rules.

### Phase 3: Legacy Rule Backfill

Viết migration hoặc service backfill:

- Với mỗi `Activity` chưa có rule:
  - Tạo rule từ `scoreType`, `maxPoints`, `type`.
- Với chuyên đề doanh nghiệp:
  - Tạo rule `CHUYEN_DE COUNT_COMPLETION`.
  - Nếu `maxPoints > 0`, tạo rule `REN_LUYEN FIXED_POINTS`.

Sau khi backfill rule thành công:

- Drop `score_type`, `max_points`, `penalty_points_incomplete` khỏi `activities`.
- Xóa các field tương ứng khỏi `Activity`, `CreateActivityRequest`, `ActivityResponse`.
- Sửa các endpoint/màn hình đang đọc field cũ sang đọc `scoreRules`.

Vì sản phẩm chưa chạy chính thức, có thể bỏ phase chạy song song và chuyển hẳn sang rule mới ngay sau khi build/test pass.

### Phase 4: Ghi Ledger Cho Event Mới

Sửa các service nghiệp vụ để sau khi cập nhật trạng thái nguồn thì gọi `ScoreRuleEngine`.

Ưu tiên sửa theo thứ tự:

1. `ActivityRegistrationServiceImpl`
2. `TaskSubmissionServiceImpl`
3. `MiniGameServiceImpl`
4. `ActivitySeriesServiceImpl`

Không nên giữ code cập nhật `StudentScore` cũ nếu đã drop field điểm cũ. Nếu cần an toàn trong lúc dev, chỉ giữ trong branch riêng hoặc feature flag ngắn hạn.

### Phase 5: Recalculate Từ Ledger

Sửa `ScoreServiceImpl.recalculateStudentScore`:

- Không còn tự cộng participation + milestone bằng logic riêng.
- Rebuild `score_entries` từ nguồn nếu cần.
- Aggregate `StudentScore` từ ledger.
- Hỗ trợ lọc theo học kỳ hiện tại bằng `semesterId` explicit hoặc current open semester.

Tạo endpoint/admin job:

```text
POST /api/scores/rebuild-ledger?semesterId=...
POST /api/scores/recalculate-from-ledger?semesterId=...
```

### Phase 6: Backfill Score Entries Lịch Sử

Tạo job đọc dữ liệu cũ:

- `ActivityParticipation COMPLETED`
- `TaskSubmission GRADED`
- `MiniGameAttempt PASSED`
- `StudentSeriesProgress`

Sau đó sinh `score_entries` theo rule hiện tại/legacy rule.

Kiểm tra:

```text
SUM(score_entries) == student_scores.score
```

Nếu lệch, xuất report để review trước khi ghi đè.

Vì chưa production, có thể chọn cách đơn giản hơn:

- Clear `student_scores.score` về 0.
- Clear `score_entries`.
- Rebuild ledger từ nguồn hiện có.
- Recalculate toàn bộ `StudentScore` từ ledger.

### Phase 7: Dọn Logic Cũ

Sau khi ổn định:

- Xóa các helper cập nhật `StudentScore` trực tiếp trong service nghiệp vụ.
- `ScoreHistory` có thể:
  - Giữ như audit human-readable.
  - Hoặc chuyển dần sang đọc từ `score_entries`.
- Đánh dấu deprecated cho `Activity.scoreType`, `Activity.maxPoints`, `Activity.penaltyPointsIncomplete`.
- Nếu đã migration nhanh, xóa hẳn code/entity/DTO liên quan đến field điểm cũ thay vì deprecated.

## Tác Động Đến Flow

### Ít Ảnh Hưởng

Các flow không đổi về hành vi người dùng:

- Tạo activity.
- Đăng ký activity.
- Check-in/check-out.
- Nộp/chấm submission.
- Làm minigame.
- Xem điểm/ranking.

Lý do ít ảnh hưởng: đa số flow đang liên kết qua `activity_id`, `registration_id`, `participation_id`, `submission_id`, `attempt_id`. Việc đổi công thức điểm sang rule/ledger chủ yếu ảnh hưởng tầng tính điểm sau khi nguồn event đã phát sinh.

### Có Thay Đổi Nội Bộ

Các service không được tự cộng/trừ `StudentScore`.

Mọi thay đổi điểm phải đi qua:

```text
source event -> ScoreRuleEngine -> ScoreEntry -> StudentScore aggregate
```

## Rủi Ro

- Backfill ledger từ dữ liệu cũ có thể lệch vì logic cũ đã từng cập nhật `StudentScore` trực tiếp.
- Chuyên đề doanh nghiệp dual-score cũ cần migration cẩn thận để không mất điểm `CHUYEN_DE`.
- Series milestone hiện có logic không cộng dồn mốc; rule mới phải giữ behavior này.
- Nếu vẫn giữ cả logic cũ và mới trong một thời gian, có nguy cơ double-count. Vì chưa production, nên ưu tiên tắt hẳn direct update cũ khi ledger đã hoạt động.
- Drop field điểm cũ sẽ ảnh hưởng compile ở các nơi đang dùng `activity.getScoreType()`, `activity.getMaxPoints()`, `activity.getPenaltyPointsIncomplete()`. Cần xử lý toàn bộ usages trong một PR/refactor.
- Các API FE đang hiển thị field điểm cũ cần đổi sang hiển thị `scoreRules`.

## Test Checklist

### Unit Test

- Rule audience:
  - `ALL_PARTICIPANTS`
  - `DEPARTMENT_ONLY`
  - `OUTSIDE_DEPARTMENTS_ONLY`
- Rule calculation:
  - `FIXED_POINTS`
  - `COUNT_COMPLETION`
  - `PASS_FAIL_POINTS`
  - `PENALTY_POINTS`
  - `SERIES_MILESTONE`
- Idempotent upsert: cùng source không cộng trùng.
- Reverse/update entry khi chấm lại submission.
- Semester resolver:
  - `ACTIVITY_SEMESTER`
  - `CURRENT_OPEN_SEMESTER`
  - `EXPLICIT_SEMESTER`

### Integration Test

- Activity thường cộng một loại điểm.
- Chuyên đề doanh nghiệp:
  - Sinh viên khoa hợp lệ được `CHUYEN_DE +1`.
  - Sinh viên ngoài khoa không được `CHUYEN_DE`.
  - Rule phụ cộng `REN_LUYEN` cho tất cả.
  - Rule phụ cộng `CONG_TAC_XA_HOI` chỉ cho khoa hợp lệ.
- Submission đạt/không đạt.
  - Không đạt không trừ: `failPoints = 0`.
  - Không đạt bị trừ: `failPoints < 0`.
  - Không đạt vẫn cộng một phần: `failPoints > 0`.
  - Chấm lại không cộng dồn.
- Minigame pass lần đầu không cộng trùng khi làm lại.
- Series milestone không cộng dồn sai.
- Recalculate từ ledger khớp `StudentScore`.
- Điểm được ghi vào đúng học kỳ theo từng `semesterPolicy`.

### Regression Test

- Ranking theo `ScoreType`.
- Xem history điểm.
- Thống kê điểm.
- Export/report nếu có đọc `StudentScore`.

## Thứ Tự Triển Khai Đề Xuất

1. Thêm schema/entity/repository cho score rules và score entries.
2. Thêm `ScoreSemesterResolver`.
3. Thêm DTO rule vào create/update/get activity.
4. Backfill rule legacy từ dữ liệu activity cũ.
5. Drop field điểm cũ khỏi `activities`, `Activity`, request/response.
6. Implement `ScoreRuleEngine`, `ScoreEntryService`, `ScoreAggregationService`.
7. Sửa check-out activity thường/chuyên đề gọi engine.
8. Sửa submission, minigame, series gọi engine.
9. Sửa recalculate đọc ledger.
10. Rebuild score entries lịch sử nếu cần và recalculate `StudentScore`.
11. Xóa direct update cũ.
12. Cập nhật FE/API docs theo `scoreRules`.

## Quy Ước Quan Trọng

- `Activity.organizers` chỉ là đơn vị tổ chức.
- Khoa đủ điều kiện nhận điểm phải nằm trong `activity_score_rule_departments`.
- `Activity.type` không quyết định công thức điểm trực tiếp; nó chỉ dùng để gợi ý tạo rule mặc định hoặc hiển thị.
- `ScoreType` hiện có thể giữ enum. Nếu sau này cần admin tự tạo loại điểm, chuyển `ScoreType` sang bảng `score_types`.
- `StudentScore` là aggregate cache, không phải nguồn sự thật.
- `score_entries` là nguồn sự thật của điểm.
- Học kỳ ghi điểm phải được resolve một lần khi tạo `score_entries` và lưu cố định vào `semester_id`.
- Chấm bài không đạt mặc định không trừ nếu `failPoints` không cấu hình hoặc bằng `0`; trừ/cộng thêm là cấu hình rule, không hard-code trong submission service.
