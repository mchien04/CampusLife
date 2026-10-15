# Sequence Diagram - Score Rule (Quy tắc tính điểm)

Quy tắc tính điểm được gửi cùng dữ liệu tạo hoặc cập nhật hoạt động. Backend lưu chúng qua `ActivityScoreRuleService.replaceRules`; không có endpoint CRUD riêng cho từng score rule. Ví dụ dưới đây dùng luồng cập nhật hoạt động thường qua `PUT /api/activities/standard/{id}`. Hoạt động minigame cũng có `scoreRules` trong request tạo/cập nhật.

## 1. Cấu hình score rule khi cập nhật hoạt động

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin/Manager
    participant Client as React Client
    participant Controller as StandardActivityController
    participant Service as StandardActivityServiceImpl
    participant Preset as ScorePresetService
    participant RuleService as ActivityScoreRuleServiceImpl
    participant ActivityRepo as ActivityRepository
    participant RuleRepo as ActivityScoreRuleRepository
    participant EntryRepo as ScoreEntryRepository

    Admin->>Client: Chỉnh sửa hoạt động và scoreRules
    Client->>Controller: PUT /api/activities/standard/{id}
    Controller->>Service: updateActivity(id, request, scope)
    Service->>Service: Kiểm tra quyền manager theo phòng ban
    Service->>ActivityRepo: findByIdAndIsDeletedFalse(id)
    ActivityRepo-->>Service: Activity hiện tại
    Service->>Service: Kiểm tra presetCode và loại hoạt động
    Service->>Preset: applyActivityPreset(request, effectiveType)
    Preset-->>Service: Request sau khi áp dụng preset
    Service->>ActivityRepo: save(activity sau khi cập nhật)
    ActivityRepo-->>Service: Activity đã lưu

    alt request.scoreRules khác null
        Service->>RuleService: replaceRules(activityId, scoreRules)
        RuleService->>ActivityRepo: findById(activityId)
        ActivityRepo-->>RuleService: Activity
        RuleService->>RuleService: Bổ sung failPoints mặc định nếu phù hợp
        RuleService->>RuleService: Kiểm tra trigger, loại điểm, đối tượng, học kỳ và loại hoạt động
        RuleService->>EntryRepo: countByActivityIdAndStatus(activityId, ACTIVE)
        EntryRepo-->>RuleService: Số score entry đang hiệu lực
        alt Có entry ACTIVE và activity đã công bố
            RuleService--xService: Từ chối sửa score rules
            Service-->>Controller: Response lỗi
            Controller-->>Client: HTTP 400
            Client-->>Admin: Hiển thị lý do không thể sửa rule
        else Được phép thay đổi
            RuleService->>RuleRepo: findByActivityId(activityId)
            RuleRepo-->>RuleService: Các rule hiện có
            loop Mỗi rule trong request
                RuleService->>RuleService: So khớp trigger, scoreType và calculation
                alt Đã có rule cùng khóa
                    RuleService->>RuleRepo: save(rule đã cập nhật)
                else Rule mới
                    RuleService->>RuleRepo: save(rule mới)
                end
            end
            loop Mỗi rule cũ không còn trong request
                alt Có score entry ACTIVE
                    RuleService->>RuleRepo: save(rule với enabled=false)
                else Chưa có score entry ACTIVE
                    RuleService->>RuleRepo: delete(rule)
                end
            end
            RuleService-->>Service: Hoàn tất đồng bộ rules
            Service-->>Controller: Response thành công
            Controller-->>Client: HTTP 200
            Client-->>Admin: Hiển thị hoạt động đã cập nhật
        end
    else request.scoreRules là null
        Service-->>Controller: Response thành công và giữ nguyên rules
        Controller-->>Client: HTTP 200
        Client-->>Admin: Hiển thị hoạt động đã cập nhật
    end
```

Khi tạo hoạt động thường, service áp dụng preset, kiểm tra request, lưu `Activity`, rồi chỉ gọi `replaceRules` nếu danh sách `scoreRules` có phần tử. Khi cập nhật, `scoreRules = null` giữ nguyên các rule, còn danh sách rỗng yêu cầu đồng bộ về rỗng.

## 2. Áp dụng score rule sau khi ghi nhận hoàn thành hoạt động

```mermaid
sequenceDiagram
    autonumber
    participant Registration as ActivityRegistrationServiceImpl
    participant Engine as ScoreRuleEngineImpl
    participant RuleService as ActivityScoreRuleService
    participant RuleRepo as ActivityScoreRuleRepository
    participant Semester as ScoreSemesterResolver
    participant EntryService as ScoreEntryServiceImpl
    participant EntryRepo as ScoreEntryRepository
    participant TotalRepo as StudentScoreRepository

    Note over Registration,Engine: Sau khi ghi nhận ActivityParticipation
    Registration->>Engine: applyActivityCompleted(participation, actor)
    alt Activity thuộc chuỗi sự kiện
        Engine-->>Registration: Không cộng điểm hoàn thành riêng lẻ
    else Activity độc lập
        Engine->>RuleService: getEnabledRules(activityId, PARTICIPATION_COMPLETED)
        RuleService->>RuleRepo: findByActivityIdAndTriggerTypeAndEnabledTrue(...)
        RuleRepo-->>RuleService: Các rule đang bật
        RuleService-->>Engine: Danh sách rule
        loop Mỗi rule
            Engine->>Engine: Kiểm tra student thuộc audience của rule
            alt Student đủ điều kiện
                Engine->>Engine: Chọn points hoặc failPoints theo isCompleted
                Engine->>Semester: resolveSemester(activity, rule, participation.date)
                Semester-->>Engine: Semester
                Engine->>EntryService: upsertEntry(student, activity, rule, semester, participation)
                EntryService->>EntryRepo: Tìm entry ACTIVE theo student, source và rule
                EntryRepo-->>EntryService: Entry hiện có hoặc rỗng
                alt Entry mới hoặc điểm thay đổi
                    EntryService->>EntryRepo: save(ScoreEntry ACTIVE)
                    EntryService->>EntryRepo: sumPointsByStudentAndSemesterAndScoreTypeAndStatus(...)
                    EntryRepo-->>EntryService: Tổng điểm ACTIVE
                    EntryService->>TotalRepo: save(StudentScore với tổng điểm mới)
                end
                EntryService-->>Engine: ScoreEntry
            end
        end
        Engine-->>Registration: Danh sách AppliedScoreAward
    end
```

Cùng engine này còn xử lý các trigger khác như `NO_SHOW`, `SUBMISSION_GRADED`, `TASK_OVERDUE`, `MINIGAME_PASSED` và `MINIGAME_EXHAUSTED_ATTEMPTS`. Luồng milestone của chuỗi sự kiện được tính riêng từ `StudentSeriesProgress` và cấu hình milestone của chuỗi.

### Source tham chiếu

- [StandardActivityController](../../src/main/java/vn/campuslife/controller/activity/StandardActivityController.java)
- [StandardActivityServiceImpl](../../src/main/java/vn/campuslife/service/impl/StandardActivityServiceImpl.java)
- [ActivityScoreRuleServiceImpl](../../src/main/java/vn/campuslife/service/impl/ActivityScoreRuleServiceImpl.java)
- [ScoreRuleEngineImpl](../../src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java)
- [ScoreEntryServiceImpl](../../src/main/java/vn/campuslife/service/impl/ScoreEntryServiceImpl.java)
