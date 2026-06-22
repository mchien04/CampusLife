# BE Score Flow Latest Report

## 1. Mục đích

Tài liệu này tóm tắt batch thay đổi mới nhất của backend theo `docs/refactor/BE_SCORE_FLOW_SPEC_V2.md`.

Mục tiêu:

- giúp nắm nhanh các thay đổi vừa implement
- chỉ ra tác động nghiệp vụ chính
- tổng hợp các file/service chính đã sửa
- rà lại `Phase 7` để biết test coverage hiện tại đã tới đâu

---

## 2. Tóm tắt nhanh

Batch mới nhất đã hoàn tất các phase sau:

- `Phase 1`: chuẩn hóa state machine attendance
- `Phase 2`: chuẩn hóa `EVENT_WITH_SUBMISSION`
- `Phase 3`: tách rõ optional task khi `requiresSubmission=false`
- `Phase 4`: thêm `MINIGAME_EXHAUSTED_ATTEMPTS`
- `Phase 5`: chuẩn hóa config `NO_SHOW`
- `Phase 6`: đồng bộ FE handoff docs

Kết quả chính:

- attendance không còn mơ hồ giữa `activity QR` và `ticket QR`
- `EVENT_WITH_SUBMISSION` chỉ `COMPLETED` khi `ATTENDED + GRADED`
- overdue của submission dùng `failPoints`, không suy từ điểm cộng event
- task optional không bị overdue/scoring
- minigame standalone có thể bị trừ điểm khi hết lượt mà không pass
- seminar mặc định không bật `NO_SHOW`
- tài liệu FE đã được cập nhật theo behavior mới

---

## 3. Thay đổi nghiệp vụ chính

### 3.1 Attendance

- `ticket QR`:
  - quét lần 1: `CHECKED_IN`
  - quét lần 2: `ATTENDED`
- `activity QR`:
  - quét xong xác nhận luôn `ATTENDED`
- `CHECKED_OUT` không còn là business target state mới

### 3.2 `EVENT_BASIC`

- `ATTENDED => COMPLETED`
- `check-in` một mình chưa đủ hoàn thành
- có thể bị `NO_SHOW` nếu đến cuối event không đạt `ATTENDED`

### 3.3 `EVENT_WITH_SUBMISSION`

- `COMPLETED = ATTENDED + GRADED`
- graded fail vẫn là completed
- nếu overdue sau khi đã attended:
  - điểm dùng `failPoints`
  - không dùng penalty suy từ `points` cộng của event
- submission không thay thế attendance

### 3.4 `requiresSubmission=false`

- vẫn có thể có task optional
- task optional:
  - không scoring
  - không overdue penalty
  - không gate completion

### 3.5 `MINIGAME`

- pass:
  - cộng điểm như cũ
- fail nhưng chưa hết lượt:
  - không trừ
- fail ở lượt cuối:
  - có thể trừ nếu có rule `MINIGAME_EXHAUSTED_ATTEMPTS`
- minigame trong series:
  - không dùng exhausted-attempt penalty

### 3.6 `NO_SHOW`

- event thường:
  - mặc định bật
- event có submission:
  - mặc định bật
- seminar:
  - mặc định tắt
  - nếu bật thì nên trừ sang score type khác, không trừ ngược `CHUYEN_DE` của buổi chính

### 3.7 `Series`

- activity con trong series chỉ là mốc
- không cộng điểm activity riêng
- penalty của series nằm ở `minimum requirement`

---

## 4. Thay đổi kỹ thuật theo phase

## 4.1 Phase 1: Attendance state machine

Đã làm:

- tách rõ `activity QR` và `ticket QR`
- chuẩn hóa `ATTENDED` là state hợp lệ cho attendance thật sự
- đồng bộ lại logic no-show/report theo state mới

File chính:

- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ReminderDispatchService.java`

## 4.2 Phase 2: `EVENT_WITH_SUBMISSION`

Đã làm:

- chỉ complete khi đủ `ATTENDED + GRADED`
- grading trước attendance không tự complete/score
- attendance sau khi đã graded sẽ finalize đúng

File chính:

- `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`
- `src/main/java/vn/campuslife/repository/TaskSubmissionRepository.java`
- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`

## 4.3 Phase 3: Optional task

Đã làm:

- chặn overdue/scoring cho task optional
- Quartz không schedule `TASK_OVERDUE` cho optional task
- dispatch cũng cancel nếu activity không thuộc diện overdue

File chính:

- `src/main/java/vn/campuslife/service/impl/ReminderScheduleServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ReminderDispatchService.java`
- `src/main/java/vn/campuslife/service/impl/ActivityTaskServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ActivityScoreRuleServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java`

## 4.4 Phase 4: Minigame exhausted attempts

Đã làm:

- thêm trigger `MINIGAME_EXHAUSTED_ATTEMPTS`
- thêm engine path apply penalty
- sửa thứ tự `resume IN_PROGRESS` trước khi check `maxAttempts`
- hỗ trợ preset cho penalty fail lượt cuối

File chính:

- `src/main/java/vn/campuslife/enumeration/ScoreRuleTrigger.java`
- `src/main/java/vn/campuslife/service/ScoreRuleEngine.java`
- `src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java`
- `src/main/java/vn/campuslife/service/impl/MiniGameServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ScorePresetServiceImpl.java`
- `src/main/java/vn/campuslife/repository/MiniGameAttemptRepository.java`

## 4.5 Phase 5: `NO_SHOW` config

Đã làm:

- thêm:
  - `noShowPenaltyEnabled`
  - `noShowPenaltyPoints`
  - `noShowPenaltyScoreType`
- preset `EVENT_BASIC` và `EVENT_WITH_SUBMISSION` mặc định sinh `NO_SHOW`
- preset seminar mặc định không sinh `NO_SHOW`
- validate `NO_SHOW` là penalty-style rule nên phải có `failPoints`

File chính:

- `src/main/java/vn/campuslife/model/activity/ActivityPresetConfig.java`
- `src/main/java/vn/campuslife/service/impl/ScorePresetServiceImpl.java`
- `src/main/java/vn/campuslife/service/impl/ActivityScoreRuleServiceImpl.java`

## 4.6 Phase 6: Handoff docs

Đã làm:

- cập nhật spec chính cho FE
- cập nhật spec delta từ usecase coverage đến hiện tại
- đồng bộ enum, state machine, preset config, minigame exhausted attempts, no-show, overdue

File chính:

- `docs/refactor/FE_BACKEND_HANDOFF_SPEC.md`
- `docs/refactor/FE_BACKEND_HANDOFF_SPEC_USECASE_TO_CURRENT.md`

---

## 5. File test đã cập nhật

- `src/test/java/vn/campuslife/service/impl/ActivityRegistrationServiceImplTest.java`
- `src/test/java/vn/campuslife/service/impl/TaskSubmissionServiceImplTest.java`
- `src/test/java/vn/campuslife/service/impl/ReminderDispatchServiceTest.java`
- `src/test/java/vn/campuslife/service/impl/MiniGameServiceImplTest.java`
- `src/test/java/vn/campuslife/service/impl/ScoreRuleEngineImplTest.java`
- `src/test/java/vn/campuslife/service/impl/ScorePresetServiceImplTest.java`

---

## 6. Kết quả verify

Đã chạy:

- `./mvnw.cmd -q compile`
- `./mvnw.cmd -q test`

Kết quả:

- compile pass
- full test pass

Lưu ý:

- vẫn có warning cũ của H2 về index `idx_is_correct`
- warning này chưa làm fail build ở batch hiện tại

---

## 7. Rà soát Phase 7

Theo spec, `Phase 7` cần khóa 7 nhóm behavior chính bằng test.

### 7.1 Ma trận coverage hiện tại

| Nhóm test bắt buộc | Trạng thái | Test hiện có | Ghi chú |
| --- | --- | --- | --- |
| event basic attendance | Done | `ActivityRegistrationServiceImplTest`, `ScoreRuleEngineImplTest` | Đã cover attendance/completion và apply activity scoring cho standalone |
| event with submission | Done | `TaskSubmissionServiceImplTest`, `ActivityRegistrationServiceImplTest`, `ScoreRuleEngineImplTest` | Đã cover `ATTENDED + GRADED`, graded trước attendance, fail/completed semantics |
| optional task no scoring | Done | `ReminderDispatchServiceTest` | Đã cover cancel overdue reminder cho optional task |
| seminar no-show default off | Done | `ScorePresetServiceImplTest` | Đã cover default preset không sinh `NO_SHOW`; chưa có test end-to-end dispatch nhưng đủ cho scope preset/config hiện tại |
| minigame exhausted attempts | Done | `MiniGameServiceImplTest`, `ScoreRuleEngineImplTest` | Đã cover standalone, in-series, final attempt |
| series child activity no individual scoring | Done | `TaskSubmissionServiceImplTest`, `ScoreRuleEngineImplTest` | Đã cover skip scoring và chỉ update progress cho series |
| 2 flow QR tách biệt | Done | `ActivityRegistrationServiceImplTest` | Đã cover ticket QR 2 bước, activity QR attendance nhanh, và standalone scoring behavior |

### 7.2 Đánh giá

`Phase 7` hiện đã ở mức:

- `Done` cho checklist regression cốt lõi của spec hiện tại
- các flow QR standalone quan trọng đã được khóa thêm bằng test

### 7.3 Kết luận Phase 7

- nếu xét theo mục tiêu regression cho batch hiện tại:
  - có thể coi `Phase 7` là **hoàn tất**
- phần còn lại chỉ là mở rộng test theo hướng nice-to-have hoặc dọn warning test log, không phải blocker của batch này

---

## 8. Kết luận

Batch mới nhất đã đưa backend sang một trạng thái nhất quán hơn ở 4 trục quan trọng:

- attendance
- submission/completion
- overdue/no-show
- minigame fail cuối lượt

Phần còn lại chủ yếu là:

- tăng độ chặt của regression test cho attendance standalone
- dọn warning H2 cũ nếu cần làm sạch test log
