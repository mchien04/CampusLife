# Multi-Tenant Department Authorization — Handoff Triển khai

> **Phiên bản:** 1.0  
> **Ngày cập nhật:** 2026-07-08  
> **Source of Truth kiến trúc:** [`multi-tenant-department-architecture.md`](./multi-tenant-department-architecture.md)  
> **Trạng thái:** Phase 1–4 đã triển khai trên backend; enforcement mặc định **tắt** trên production (`department.scope.enforcement.enabled=false`).

---

## 1. Tóm tắt Executive

Hệ thống đã bổ sung **Department Scoping Layer** cho role `MANAGER`: mọi truy vấn và thao tác quản trị phải gắn với khoa/đơn vị được phân công qua bảng `user_departments`. Role `ADMIN` giữ quyền toàn trường (có ghi audit bypass). Role `STUDENT` **không** bị scope — vẫn xem activity công khai như cũ.

**Nguyên tắc vận hành:**

| Role | Hành vi |
|------|---------|
| `ADMIN` | Toàn quyền; bypass scope; ghi `DEPT_ADMIN_BYPASS` vào `audit_logs` khi enforcement bật |
| `MANAGER` | Chỉ thấy/sửa dữ liệu thuộc khoa trong `user_departments` hoặc activity có khoa mình là `organizer` |
| `MANAGER` chưa gán khoa | HTTP 403 `"Manager chưa được phân công Khoa"` (khi enforcement bật) |
| `STUDENT` | Không áp department scope cho read public |

---

## 2. Database Migrations (MySQL)

Ba migration mới — cú pháp **MySQL chuẩn** (Flyway chạy một lần; không dùng `ADD COLUMN IF NOT EXISTS` / `CREATE INDEX IF NOT EXISTS`):

### V1028 — `department_scope_foundation`

| Thay đổi | Mô tả |
|----------|--------|
| `user_departments` | Bảng N-N gán Manager ↔ Department |
| Snapshot columns | `student_department_id_at_registration`, `_at_participation`, `_at_award` trên registration/participation/score |
| Backfill | UPDATE snapshot từ `students.department_id` hiện tại |
| Indexes | `user_departments`, snapshot columns |

### V1029 — `department_owned_content_foundation`

| Thay đổi | Mô tả |
|----------|--------|
| `event_articles.owner_department_id` | Owner khoa cho bài viết admin |
| `email_history.sender_department_id`, `recipient_department_id_at_send` | Metadata campaign email |
| `email_history_target_departments` | N-N target departments |
| `notifications.sender_department_id` | Metadata notification |
| `notification_target_departments` | N-N target departments |

### V1030 — `department_scope_performance_indexes`

| Index | Bảng |
|-------|------|
| `idx_activity_departments_dept_activity` | `activity_departments(department_id, activity_id)` |
| `idx_students_department` | `students(department_id)` |

**Triển khai:** chạy Flyway trên MySQL như các migration khác. Script dùng `INNER JOIN` cho UPDATE (chuẩn MySQL).

**Seed bắt buộc trước khi bật enforcement:**

```sql
-- Ví dụ: gán manager user_id=5 quản lý department_id=1
INSERT INTO user_departments (user_id, department_id, assigned_by_user_id)
VALUES (5, 1, 1);
```

---

## 3. Feature Flags & Cấu hình

File: `src/main/resources/application.properties`

```properties
department.scope.enforcement.enabled=${DEPARTMENT_SCOPE_ENFORCEMENT_ENABLED:false}
department.scope.auditOnly=${DEPARTMENT_SCOPE_AUDIT_ONLY:false}
```

| Flag | Mặc định prod | Ý nghĩa |
|------|---------------|---------|
| `enforcement.enabled` | `false` | Bật chặn 403 khi vi phạm scope |
| `auditOnly` | `false` | Chỉ log vi phạm, **không** chặn (dry-run) |

Test profile (`application-test.properties`) đặt `enforcement.enabled=true`.

**Class cấu hình:** `DepartmentScopeProperties`

---

## 4. Kiến trúc Core (Java)

### 4.1. Luồng request

```
JWT Filter → DepartmentContextFilter → Controller → Service (scoped overload)
                      ↓
            DepartmentScopeResolver (DB: user_departments)
                      ↓
            DepartmentRequestScope (HttpServletRequest attribute)
```

- **`DepartmentScope`** — record: `admin`, `student`, `studentId`, `departmentIds`
- **`DepartmentScopeResolver`** — đọc scope từ DB theo username; không tin JWT claims
- **`DepartmentContextFilter`** — gắn scope vào request; chặn manager chưa gán khoa
- **`DepartmentRequestScope`** — `get(HttpServletRequest)` trong controller

### 4.2. Authorization & Query

| Component | Vai trò |
|-----------|---------|
| `DepartmentAuthorizationService` | `requireActivityAccess`, `requireStudentAccess`, `requireSeriesAccess`, … |
| `DepartmentScopeSpec` | JPA `Specification<>` filter theo `deptIds` cho từng entity |
| `DepartmentScopeAuditService` | Ghi `audit_logs`: `DEPT_SCOPE_VIOLATION`, `DEPT_ADMIN_BYPASS` |

### 4.3. Pattern Controller (Manager)

```java
DepartmentScope scope = DepartmentRequestScope.get(httpRequest).orElse(null);
Result result = hasManagerScope(scope)
    ? service.method(args, scope)
    : service.method(args);

private boolean hasManagerScope(DepartmentScope scope) {
    return scope != null && scope.manager() && !scope.departmentIds().isEmpty();
}
```

- **ADMIN** hoặc test không có scope → gọi overload không scope (backward compatible)
- **MANAGER** có `departmentIds` → gọi overload có guard + scoped query

### 4.4. Pattern Service (Manager create)

Manager tạo activity/content phải gán `organizerIds` / `ownerDepartmentId` **trong scope**:

- 1 khoa → auto-assign khoa đó nếu không gửi `organizerIds`
- Nhiều khoa → bắt buộc chọn `organizerIds` ⊆ scope

---

## 5. Module đã áp dụng Department Scope

### Phase 1 — Schema & Auth Foundation

- Entity: `UserDepartment`, snapshot columns trên registration/participation/score
- Entity: `EmailHistory`, `Notification`, `EventArticle` — department metadata (V1029)
- `DepartmentContextFilter`, `DepartmentScopeResolver`, `SecurityConfig` filter order

### Phase 2 — Repository & Spec

`DepartmentScopeSpec` hỗ trợ:

`Activity`, `MiniGame`, `ActivitySeries`, `Student`, `StudentClass`, `ActivityRegistration`, `ActivityParticipation`, `ActivityTask`, `TaskAssignment`, `TaskSubmission`, `ScoreEntry`, `StudentScore`, `PreparationTask`, `Expense`, `FundAdvance`, `AllocationAdjustmentRequest`, `EventArticle`, `EmailHistory`, `Notification`

Repository bổ sung method `existsBy…DepartmentIds` / `JpaSpecificationExecutor` where needed.

### Phase 3 — Service & Controller Adoption

| Module | Controller / Service | Ghi chú |
|--------|---------------------|---------|
| Activity | `ActivityController`, `ActivityServiceImpl` | List/filter/create/update theo organizer scope |
| Standard Activity | `StandardActivityController`, `StandardActivityServiceImpl` | Giống Minigame pattern |
| Minigame Activity | `MinigameActivityController`, `MinigameActivityServiceImpl` | |
| MiniGame (quiz) | `MiniGameController`, `MiniGameServiceImpl` | |
| Activity Series | `ActivitySeriesController`, `ActivitySeriesServiceImpl` | |
| Registration / Participation | `ActivityRegistrationController`, `ActivityRegistrationServiceImpl` | List, approve, report scoped |
| Activity Task | `ActivityTaskController`, `ActivityTaskServiceImpl` | |
| Task Assignment | `TaskAssignmentController` | |
| Task Submission | `TaskSubmissionController`, `TaskSubmissionServiceImpl` | |
| Score | `ScoreController`, `ScoreServiceImpl` | |
| Statistics | `StatisticsController`, `StatisticsServiceImpl` | Dashboard scoped |
| Student | `StudentController`, `StudentServiceImpl` | CRUD/list trong scope |
| Event Article (admin) | `EventArticleAdminController`, `EventArticleServiceImpl` | `owner_department_id` |
| Preparation | `PreparationController`, `PreparationServiceImpl` | Dashboard, summary, task, organizer |
| Preparation Finance | `PreparationFinanceController`, `PreparationFinanceServiceImpl` | Budget, expense, fund advance, reports |
| Preparation Export | `PreparationExportController`, `PreparationExportServiceImpl` | Export guard activity access |
| Email / Notification send | `EmailController`, `EmailServiceImpl` | Validate recipient type; scoped history |
| User management | `UserManagementController`, `UserManagementServiceImpl` | Gán khoa Manager tích hợp CRUD (không endpoint riêng) |
| Student accounts | `StudentAccountManagementController`, `StudentAccountManagementServiceImpl` | Manager truy cập; khoa tùy chọn khi tạo/sửa |

### 5.1 User & Student Account — Gán khoa tích hợp CRUD

**Manager (`/api/admin/users/**` — ADMIN only):**

| Thao tác | Body | Ghi chú |
|----------|------|---------|
| `POST /api/admin/users` | `{ ..., "role": "MANAGER", "departmentIds": [1, 2] }` | `departmentIds` **bắt buộc** khi tạo MANAGER |
| `PUT /api/admin/users/{userId}` | `{ "departmentIds": [1] }` | Thay toàn bộ danh sách khoa của MANAGER |
| `GET` user / list | Response có `departmentIds` nếu role MANAGER | |

**Student accounts (`/api/admin/students/**` — ADMIN + MANAGER):**

| Luồng | `departmentId` | Ghi chú |
|-------|----------------|---------|
| Import Excel / `bulk-create` / `create-multiple` | Không cần | SV tự điền khoa sau khi đăng nhập (`PUT /api/student/profile`) |
| Tạo thủ công `POST /create` | Tùy chọn | Admin gán bất kỳ khoa; Manager chỉ gán khoa trong scope |
| Sửa `PUT /{studentId}/account` | Tùy chọn | Admin/Manager gán hoặc đổi khoa; Manager chỉ sửa SV thuộc khoa mình hoặc SV chưa có khoa (gán khoa trong scope) |
| Response `StudentAccountResponse` | `departmentId`, `departmentName` | |

Không có endpoint riêng `GET/PUT .../departments` — mọi gán khoa nằm trong CRUD hiện có.

**Scope các thao tác khác trên student account (ADMIN + MANAGER):**

| Endpoint | Manager scope |
|----------|---------------|
| `GET /pending` | Chỉ liệt kê SV thuộc khoa được phân công (`DepartmentScopeSpec.student`). SV chưa có khoa không hiện với Manager. |
| `DELETE /{studentId}/account` | `requireStudentAccess` — chỉ xóa SV trong khoa scope |
| `POST /{studentId}/send-credentials` | `requireStudentAccess` — chỉ gửi cho SV trong khoa scope |
| `POST /bulk-send-credentials` | Bỏ qua (ghi lỗi `Access denied`) các SV ngoài khoa scope |
| `POST /bulk-create`, `/create-multiple`, `/upload-excel` | Tạo SV mới không kèm khoa (không lộ dữ liệu SV khác) |

### Phase 4 — Hardening & Audit

- `DepartmentScopeAuditService` — audit violation & admin bypass
- `DepartmentAuthorizationService` — tích hợp audit + `auditOnly`
- Migration V1030 — performance indexes
- Penetration tests (xem §7)

---

## 6. Email / Notification — Recipient Policy (Manager)

`EmailServiceImpl` validate trước khi gửi:

| RecipientType | Manager |
|---------------|---------|
| `ALL_STUDENTS` | **Chặn** |
| `BY_DEPARTMENT` | Chỉ khoa trong scope |
| `BY_CLASS` | Chỉ lớp thuộc khoa scope |
| `BULK` | Validate từng user trong scope |
| `ACTIVITY_REGISTRATIONS` | Chỉ nếu manager là organizer của activity |
| `SERIES_REGISTRATIONS` | Chỉ nếu manager có quyền series |

Notification-only send: `POST /api/emails/notifications/send` — cùng validation.

Email history list (`GET /api/emails/history`): luôn lọc theo `senderId` của người đăng nhập. Khi gửi email/notification, backend ghi `sender_department_id`, `recipient_department_id_at_send`, và `email_history_target_departments` / `notification_target_departments` để phục vụ audit và dashboard sau này.

---

## 7. Tests

| Test class | Mục đích |
|------------|----------|
| `DepartmentScopeResolverTest` | Resolve scope theo role |
| `DepartmentContextFilterTest` | Filter gắn scope / 403 unassigned manager |
| `DepartmentScopeSpecRepositoryTest` | Spec filter activity/student/registration/article |
| `UserDepartmentRepositoryTest` | Repository mapping |
| `DepartmentScopePenetrationTest` | Manager không truy cập activity/student khoa khác |
| `DepartmentScopeAuditOnlyTest` | `auditOnly` không throw |
| `DepartmentScopeAuditServiceTest` | Unit test ghi audit |
| `StandardActivityServiceImplScopeTest` | Validate organizer scope khi tạo activity |

Chạy: `mvn test`

---

## 8. Hướng dẫn bật Production

1. Chạy Flyway V1028 → V1030 trên MySQL
2. Seed `user_departments` cho tất cả manager
3. Backfill `event_articles.owner_department_id` nếu có dữ liệu cũ
4. Bật dry-run: `DEPARTMENT_SCOPE_AUDIT_ONLY=true`, `ENFORCEMENT=false` — theo dõi log/audit
5. Bật enforcement: `DEPARTMENT_SCOPE_ENFORCEMENT_ENABLED=true`, `AUDIT_ONLY=false`
6. FE Manager: không cần gửi `departmentIds` trong JWT — scope resolve server-side; có thể thêm filter `?departmentId=` cho ADMIN dashboard

---

## 9. Tác động Frontend (Manager UI)

| Khu vực | FE cần làm |
|---------|------------|
| Manager dashboard | Chỉ hiển thị dữ liệu trả về từ API (BE đã filter); không assume toàn trường |
| Tạo Activity (standard/minigame/series) | Gửi `organizerIds` thuộc khoa manager; 1 khoa có thể bỏ qua |
| Tạo Event Article | Gửi / hiển thị `ownerDepartmentId` |
| Email campaign | Không hiển thị option broadcast toàn trường cho Manager |
| Preparation / Finance | Không đổi contract API; scope tự áp khi manager login |
| Admin user CRUD | Tạo/sửa MANAGER kèm `departmentIds` trong body; không endpoint riêng |
| Student account admin | Excel bulk không cần khoa; form tạo thủ công có dropdown khoa; form sửa cho phép gán khoa |
| Admin | Có thể thêm dropdown filter khoa trên statistics/list (optional `departmentId`) |

API contract **không breaking** cho ADMIN và STUDENT. Manager nhận 403 hoặc list rỗng nếu truy cập cross-department.

---

## 10. Chưa triển khai / Follow-up

| Hạng mục | Ghi chú |
|----------|---------|
| Notification admin campaign list | Chưa có endpoint dashboard; user notifications (`/api/notifications/**`) vẫn self-scope |
| Export audit metadata | SoT yêu cầu log requester/dept_ids/row count khi export — chưa ghi riêng |
| SecurityConfig endpoint audit table | Checklist SoT §5.4 — review thủ công |
| `ActivityScoreRuleService` target department validation | Cần verify riêng nếu chưa cover |
| Redis cache cho `DepartmentScopeResolver` | Chưa cần; DB-only phase 1 |

---

## 11. File tham chiếu nhanh

```
db/migration/
  V1028__department_scope_foundation.sql
  V1029__department_owned_content_foundation.sql
  V1030__department_scope_performance_indexes.sql

src/main/java/vn/campuslife/
  config/DepartmentScopeProperties.java
  filter/DepartmentContextFilter.java
  security/department/
    DepartmentScope.java
    DepartmentScopeResolver.java
    DepartmentRequestScope.java
    DepartmentAuthorizationService.java
    DepartmentScopeSpec.java
    DepartmentScopeAuditService.java
  entity/UserDepartment.java

docs/
  multi-tenant-department-architecture.md   ← SoT thiết kế
  multi-tenant-department-handoff.md        ← Tài liệu này
```

---

## 12. Rollback

- Tắt enforcement: `DEPARTMENT_SCOPE_ENFORCEMENT_ENABLED=false` — code mới vẫn chạy, manager thấy full data như cũ
- Schema expand-only: không cần rollback migration để restore login/public flow
- Không xóa cột/tables V1028–V1030 trừ khi có migration revert riêng

---

*Tài liệu handoff phản ánh trạng thái codebase tại thời điểm Phase 1–4 hoàn tất. Mọi thay đổi tiếp theo cập nhật vào file này và delta FE nếu có breaking contract.*
