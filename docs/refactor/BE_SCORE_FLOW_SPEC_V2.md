# BE Score Flow Spec V2

## 1. Mục đích

Tài liệu này chốt lại nghiệp vụ và kế hoạch phát triển cho các flow tính điểm liên quan tới:

- `EVENT_BASIC`
- `EVENT_WITH_SUBMISSION`
- `ENTERPRISE_SEMINAR_BASIC`
- `ENTERPRISE_SEMINAR_WITH_BONUS`
- `MINIGAME_PASS_ONLY`
- `CUSTOM`
- `series milestone`
- `series minimum requirement`
- `no-show`
- `task overdue`
- `2 dạng QR attendance`

Đây là tài liệu chuẩn để tiếp tục phát triển backend theo hướng thống nhất state machine và scoring behavior.

---

## 2. Các quyết định nghiệp vụ đã chốt

### 2.1 `EVENT_WITH_SUBMISSION`

- `COMPLETED` vẫn được ghi nhận khi bài nộp đã được chấm, kể cả trường hợp fail.
- Lý do:
  - backend có hỗ trợ `failPoints`
  - `failPoints` có thể là `0` hoặc giá trị khác
  - vì vậy fail không đồng nghĩa với chưa hoàn thành nghiệp vụ
- Kết luận:
  - với `EVENT_WITH_SUBMISSION`, `COMPLETED` nghĩa là:
    - đã `ATTENDED`
    - và submission đã `GRADED`
  - điểm cuối cùng có thể:
    - cộng điểm
    - 0 điểm
    - hoặc điểm fail theo cấu hình
  - nếu submission bị `OVERDUE` sau khi đã `ATTENDED`:
    - điểm trừ phải bám theo `failPoints`
    - không lấy penalty từ `points` cộng của sự kiện

### 2.2 `requiresSubmission=false`

- Activity vẫn có thể có task optional.
- Tuy nhiên task optional này:
  - không tham gia scoring
  - không tham gia overdue penalty
- Kết luận:
  - `requiresSubmission=false` nghĩa là completion của activity không bị gate bởi task/submission
  - task nếu tồn tại chỉ là optional support material hoặc optional assignment

### 2.3 `ENTERPRISE_SEMINAR`

- Mặc định `no-show penalty = off`.
- Nếu admin bật no-show penalty thì được phép trừ sang score type khác.
- Không dùng no-show để trừ ngược vào phần tích lũy `CHUYEN_DE` của chính buổi seminar đó.

### 2.4 `MINIGAME exhausted attempts` trong `series`

- `off`
- không cộng/trừ điểm event riêng
- không tác động milestone

### 2.5 Hai kiểu QR attendance

Hiện có hai luồng QR khác nhau, phải giữ tách biệt về nghiệp vụ:

#### Loại 1: QR của activity

- Nguồn: `Activity.checkInCode`
- Ban tổ chức mở QR của activity cho sinh viên quét
- Sinh viên tự quét
- Trạng thái trước khi quét:
  - có thể chưa có participation
  - có thể đang ở `CHECKED_IN`
- Sau khi quét:
  - hệ thống ghi nhận `đã tham gia`
  - về state machine mới, QR activity là hành động xác nhận `ATTENDED`

#### Loại 2: QR từ ticket của sinh viên đăng ký

- Nguồn: `ActivityRegistration.ticketCode`
- Ban tổ chức quét ticket của từng sinh viên
- Có thể dùng để cập nhật trạng thái qua từng lần quét
- Đây là flow hỗ trợ stateful attendance:
  - quét lần 1: `CHECKED_IN`
  - quét lần 2: `ATTENDED`
  - có thể mở rộng logic nếu cần

Kết luận:

- `activity QR` là QR xác nhận tham gia nhanh
- `ticket QR` là QR điều khiển transition theo từng sinh viên
- Hai flow không được gộp chung một logic nội bộ

---

## 3. Định nghĩa state chuẩn

## 3.1 Registration state

`RegistrationStatus`

- `PENDING`
- `APPROVED`
- `REJECTED`
- `CANCELLED`
- `ATTENDED`
- `WAITLIST`

Ý nghĩa:

- `APPROVED`: đủ điều kiện tham gia
- `ATTENDED`: đã tham gia thực tế

## 3.2 Participation state

`ParticipationType`

- `REGISTERED`
- `CHECKED_IN`
- `ATTENDED`
- `COMPLETED`

Ghi chú:

- `CHECKED_OUT` không nên là state bền chính trong spec mới.
- Nếu vẫn giữ enum để tương thích code cũ, chỉ coi đây là transitional/internal state, không dùng làm business target state.

## 3.3 State semantics

- `REGISTERED`: đã có registration hợp lệ và participation record
- `CHECKED_IN`: đã đến địa điểm / đã được ghi nhận hiện diện bước đầu
- `ATTENDED`: đã đủ điều kiện xác nhận tham dự sự kiện
- `COMPLETED`: đã hoàn thành đầy đủ nghiệp vụ của activity

---

## 4. Rule theo loại activity

## 4.1 `EVENT_BASIC`

### Nghiệp vụ

- Không yêu cầu submission để hoàn thành.
- `COMPLETED` khi `ATTENDED`.

### Attendance

- `check-in` chưa đủ để hoàn thành.
- `check-in` rồi bỏ về giữa chừng:
  - vẫn có thể chỉ ở `CHECKED_IN`
  - chưa được coi là `ATTENDED`
- `ticket QR`:
  - quét lần 1 có thể là `CHECKED_IN`
  - quét lần 2 xác nhận `ATTENDED`
- `activity QR`:
  - quét xong xác nhận luôn `ATTENDED`

### Completion

- `ATTENDED => COMPLETED`

### Scoring

- dùng trigger `PARTICIPATION_COMPLETED`
- chỉ áp cho activity standalone

### No-show

- nếu registration `APPROVED` nhưng đến cuối event không đạt `ATTENDED`
  - có thể áp `NO_SHOW`

---

## 4.2 `EVENT_WITH_SUBMISSION`

### Nghiệp vụ

- Có yêu cầu submission để hoàn tất nghiệp vụ.
- `ATTENDED` và `COMPLETED` là hai tầng khác nhau.

### Attendance

- muốn hoàn thành thì trước hết phải `ATTENDED`
- `ATTENDED` được xác nhận qua:
  - `activity QR`
  - hoặc flow `ticket QR/manual attendance`

### Submission

- cho phép nộp trước hoặc sau attendance
- nhưng chỉ khi đủ cả hai điều kiện mới `COMPLETED`:
  - `ATTENDED`
  - `submission.status = GRADED`

### Completion

- `ATTENDED + GRADED => COMPLETED`
- trường hợp graded fail:
  - vẫn là `COMPLETED`
  - điểm dùng `failPoints`

### Các case bắt buộc hỗ trợ

- `ATTENDED + chưa nộp`
  - không `COMPLETED`
  - không `NO_SHOW`
- `ATTENDED + đã nộp + chưa chấm`
  - chưa `COMPLETED`
  - không `NO_SHOW`
- `ATTENDED + graded pass`
  - `COMPLETED`
  - cộng điểm pass
- `ATTENDED + graded fail`
  - `COMPLETED`
  - cộng `failPoints` hoặc `0`
- `không ATTENDED + có nộp`
  - không `COMPLETED`
  - vẫn có thể `NO_SHOW`

### Scoring

- dùng trigger `SUBMISSION_GRADED`
- chỉ áp cho activity standalone

### Overdue

- chỉ áp nếu `requiresSubmission=true`
- nếu activity đã `ATTENDED` nhưng submission bị overdue:
  - overdue sẽ dùng `failPoints` như kết quả fail của submission
  - không dùng `points` cộng của event để suy ra penalty
- nếu `failPoints = 0`:
  - overdue có thể cho kết quả `0 điểm`
  - nhưng vẫn là một trạng thái fail/completed theo nghiệp vụ nếu submission sau đó được chấm

---

## 4.3 `ENTERPRISE_SEMINAR_BASIC`

### Nghiệp vụ

- Là activity tích lũy chuyên đề.
- Completion theo attendance.

### Completion

- `ATTENDED => COMPLETED`

### Scoring

- cộng vào score type seminar chính, thường là `CHUYEN_DE`

### No-show

- mặc định `off`
- nếu bật penalty:
  - phải được cấu hình trừ sang score type khác
  - không trừ vào chính phần tích lũy seminar của buổi đó

---

## 4.4 `ENTERPRISE_SEMINAR_WITH_BONUS`

### Nghiệp vụ

- Giống seminar basic nhưng có thêm bonus rule.

### Completion

- `ATTENDED => COMPLETED`

### Scoring

- rule chính cho seminar
- rule bonus riêng

### No-show

- mặc định `off`
- nếu bật, penalty phải đi vào score type khác

---

## 4.5 `MINIGAME_PASS_ONLY`

### Nghiệp vụ

- Pass mới cộng điểm.
- Fail bình thường không bị phạt ngay.

### Scoring

- `PASSED => apply MINIGAME_PASSED`

### New rule mở rộng

- thêm case `exhausted attempts but never passed`
- rule mới chỉ áp cho activity standalone

### Series

- nếu minigame nằm trong `series`
  - không cộng điểm event riêng
  - không phạt exhausted attempts
  - không tác động milestone khi fail

---

## 4.6 `CUSTOM`

### Nghiệp vụ

- Admin được tự cấu hình rule.

### Constraint

- vẫn phải qua validation tương thích theo activity type
- không được phá các nguyên tắc:
  - `requiresSubmission=false` thì không có scoring/overdue theo submission
  - `MINIGAME` không dùng trigger event submission thường
  - activity trong `series` không cộng/trừ điểm event riêng

---

## 5. Quy tắc `requiresSubmission`

## 5.1 Khi `requiresSubmission=true`

- activity completion phụ thuộc vào submission flow
- allowed:
  - `SUBMISSION_GRADED`
  - `TASK_OVERDUE`
- `COMPLETED` khi:
  - `ATTENDED`
  - và submission đã `GRADED`
- quy tắc scoring cho overdue:
  - `TASK_OVERDUE` phải dùng `failPoints` của flow submission
  - không dùng `points` cộng của activity làm penalty mặc định
- ý nghĩa nghiệp vụ:
  - overdue của submission là một dạng fail result của bài nộp
  - không phải penalty độc lập lấy từ participation points

## 5.2 Khi `requiresSubmission=false`

- activity completion không phụ thuộc task
- task có thể tồn tại nhưng là optional
- task optional:
  - không scoring
  - không overdue penalty
- backend cần enforce:
  - không cho tạo submission-based scoring rule
  - không cho assign overdue penalty rule cho activity này

---

## 6. Quy tắc `NO_SHOW`

## 6.1 Điều kiện no-show

- registration đã `APPROVED`
- đến hết thời điểm event + grace
- chưa đạt `ATTENDED`

## 6.2 Không bị no-show nếu

- đã `ATTENDED`
- hoặc đã có attendance hợp lệ theo flow ticket/activity QR

## 6.3 Không phụ thuộc submission

- submission không thay thế attendance
- trường hợp có nộp bài nhưng không tham dự:
  - vẫn có thể bị no-show

## 6.4 Cấu hình penalty

Cần chuẩn hóa thêm các field config:

- `noShowPenaltyEnabled`
- `noShowPenaltyPoints`
- `noShowPenaltyScoreType`

Default theo preset:

- `EVENT_BASIC`
  - enabled
  - penalty mặc định có thể bằng participation points
- `EVENT_WITH_SUBMISSION`
  - enabled
- `ENTERPRISE_SEMINAR_BASIC`
  - disabled
- `ENTERPRISE_SEMINAR_WITH_BONUS`
  - disabled
- `MINIGAME_PASS_ONLY`
  - disabled

---

## 7. Quy tắc `TASK_OVERDUE`

## 7.1 Chỉ áp dụng khi

- `requiresSubmission=true`
- task thuộc activity có scoring submission

## 7.2 Không áp dụng khi

- `requiresSubmission=false`
- task chỉ là optional task
- activity thuộc `series`

## 7.3 Hành vi

- Quartz đánh dấu assignment `OVERDUE`
- áp điểm theo `failPoints` của rule submission nếu rule hợp lệ
- không suy penalty từ `points` cộng của event

## 7.4 Quy tắc điểm cho overdue

- `EVENT_WITH_SUBMISSION`:
  - overdue sau khi đã `ATTENDED` sẽ dùng `failPoints`
  - nếu `failPoints = 0` thì kết quả điểm là `0`
  - nếu `failPoints > 0` thì dùng đúng giá trị đó
- `requiresSubmission=false`:
  - không overdue scoring
- activity trong `series`:
  - không overdue scoring

## 7.5 Ý nghĩa nghiệp vụ

- overdue trong activity có submission được coi là kết quả fail của bài nộp
- vì vậy điểm fail phải thống nhất với `failPoints`
- backend không dùng `penalty from participation points` cho case này

---

## 8. Quy tắc `series`

## 8.1 Bản chất activity trong series

- event con chỉ là milestone step
- không cộng điểm event riêng
- không trừ điểm event riêng

## 8.2 Khi nào update progress

- `EVENT_BASIC` trong series:
  - khi đạt `ATTENDED`
- `EVENT_WITH_SUBMISSION` trong series:
  - khi `ATTENDED + GRADED`
- `MINIGAME` trong series:
  - chỉ khi `PASSED`

## 8.3 Scoring của series

- `SERIES_PROGRESS`
  - cộng milestone
- `SERIES_MINIMUM_REQUIREMENT`
  - trừ nếu không đủ số activity tối thiểu

## 8.4 Không làm trong series

- không `NO_SHOW` per child activity
- không `TASK_OVERDUE` per child activity
- không `MINIGAME exhausted attempts penalty`

---

## 9. Hai flow QR chuẩn cần triển khai

## 9.1 Flow A: Activity QR

### Mục đích

- xác nhận tham gia nhanh theo QR chung của activity

### Nguồn dữ liệu

- `Activity.checkInCode`

### Actor

- student tự quét

### State transition mục tiêu

- nếu chưa có participation:
  - tạo participation phù hợp rồi set `ATTENDED`
- nếu đang `CHECKED_IN`:
  - set `ATTENDED`
- không dùng flow này để mô phỏng 2 bước `check-in/check-out`

### Kết quả

- attendance được xác nhận nhanh
- nếu activity không cần submission:
  - có thể đi tiếp sang `COMPLETED`
- nếu activity cần submission:
  - dừng ở `ATTENDED`

## 9.2 Flow B: Ticket QR

### Mục đích

- ban tổ chức quét từng ticket của sinh viên
- điều khiển transition stateful

### Nguồn dữ liệu

- `ActivityRegistration.ticketCode`

### Actor

- organizer / ban tổ chức quét

### State transition mục tiêu

- lần quét đầu:
  - `REGISTERED -> CHECKED_IN`
- lần quét tiếp theo:
  - `CHECKED_IN -> ATTENDED`
- nếu activity không yêu cầu submission:
  - `ATTENDED -> COMPLETED`
- nếu activity yêu cầu submission:
  - dừng ở `ATTENDED`

---

## 10. Ma trận case chuẩn cần backend hỗ trợ

| Case | Attendance | Submission | Completion | Scoring | No-show |
| --- | --- | --- | --- | --- | --- |
| Event basic check-in chưa checkout | `CHECKED_IN` | Không cần | Chưa | Chưa | Có thể có |
| Event basic attended | `ATTENDED` | Không cần | Có | `PARTICIPATION_COMPLETED` | Không |
| Event with submission attended chưa nộp | `ATTENDED` | Chưa | Chưa | Chưa | Không |
| Event with submission attended đã nộp chưa chấm | `ATTENDED` | `SUBMITTED` | Chưa | Chưa | Không |
| Event with submission graded pass | `ATTENDED` | `GRADED` | Có | Pass points | Không |
| Event with submission graded fail | `ATTENDED` | `GRADED` | Có | Fail points hoặc 0 | Không |
| Event with submission attended nhưng overdue | `ATTENDED` | `OVERDUE/late` | Chưa hoàn tất chấm | Dùng `failPoints` làm điểm fail/penalty | Không |
| Event with submission có nộp nhưng không attend | Không | Có | Không | Không | Có |
| Event no-show | Không | Không bắt buộc | Không | Penalty nếu enabled | Có |
| Seminar attended | `ATTENDED` | Không cần | Có | cộng seminar | Không |
| Seminar no-show default | Không | Không cần | Không | không trừ mặc định | Có check nhưng penalty off |
| Minigame pass standalone | N/A | N/A | pass | cộng điểm | Không |
| Minigame fail chưa hết lượt | N/A | N/A | fail | không cộng/trừ | Không |
| Minigame fail hết lượt standalone | N/A | N/A | fail final | penalty mới | Không |
| Minigame fail hết lượt trong series | N/A | N/A | fail final | không penalty | Không milestone |

---

## 11. Kế hoạch phát triển

## Phase 1: Chuẩn hóa state machine

### Mục tiêu

- thống nhất `REGISTERED`, `CHECKED_IN`, `ATTENDED`, `COMPLETED`
- tách rõ `activity QR` và `ticket QR`

### Việc cần làm

- refactor `ActivityRegistrationServiceImpl`
- chuẩn hóa transition manual / QR
- bỏ logic gán `checkIn + checkOut` cùng lúc cho activity QR
- giảm vai trò `CHECKED_OUT` hoặc chỉ giữ như transitional event

### Output

- state machine rõ ràng cho event attendance

## Phase 2: Chuẩn hóa `EVENT_WITH_SUBMISSION`

### Mục tiêu

- `COMPLETED = ATTENDED + GRADED`

### Việc cần làm

- sửa flow `TaskSubmissionServiceImpl`
- sửa flow `gradeCompletion`
- đảm bảo fail vẫn là completed
- chặn trường hợp submission tự làm mất no-show nếu chưa attended
- chuẩn hóa `TASK_OVERDUE` dùng `failPoints` thay vì suy từ `points`

### Output

- flow submission nhất quán

## Phase 3: Chuẩn hóa `requiresSubmission`

### Mục tiêu

- task optional hợp lệ nhưng không scoring/overdue khi `requiresSubmission=false`

### Việc cần làm

- cập nhật validation score rules
- cập nhật assignment/reminder creation
- chặn overdue scoring cho optional task
- khóa rule: `requiresSubmission=true` thì overdue của submission luôn dùng `failPoints`

### Output

- `requiresSubmission` trở thành business gate rõ ràng

## Phase 4: Mở rộng `MINIGAME exhausted attempts`

### Mục tiêu

- thêm penalty cho trường hợp hết lượt mà không pass

### Việc cần làm

- thêm trigger mới, đề xuất:
  - `MINIGAME_EXHAUSTED_ATTEMPTS`
- thêm engine path
- thêm source type ledger riêng nếu cần
- đảm bảo:
  - chỉ apply 1 lần
  - không apply nếu đã từng pass
  - không apply trong series
- sửa logic `startAttempt` để resume `IN_PROGRESS` trước khi check hết lượt

### Output

- minigame penalty flow hoàn chỉnh

## Phase 5: Tách config `NO_SHOW`

### Mục tiêu

- no-show không còn là rule mơ hồ phụ thuộc fallback `points/failPoints`

### Việc cần làm

- thêm config:
  - `noShowPenaltyEnabled`
  - `noShowPenaltyPoints`
  - `noShowPenaltyScoreType`
- cập nhật activity preset
- cập nhật rule preview
- set default:
  - event thường bật
  - seminar tắt

### Output

- no-show chuẩn theo từng preset

## Phase 6: Đồng bộ docs và handoff

### Mục tiêu

- FE/BE cùng hiểu giống nhau

### Việc cần làm

- cập nhật FE handoff spec
- thêm state transition table
- mô tả 2 flow QR
- mô tả `EVENT_WITH_SUBMISSION` completed semantics

### Output

- tài liệu tích hợp rõ ràng

## Phase 7: Test

### Mục tiêu

- khóa behavior quan trọng bằng unit/integration tests

### Nhóm test bắt buộc

- event basic attendance
- event with submission
- optional task no scoring
- seminar no-show default off
- minigame exhausted attempts
- series child activity no individual scoring
- 2 flow QR tách biệt

---

## 12. Thay đổi kỹ thuật dự kiến

### Enum

- thêm `MINIGAME_EXHAUSTED_ATTEMPTS`

### Service / Engine

- `ActivityRegistrationServiceImpl`
- `TaskSubmissionServiceImpl`
- `MiniGameServiceImpl`
- `ActivityScoreRuleServiceImpl`
- `ScoreRuleEngineImpl`
- `ReminderScheduleServiceImpl`
- `ReminderDispatchService`
- `ScorePresetServiceImpl`

### DTO / Config

- activity preset config
- minigame config
- FE handoff docs

---

## 13. Nguyên tắc triển khai

- ưu tiên chuẩn hóa nghiệp vụ trước khi vá từng case lẻ
- không để submission thay thế attendance
- không để activity con trong series có scoring riêng
- no-show và overdue phải phụ thuộc `activity type` + `requiresSubmission`
- seminar không bị trừ ngược phần tích lũy của buổi chính

---

## 14. Kết luận

Hướng phát triển tiếp theo là:

1. Chuẩn hóa state machine attendance
2. Chuẩn hóa completion cho `EVENT_WITH_SUBMISSION`
3. Tách rõ optional task và submission-gated activity
4. Bổ sung minigame exhausted attempts penalty cho standalone activity
5. Chuẩn hóa no-show config theo preset và score type

Sau khi hoàn tất các bước trên, backend sẽ có flow điểm rõ ràng, ít mâu thuẫn hơn, và dễ handoff cho frontend hơn.
