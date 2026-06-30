# Giải pháp Kiến trúc Multi-Tenant theo Khoa/Đơn vị (Department-Scoped Authorization)

> **Mức độ**: Nâng cấp lớn (Major Architecture Change)  
> **Nguyên tắc**: Security-First, Defense in Depth, Zero-Trust Query Scope  
> **Mục tiêu**: Manager chỉ thấy/sửa được dữ liệu thuộc Khoa mình quản lý, không có "backdoor" bằng cách gọi API trực tiếp.

---

## 1. Phân tích Hiện trạng & Thách thức

### Hiện trạng
| Thành phần | Tình trạng |
|---|---|
| `User.role` | `ADMIN` / `MANAGER` / `STUDENT` — phân quyền đơn giản theo enum |
| `User` | **Không có** `department_id` → Manager chưa gắn với Khoa nào |
| `Student` | Đã có `department` (ManyToOne) và `studentClass.department` |
| `Activity` | Đã có `organizers` (ManyToMany `Department`) |
| `Registration/Submission/Assignment` | Quản lý đăng ký, bài nộp, phân công nhiệm vụ đang cascade theo activity/student nhưng chưa có department scope nhất quán cho MANAGER |
| `Preparation` | Đang có security theo organizer/prep supervisor, nhưng `ADMIN/MANAGER` vẫn được bypass rộng ở nhiều endpoint |
| `Score/Statistics` | Có nhiều query tổng hợp/count/top list chưa nhận department scope bắt buộc |
| `Email/Notification/Article` | Đang phân quyền chủ yếu theo role, chưa gắn phạm vi khoa cho audience, lịch sử gửi, nội dung quản trị |
| `SecurityConfig` | Chỉ kiểm tra `hasRole/hasAnyRole`, **không kiểm tra** department scope |
| Repository queries | Chưa có filter department cho MANAGER |
| Dashboard/Statistics | Tổng toàn trường, chưa có filter theo Khoa |

### Thách thức chính
1. **Horizontal Authorization (Data Scope)**: Ngăn Manager Khoa A đọc Activity/Student/Registration của Khoa B.
2. **Cross-Cutting Concern**: Có ~20-30 repository và service cần áp scope. Không thể sửa thủ công từng query (dễ miss, dễ bug).
3. **ADMIN toàn quyền**: ADMIN vẫn phải xem được toàn bộ.
4. **STUDENT**: Sinh viên thuộc Khoa X vẫn xem được Activity công khai của Khoa khác, nhưng Manager thì không.
5. **Many-to-Many Organizers**: Một Activity có thể thuộc nhiều Khoa đồng tổ chức. Manager của bất kỳ Khoa nào trong danh sách `organizers` đều có quyền quản lý.
6. **Statistics Aggregation**: Dashboard cần đếm, group, join — scope phải được đẩy xuống SQL thay vì lọc trong Java (hiệu năng).
7. **Module không chỉ Activity**: Scope phải bao phủ đăng ký, bài nộp, phân công nhiệm vụ, preparation, tài chính chuẩn bị, điểm số, thống kê, email, thông báo, bài viết, sinh viên/lớp, export/report.
8. **Manager thêm sinh viên**: Manager được tạo/thêm/cập nhật sinh viên trong khoa mình, nhưng không được đưa sinh viên vào khoa khác hoặc xem/sửa sinh viên ngoài scope.

---

## 2. Giải pháp Tổng thể: "Department Scoping Layer"

Kiến trúc gồm 4 lớp cắt ngang (cross-cutting layers):

```
┌─────────────────────────────────────────────────────────────┐
│  LAYER 4: API / Controller                                  │
│  - @PreAuthorize + custom SpEL: @IsManagerOfDepartment     │
│  - DepartmentScopeResolver từ DB trước, không tin scope từ client │
├─────────────────────────────────────────────────────────────┤
│  LAYER 3: Service                                           │
│  - Không tự viết WHERE department_id = ?                   │
│  - Gọi repository bình thường, scope tự inject ở Layer 2   │
├─────────────────────────────────────────────────────────────┤
│  LAYER 2: Repository / Query Scope Enforcement              │
│  - JpaSpecificationExecutor + TenantAwareSpec               │
│  - Hoặc Hibernate Filter (global WHERE)                   │
│  - Hoặc Custom Repository Proxy (AOP)                     │
├─────────────────────────────────────────────────────────────┤
│  LAYER 1: Data Model (Schema Evolution)                     │
│  - user_departments (MANAGER ↔ DEPARTMENT)                │
│  - JWT chỉ giữ identity/role; scope lấy từ DB/distributed cache │
│  - Activity.organizers đã sẵn                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Lớp 1: Thay đổi Data Model (Zero-Downtime Migration)

### 3.1. Bảng mới: `user_departments`
```sql
CREATE TABLE user_departments (
    user_id        BIGINT NOT NULL,
    department_id  BIGINT NOT NULL,
    assigned_at    TIMESTAMP DEFAULT NOW(),
    assigned_by_user_id BIGINT NULL, -- ADMIN nào phân quyền
    PRIMARY KEY (user_id, department_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_user_departments_user ON user_departments(user_id);
CREATE INDEX idx_user_departments_dept ON user_departments(department_id);
```
- Cho phép 1 Manager quản lý **nhiều Khoa** (multi-department manager).
- Mối quan hệ Many-to-Many, không sửa trực tiếp `users` table (expand-contract pattern).
- `assigned_by_user_id` giữ referential integrity tốt hơn username string. Nếu cần hiển thị username/email lịch sử, tạo audit log riêng lưu snapshot text.
- Khi DELETE department, service phải kiểm tra trước còn manager mapping trong `user_departments` không. Nếu còn, trả `409 Conflict` với message rõ danh sách manager cần gỡ/chuyển trước; không để FK `ON DELETE RESTRICT` bung thành lỗi 500.

### 3.2. JWT Payload và nguồn sự thật scope
```json
{
  "sub": "manager_a",
  "role": "MANAGER"
}
```
- **Không dùng `dept_ids` trong JWT làm nguồn sự thật** vì token hiện có TTL dài và sẽ stale ngay khi ADMIN đổi phân công khoa.
- Giai đoạn đầu dùng **DB-only, không cache**: `DepartmentScopeResolver` tra `user_departments` mỗi request cần scope bằng indexed query (`user_id`/`username`). Cách này đơn giản, nhất quán giữa nhiều pod, và tránh stale authorization.
- Nếu hiệu năng trở thành vấn đề, chỉ chuyển sang Redis/distributed cache có invalidation đồng bộ hoặc pub/sub. Không dùng Caffeine/in-process cache trong multi-pod nếu không có distributed invalidation.
- `JwtUtil.generateToken(UserDetails userDetails)` hiện chỉ nhận `UserDetails`; Phase 1 không cần đổi signature để nhét `dept_ids`. Nếu sau này muốn thêm claim scope tối ưu hiệu năng, phải đi kèm refresh token ngắn hạn, token revocation, và sửa `AuthServiceImpl.login()` load `user_departments` trước khi tạo token.
- Khi ADMIN thay đổi phân công khoa/phòng ban, nếu có cache scope thì cache phải bị evict ngay. Nếu dùng cache phân tán, event invalidation phải là một phần của transaction quản lý phân công.

### 3.3. DepartmentType scope rule
`DepartmentType.KHOA` và `DepartmentType.PHONG_BAN` là các đơn vị cùng cấp trong mô hình scope. `user_departments.department_id` có thể gán manager vào bất kỳ `Department` active nào, không giới hạn chỉ `KHOA`.

Quy tắc:
- Scope của manager luôn là tập `Department.id`, bất kể type là `KHOA` hay `PHONG_BAN`.
- Activity organizers cũng dùng cùng tập `Department`; activity có thể do khoa và phòng ban đồng tổ chức.
- Student scope vẫn dựa trên `Student.department_id` và không phân biệt type. Nếu sinh viên/lớp gắn với Department type `PHONG_BAN` thì manager của phòng ban đó có quyền như manager của `KHOA`; `KHOA` và `PHONG_BAN` chỉ khác tên/phân loại hiển thị, không khác cấp scope.

### 3.4. Student đã có department
- `Student.department_id` đã tồn tại → **không cần sửa**.
- Tuy nhiên cần đảm bảo `Student.department` luôn đồng bộ với `Student.studentClass.department` (nếu chuyển lớp thì cập nhật cả 2).

### 3.5. Snapshot khoa cho lịch sử
Các nghiệp vụ lịch sử không nên thay đổi khi sinh viên chuyển khoa:
- `activity_registrations`: thêm `student_department_id_at_registration`.
- `activity_participations`: thêm `student_department_id_at_participation` nếu bảng participation không luôn join qua registration.
- `score_entries` / `student_scores`: thêm `student_department_id_at_award` hoặc đảm bảo score query có thể truy về snapshot tương ứng.

Mục tiêu: thống kê kỳ trước không bị đổi retroactively khi admin/manager chuyển sinh viên sang khoa khác.

Migration data cũ:
- Backfill bằng department hiện tại của student tại thời điểm migration:
  ```sql
  UPDATE activity_registrations ar
  SET student_department_id_at_registration = s.department_id
  FROM students s
  WHERE ar.student_id = s.id
    AND ar.student_department_id_at_registration IS NULL;
  ```
- Chấp nhận đây là approximation, không phải lịch sử thật. Report giai đoạn trước migration phải ghi chú "department snapshot approximated from current student department".
- Query scoped phải handle `NULL`: data cũ chưa backfill hoặc student không có khoa chỉ ADMIN thấy, trừ khi business chấp nhận fallback current department.
- Sau backfill, đặt NOT NULL cho snapshot ở các bảng đủ dữ liệu; nếu có student không khoa thì giữ nullable và xử lý explicit.

### 3.6. Department-owned content
Các nội dung do manager tạo cần có ownership rõ:
- `event_articles`: thêm `owner_department_id` hoặc `owner_department_ids` nếu bài viết có nhiều khoa đồng sở hữu.
- `notifications` / notification campaigns: lưu `sender_department_id`, `target_department_ids`.
- `email_history`: lưu `sender_department_id`, `target_department_ids`, và nếu gửi cho sinh viên thì lưu recipient department snapshot.

Nếu không thêm ownership, manager có thể tạo/sửa nội dung toàn trường chỉ vì có role `MANAGER`.

---

## 4. Lớp 2: Query Scope Enforcement (Bảo vệ ở tầng Data)

> **Quy tắc vàng**: Không để Service/Controller tự nhiệm vụ filter department. Phải enforce ở tầng repository/query để tránh lỗ hổng khi dev quên thêm WHERE.

### Phương án A: Hibernate `@Filter` (Khuyến nghị cho read-path đơn giản)
```java
@FilterDef(name = "departmentScopeFilter", 
           parameters = @ParamDef(name = "deptIds", type = Long[].class))
@Filter(name = "departmentScopeFilter", 
        condition = "id IN (SELECT activity_id FROM activity_departments WHERE department_id IN (:deptIds))")
```
- **Ưu điểm**: Kích hoạt 1 lần trên `Session`, tất cả query sau đó tự động scope.
- **Nhược điểm**: Khó áp với query phức tạp (aggregation, native SQL). Phải `enable/disable` filter thủ công cho ADMIN.

### Phương án B: JPA Specification + TenantAware Repository (Khuyến nghị tổng thể)
Mỗi repository custom kế thừa thêm `JpaSpecificationExecutor`.
```java
public interface ActivityRepository extends JpaRepository<Activity, Long>, 
                                              JpaSpecificationExecutor<Activity> {
}
```

**Tenant Specification Builder**:
```java
public class DepartmentScopeSpec {
    public static Specification<Activity> forManager(List<Long> deptIds) {
        return (root, query, cb) -> {
            query.distinct(true); // ManyToMany organizers can duplicate rows
            Join<Activity, Department> organizers = root.join("organizers");
            return organizers.get("id").in(deptIds);
        };
    }
}
```
- **Ưu điểm**: Kiểm soát hoàn toàn, hoạt động với `Page`, `Sort`, projection.
- **Nhược điểm**: Phải gọi `.findAll(Specification)` thay vì `.findAll()`. Cần wrap repository.
- Với `Pageable`, ưu tiên `EXISTS` subquery hoặc count query riêng nếu `distinct` làm lệch total count/performance.
- Không viết một spec generic mơ hồ cho mọi entity. Phải có strategy theo entity:
  - `Activity`: scope qua `activity.organizers`.
  - `Student`: scope qua `student.department`.
  - `ActivityRegistration` / `ActivityParticipation`: scope qua snapshot department hoặc `student.department` theo policy.
  - `TaskSubmission` / `TaskAssignment`: scope qua `task.activity.organizers` và student owner/assignee.
  - `EventArticle`: scope qua `owner_department_id`.
  - `EmailHistory` / `Notification`: scope qua sender/target departments.

### Phương án C: Custom Repository Guard / AOP (Chỉ là backstop)
Dùng Spring AOP bao quanh các phương thức `find*`, `count*` trong repository:
```java
@Around("execution(* vn.campuslife.repository.scoped..*.find*(..)) || execution(* vn.campuslife.repository.scoped..*.count*(..))")
public Object enforceDepartmentScope(ProceedingJoinPoint pjp) {
    if (SecurityUtils.isManager()) {
        // Nếu tham số không phải Specification đã chứa scope → reject hoặc auto-wrap
        // Hoặc: reject gọi trực tiếp findAll() không có scope
    }
    return pjp.proceed();
}
```
- **Mục đích**: Ngăn developer quên scope bằng cách crash/fail-fast trong môi trường dev/test.
- AOP không phải primary defense: không bắt tốt mọi async/scheduled/auth flow và dễ quá rộng nếu intercept toàn bộ repository package.
- Primary defense là scoped service/repository methods có `DepartmentScope` bắt buộc và deprecate raw `findAll()/count()/findTop*()` cho manager path.

### Khuyến nghị triển khai
- **Phase 1**: Dùng **Phương án B (Specification)** cho toàn bộ endpoint Manager.
- **Phase 2**: Bổ sung **Phương án C (AOP fail-fast)** trong dev/test profile, giới hạn trong package repository scoped để phát hiện quên scope.
- **Phase 3**: Nếu cần global transparent filter, thêm **Phương án A (Hibernate Filter)** cho read-only path.

### 4.4. Scope Resolver bắt buộc
Tạo một service trung tâm, ví dụ `DepartmentScopeResolver`, trả về object bất biến:
```java
public record DepartmentScope(
    boolean admin,
    boolean student,
    Long studentId,
    Set<Long> departmentIds
) {}
```

Quy tắc:
- `ADMIN`: `admin=true`, không giới hạn department.
- `MANAGER`: `departmentIds` lấy từ `user_departments` qua DB server-side ở giai đoạn đầu, không lấy từ JWT.
- `STUDENT`: chỉ self-scope theo `studentId`, không dùng `dept_ids` để mở rộng quyền.
- Không truyền raw `departmentId` từ request xuống service như nguồn sự thật. Với MANAGER, request param `departmentId` chỉ được dùng để thu hẹp trong `departmentIds`, không được mở rộng.
- Nếu MANAGER chưa được gán khoa (`departmentIds` rỗng), trả 403 với message "Manager chưa được phân công Khoa" ngay tại resolver/filter boundary. Không để `IN (:empty)` xuống repository.
- Các method async/job/export phải nhận `DepartmentScope` hoặc snapshot scope explicit làm parameter. Không dựa vào `ThreadLocal` qua `@Async`, scheduler, event listener, hoặc job queue.
- Resolver/filter nên resolve lazy hoặc early-return cho non-MANAGER/non-ADMIN request để tránh query scope không cần thiết trên public/student flow.

---

## 5. Lớp 3: Authorization Layer (API / Controller)

### 5.1. Custom Security Expression
```java
@PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @departmentAuth.isManagerOf(#activityId, authentication))")
@GetMapping("/api/activities/{activityId}")
public ResponseEntity<?> getActivity(@PathVariable Long activityId) { ... }
```
`departmentAuth.isManagerOf(activityId, auth)`:
1. Resolve `DepartmentScope` từ DB server-side hoặc distributed cache nếu sau này bật cache.
2. Query `SELECT 1 FROM activity_departments WHERE activity_id = ? AND department_id IN (?)`.
3. Trả về `true` nếu match.

Không dùng cả `@PreAuthorize` DB lookup và repository scope cho cùng một read path nếu không cần. Quy tắc:
- Endpoint một resource nhạy cảm (`PUT/DELETE /activities/{id}`): dùng `@PreAuthorize` hoặc service guard để fail sớm.
- Endpoint list/page/report/export: dùng scoped repository query, không chạy N+1 `@PreAuthorize`.
- Nếu dùng cả hai vì defense-in-depth cho write action, phải chấp nhận overhead và có test performance.

### 5.2. Request Scope Context
Một filter chạy sau JWT filter có thể resolve scope và attach vào request/security details:
```java
@Component
public class DepartmentContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        if (auth is MANAGER) {
            DepartmentScope scope = departmentScopeResolver.resolve(auth);
            if (scope.departmentIds().isEmpty()) throw new AccessDeniedException(...);
            request.setAttribute("departmentScope", scope);
        } else if (auth is ADMIN) {
            request.setAttribute("departmentScope", DepartmentScope.admin());
        }
        try {
            filterChain.doFilter(...);
        } finally {
            DepartmentRequestScope.clearIfThreadLocalIsUsed();
        }
    }
}
```
- Request attribute hoặc explicit method parameter được ưu tiên hơn ThreadLocal.
- Nếu vẫn dùng ThreadLocal cho code đồng bộ, bắt buộc `clear()` trong `finally`.
- Không dùng ThreadLocal trong `@Async`, scheduler, event listener, background jobs. Các flow đó phải nhận `DepartmentScopeSnapshot` explicit.

### 5.3. Security Config cập nhật
Thêm `DepartmentContextFilter` vào chain **sau** `JwtAuthenticationFilter`:
```java
.addFilterAfter(new DepartmentContextFilter(), JwtAuthenticationFilter.class)
```

### 5.4. SecurityConfig endpoint audit bắt buộc
Trước Phase 2 phải tạo bảng audit tất cả endpoint hiện tại:

| Endpoint/Pattern | Hiện trạng | Quyết định scope |
|---|---|---|
| `/api/upload/**` | `permitAll()` | Không để public upload cho file nghiệp vụ quản trị. Tách public upload nếu cần; upload manager phải auth và gắn owner/scope vào metadata file. |
| `/api/departments/**` | public GET | GET public được phép; POST/PUT/DELETE department chỉ ADMIN. |
| `/api/admin/departments/**` | ADMIN/MANAGER | Đổi thành ADMIN-only cho CRUD khoa. |
| `/api/admin/users/**` | ADMIN/MANAGER | MANAGER chỉ quản lý student/user thuộc `dept_ids`; phân quyền manager/department assignment chỉ ADMIN. |
| `/api/participations` | `permitAll()` | Không public danh sách participation toàn trường; cần authenticated + scope hoặc chỉ public aggregate đã anonymize. |
| `/api/students/**` | ADMIN/MANAGER | MANAGER scope theo student department/lớp. |
| `/api/minigames/**` | ADMIN/MANAGER | Scope như activity; `MinigameActivityController` phải guard create/update/delete. |
| `/api/series/**` | ADMIN/MANAGER cho write | Scope theo owner departments/child activity organizers. |
| `/api/statistics/**` | role-based | Mọi metric manager phải scoped; ADMIN bypass có test riêng. |

---

## 6. Lớp 4: Service & Controller Adaptation

### 6.1. Activity Service
- `createActivity`: Nếu MANAGER tạo, tự động gán Khoa của manager vào `organizers` (nếu request không gửi hoặc gửi Khoa khác → reject).
- `getActivityById`: ADMIN → ok. MANAGER → kiểm tra `activity.organizers` intersect `dept_ids`.
- `listActivities`: ADMIN → `findAll()`. MANAGER → `findAll(Specification.where(DepartmentScopeSpec.forManager(deptIds)))`.
- `update/delete`: Giống `getActivityById`, thêm ownership check.
- `StandardActivityController`, `MinigameActivityController`, `ActivitySeriesController` đều phải dùng cùng `DepartmentScope` guard. Không chỉ sửa `ActivityController`.
- Activity nhiều khoa đồng tổ chức:
  - MANAGER được update metadata cơ bản nếu khoa mình là organizer.
  - DELETE/PUBLISH/UNPUBLISH activity nhiều khoa chỉ ADMIN, trừ khi sau này có workflow approval đa khoa.
  - Khi manager tạo activity, `organizerIds` phải là subset của `dept_ids`; không auto thêm khoa khác.

### 6.2. Student Service
- `listStudents`: MANAGER chỉ thấy `Student` có `department_id IN dept_ids`.
- `getStudentById`: MANAGER chỉ thấy nếu `student.department_id IN dept_ids`.
- `createStudent` / bulk import: MANAGER chỉ được tạo sinh viên vào khoa mình. Nếu request có `departmentId` ngoài `dept_ids` → 403. Nếu manager chỉ quản lý 1 khoa và request không gửi `departmentId`, tự gán khoa đó.
- `assignStudentToClass`: lớp đích phải thuộc khoa manager quản lý. Khi gán lớp, cập nhật đồng bộ `Student.department = StudentClass.department`.
- `moveStudentDepartment`: chỉ ADMIN được chuyển sinh viên giữa 2 khoa khác nhau. MANAGER chỉ được cập nhật thông tin sinh viên đang thuộc khoa mình, không được “kéo” sinh viên từ khoa khác vào khoa mình nếu chưa có quy trình duyệt.
- **Lưu ý**: Sinh viên không có lớp / không có Khoa → chỉ ADMIN thấy.

### 6.3. Registration & Participation
- `getRegistrationsByActivity(activityId)`: MANAGER chỉ gọi được nếu activity thuộc Khoa mình.
- Policy mặc định an toàn: Manager Khoa A chỉ thấy registration của sinh viên Khoa A, dù activity có nhiều khoa đồng tổ chức.
- `approve/reject/cancelRegistration`: MANAGER chỉ được xử lý registration nếu activity thuộc khoa mình **và** registration nằm trong policy recipient đã chọn. Khuyến nghị an toàn: chỉ duyệt sinh viên có department thuộc `dept_ids`.
- `checkIn/QR check-in`: nếu MANAGER hoặc staff check-in, phải kiểm tra activity thuộc khoa manager hoặc user là organizer/prep supervisor hợp lệ. STUDENT tự check-in vẫn self-scope theo ticket/user.
- `registration report/export`: dùng cùng scope với list registration. Không được export toàn bộ participants nếu UI đang chỉ hiển thị sinh viên thuộc khoa mình.
- `backfill/auto-register`: MANAGER không được chạy backfill toàn trường. Nếu activity thuộc khoa mình, chỉ auto-register sinh viên thuộc `dept_ids`, trừ khi ADMIN chạy job toàn trường.
- `activityId` không thuộc scope manager nên trả 404 hoặc 403 nhất quán để tránh leak existence.

### 6.4. Bài nộp / Submission
- `getSubmissionsByTask(taskId)`: MANAGER chỉ xem nếu task thuộc activity có organizer giao với `dept_ids`.
- Policy mặc định an toàn: với activity nhiều khoa, manager chỉ thấy bài nộp của sinh viên thuộc `dept_ids`.
- `getSubmissionById`, `downloadAttachment`, `gradeSubmission`: phải scope theo cả activity của task và student owner của submission.
- `gradeSubmission`: MANAGER chỉ chấm bài cho sinh viên thuộc khoa mình theo policy an toàn; nếu chấm toàn event chung thì cần ghi audit và hiển thị rõ trong UI.
- STUDENT chỉ được tạo/sửa/xóa bài nộp của chính mình trước deadline; không dùng department scope để mở rộng quyền.
- File storage/download endpoint phải gọi authorization trước khi stream file, tránh bypass qua URL trực tiếp.

### 6.5. Phân công nhiệm vụ sự kiện / Task Assignment
- `createTask/updateTask/deleteTask`: MANAGER chỉ thao tác task của activity thuộc khoa mình.
- `assignTaskToStudent`, `assignTaskToRegisteredStudents`: assignee phải là sinh viên thuộc `dept_ids` hoặc thuộc registration scope của activity theo policy đã chọn.
- Không cho MANAGER khoa A phân công sinh viên khoa B vào task, kể cả activity đồng tổ chức. Cross-department assignment cần ADMIN.
- `getRegisteredStudentsForActivity`: phải filter theo registration policy. Đây là endpoint nhạy cảm vì có thể leak danh sách sinh viên tham gia.
- `getAssignmentsByActivityAndStudent`, `getStudentTasks(studentId)`: MANAGER chỉ xem nếu student thuộc `dept_ids`; STUDENT chỉ xem task của chính mình.
- Task assignment hàng loạt cần trả về `rejectedStudentIds` cho các sinh viên ngoài scope thay vì âm thầm assign.

### 6.6. Task / Submission / Score Cascade
- `getTasksByActivity`, `getSubmissionsByTask`: Cascade theo Activity scope và student scope.
- `getScoresByStudent`: MANAGER chỉ xem score của sinh viên thuộc Khoa mình.
- `gradeParticipation`: MANAGER chỉ grade activity thuộc Khoa mình và student/registration trong scope.

### 6.7. Preparation / Tài chính chuẩn bị
Preparation hiện có quyền theo organizer/prep supervisor, nhưng các biểu thức kiểu `hasAnyRole('ADMIN','MANAGER')` phải được thay bằng department-aware checks:
- `createPreparationTask`, `listTasksByActivity`, `dashboard(activityId)`: MANAGER chỉ được thao tác nếu activity có `organizers` giao với `dept_ids`.
- `allocateTaskAmount`, `createFundAdvance`, `approveExpense`, `approveAllocationAdjustment`: scope theo activity của task/expense/fund advance. MANAGER khoa A không được duyệt chi phí của activity chỉ thuộc khoa B.
- `export` và báo cáo tài chính preparation: mọi query phải join từ `activity_id -> activity_departments` và filter `department_id IN (:deptIds)` cho MANAGER.
- Quyền `prepSupervisor` theo sinh viên vẫn giữ cho workflow nội bộ, nhưng không thay thế quyền manager theo khoa.

Business rule khuyến nghị:
- Activity nhiều khoa đồng tổ chức: manager của khoa đồng tổ chức được xem preparation chung.
- Duyệt/xóa ngân sách hoặc khoản chi của activity nhiều khoa: chỉ ADMIN hoặc manager của khoa owner ngân sách. Nếu chưa có owner ngân sách, cần thêm `budget_owner_department_id`.

### 6.8. Quản lý điểm số
- `ScoreController`, `ScoreEntryService`, `ScoreRuleEngine`, recalculation jobs phải nhận `DepartmentScope`.
- MANAGER chỉ xem/sửa/recalculate điểm của sinh viên thuộc `dept_ids`.
- Rule theo activity: manager chỉ cấu hình score rules cho activity thuộc khoa mình.
- Rule theo department audience:
  - Nếu activity chỉ có một organizer hoặc manager đang sửa activity của khoa mình, `targetDepartmentIds` phải nằm trong tập organizer departments của activity, không nhất thiết chỉ nằm trong `dept_ids`.
  - MANAGER chỉ được tạo/sửa score rule khi activity có organizer giao với scope của manager.
  - Cho phép target khoa/phòng ban khác nếu các đơn vị đó là đồng tổ chức của sự kiện. Không cho target department không phải organizer của activity, trừ ADMIN.
- `ActivityScoreRuleServiceImpl` phải validate `targetDepartments` khi create/update rule; không để request tự do ảnh hưởng điểm sinh viên của đơn vị không liên quan sự kiện.
- Ranking và score history: MANAGER chỉ thấy sinh viên thuộc khoa mình; STUDENT chỉ thấy chính mình; ADMIN thấy toàn bộ hoặc filter theo khoa.
- Recalculation async/job phải lưu scope người khởi tạo. Không được chạy job toàn trường nếu requester là MANAGER.
- `RecalculationJobService` phải nhận `DepartmentScopeSnapshot` từ REST trigger endpoint. Scheduled/system jobs không có requester thì chạy với ADMIN/system scope và phải log audit rõ `triggeredBy=SYSTEM`.

### 6.9. Email
- `EmailServiceImpl.getRecipients()` và `getRecipientsForNotification()` phải nhận `DepartmentScope` và validate từng `RecipientType` trước khi resolve danh sách recipient.
- `ALL_STUDENTS`: chỉ ADMIN.
- `BY_DEPARTMENT`: MANAGER chỉ được gửi tới department nằm trong scope của mình.
- `BY_CLASS`: MANAGER chỉ được gửi tới class có `StudentClass.department_id` nằm trong scope của mình.
- `BULK`: MANAGER chỉ được gửi tới user/student thuộc scope của mình, trừ recipient là participant của một activity mà manager có quyền gửi theo activity recipient policy.
- `ACTIVITY_REGISTRATIONS`: nếu manager thuộc một organizer của activity, được gửi tới **toàn bộ sinh viên đã đăng ký activity**, kể cả ngoài khoa/phòng ban của manager, vì sự kiện có thể do nhiều đơn vị đồng tổ chức. Bắt buộc audit campaign: activityId, requester, requester scope, total recipients, departments represented.
- `SERIES_REGISTRATIONS`: áp cùng rule như `ACTIVITY_REGISTRATIONS`, nhưng scope theo series owner/child activities.
- `email_history`: MANAGER chỉ xem lịch sử email do mình/khoa-phòng ban mình gửi, hoặc campaign gắn activity/series mà đơn vị của manager là organizer.
- Không cho MANAGER gửi email broadcast toàn trường, gửi theo role `ALL_STUDENTS`, hoặc gửi tới department/class ngoài scope nếu không thông qua activity/series registration policy hợp lệ.

### 6.10. Thông báo
- Notification campaign phải có `target_department_ids` hoặc target activity/series.
- MANAGER chỉ tạo notification cho khoa mình, lớp thuộc khoa mình, hoặc participant của activity thuộc khoa mình theo policy recipient.
- `GET /api/notifications/**` cho người nhận vẫn theo user hiện tại; admin/manager dashboard quản trị notification phải filter theo `sender_department_id`/`target_department_ids`.
- Push token/device token vẫn self-scope theo user, không mở rộng theo department.

### 6.11. Quản lý bài viết
- Public article read không department-scope, giống public activity read.
- Admin article management phải thêm owner scope:
  - ADMIN tạo/sửa/xóa mọi bài.
  - MANAGER tạo bài với `owner_department_id` thuộc `dept_ids`.
  - MANAGER chỉ sửa/xóa bài do khoa mình sở hữu.
- Nếu bài viết liên kết activity/series, activity/series đó phải thuộc khoa manager.
- Featured/trending public vẫn toàn trường; dashboard quản trị bài viết của manager chỉ tính bài thuộc khoa mình.

### 6.12. Export / Report
- Mọi endpoint export CSV/Excel/PDF phải dùng cùng query scoped với màn hình danh sách.
- Không được export toàn trường từ một service method dùng chung nếu caller là MANAGER.
- File export nên ghi audit: requester, role, `dept_ids`, filter, số dòng.

---

## 7. Dashboard & Statistics (Department-Scoped Metrics)

### 7.1. Dashboard Controller hiện tại
Endpoint `/api/statistics/dashboard` trả về tổng toàn trường.

### 7.2. Giải pháp
- Thêm query parameter tùy chọn: `?departmentId=...` (ADMIN dùng để xem Khoa cụ thể).
- Nếu caller là MANAGER: **ignore** query param mở rộng quyền, force filter theo `DepartmentScope.departmentIds` từ DB/server-side scope.
- Viết **JPQL hoặc native query riêng** cho mỗi metric để scope đẩy xuống database. Ưu tiên JPQL khi có enum/soft-delete mapping phức tạp; nếu dùng native SQL phải include đầy đủ `is_deleted=false`, status filter, và join condition tương ứng.

```sql
-- Tổng số hoạt động theo Khoa (Manager scope)
SELECT COUNT(DISTINCT a.id)
FROM activities a
JOIN activity_departments ad ON a.id = ad.activity_id
WHERE a.is_deleted = false
  AND ad.department_id IN (:deptIds);

-- Tổng số sinh viên theo Khoa
SELECT COUNT(*) FROM students 
WHERE department_id IN (:deptIds) AND is_deleted = false;

-- Tổng số đăng ký theo Khoa (nếu policy là chỉ SV Khoa mình)
SELECT COUNT(*) FROM activity_registrations ar
JOIN students s ON ar.student_id = s.id
WHERE s.department_id IN (:deptIds);
```

### 7.3. Multi-Department Manager
Nếu Manager quản lý Khoa [1, 3], Dashboard hiển thị **aggregated** cả 2 Khoa (group by department) hoặc tổng chung (tùy UI). Khuyến nghị trả về `Map<departmentId, metric>` để frontend vẽ biểu đồ so sánh.

### 7.4. Coverage bắt buộc cho thống kê
Các thống kê cần scope riêng, không dùng count/top query toàn trường:

| Nhóm metric | Scope cho MANAGER |
|---|---|
| Activity/Series/MiniGame | Activity có organizer thuộc `dept_ids`; series scope theo child activities hoặc owner departments |
| Registration/Participation | Theo policy: sinh viên thuộc khoa mình hoặc toàn event chung nếu khoa là organizer |
| Submission/Assignment | Join submission/assignment → task → activity → `activity_departments`, sau đó filter student department theo policy |
| Student | `students.department_id IN (:deptIds)` và `is_deleted=false` |
| Score/Ranking | Sinh viên thuộc `dept_ids`, ưu tiên snapshot department ở thời điểm phát sinh điểm |
| Preparation finance | Join task/expense/fund advance → activity → `activity_departments` |
| Email/Notification | `sender_department_id IN (:deptIds)` hoặc `target_department_ids` giao `dept_ids` |
| Article | `owner_department_id IN (:deptIds)` cho dashboard quản trị; public article không scope |

---

## 8. Lộ trình Triển khai (Migration Path)

### Phase 1: Schema & Auth Foundation (Tuần 1)
1. Tạo bảng `user_departments`.
2. Tạo `DepartmentScopeResolver` đọc `user_departments` trực tiếp từ DB bằng indexed query. Không thêm `dept_ids` vào JWT làm nguồn sự thật; chưa dùng cache ở giai đoạn đầu.
3. Tạo request-scope filter/context và quy định async phải nhận `DepartmentScopeSnapshot` explicit.
4. Seed data: Gán MANAGER hiện tại vào Khoa mặc định nếu biết chắc; nếu chưa gán thì manager vào endpoint quản trị phải nhận 403 "Manager chưa được phân công Khoa".
5. Thêm feature flag `department.scope.enforcement.enabled=false` mặc định khi deploy schema đầu tiên.
6. Backfill snapshot department cho registration/participation/score theo policy data cũ.

### Phase 2: Repository Scope Wrapper (Tuần 2)
1. Hoàn tất SecurityConfig endpoint audit table: upload, participations, admin users/departments, students, minigames, series, statistics.
2. Tạo `DepartmentScopeSpec` cho các entity chính: Activity, Student, ActivityRegistration, ActivityParticipation, ActivityTask, TaskAssignment, TaskSubmission, ScoreEntry, PreparationTask, Expense, FundAdvance, EventArticle, EmailHistory, Notification.
3. Wrap các repository nhạy cảm: vô hiệu hóa các method `findAll()/count()/findTop*()` raw đối với MANAGER nếu không có scoped method tương ứng.
4. Viết test integration: Manager gọi API → verify chỉ thấy đúng Khoa.

### Phase 3: Service & Controller gắn Scope (Tuần 3)
1. Sửa `ActivityServiceImpl`: tất cả read/write route qua scope check.
2. Sửa `StudentServiceImpl`: list/get scope theo Khoa.
3. Sửa `StatisticsServiceImpl`: dashboard query scoped.
4. Sửa `Registration/Participation/Task/Assignment/Submission/Score` controllers: cascade check.
5. Sửa `Preparation/PreparationFinance/PreparationExport`: mọi action manager phải scope theo activity departments.
6. Sửa `Email/Notification/EventArticle`: campaign/content/audience/history phải có owner/target department scope; `EmailServiceImpl.getRecipients()` và `getRecipientsForNotification()` validate từng `RecipientType`.

### Phase 4: Hardening & Audit (Tuần 4)
1. Thêm **audit log** cho hành động cross-department: nếu ADMIN xem/sửa Khoa khác, log lại.
2. Penetration test: thử dùng token Manager gọi API của Khoa khác (must fail 403 hoặc empty).
3. Review tất cả `@PreAuthorize` để đảm bảo không có endpoint bypass.
4. Performance test: verify index trên `activity_departments(department_id, activity_id)` và `students(department_id)`.

### Rollback / Feature Flag
- `department.scope.enforcement.enabled`: bật/tắt enforcement trong service/repository guard.
- `department.scope.auditOnly`: log violation nhưng chưa block trong giai đoạn dry-run.
- Schema migration phải backward-compatible: thêm bảng/cột nullable trước, backfill sau, chỉ đặt constraint khi query mới đã chạy ổn.
- Nếu Phase 2/3 lỗi, rollback code có thể chạy với schema mới mà không phá login/public flow.
- Không deploy trạng thái "manager vẫn thấy toàn trường" quá lâu: audit-only window phải có deadline và dashboard theo dõi violations.

---

## 9. Các Điểm Cần Định rõ Business Rule

| # | Câu hỏi | Khuyến nghị |
|---|---|---|
| 1 | **Manager có thể tạo Activity cho Khoa khác không?** | Không. Tự động gán Khoa manager vào `organizers`. Nếu request gửi `organizers` khác → validate reject. |
| 2 | **Activity do 2 Khoa đồng tổ chức, Manager Khoa A thấy registration của SV Khoa B không?** | Chọn policy an toàn: chỉ thấy registration của SV thuộc Khoa A. Toàn event chung chỉ ADMIN hoặc workflow riêng có audit. |
| 3 | **STUDENT xem Activity có bị scope không?** | Không. Student xem toàn bộ Activity công khai (`isDraft=false`) như hiện tại. Scope chỉ áp với MANAGER. |
| 4 | **ADMIN có cần chọn Khoa để xem Dashboard không?** | Nên có bộ lọc Khoa trên UI. API hỗ trợ `?departmentId=`. Nếu không truyền, ADMIN thấy toàn trường. |
| 5 | **MANAGER có quyền xóa Activity không?** | Activity một khoa: có nếu khoa mình là organizer. Activity nhiều khoa: chỉ ADMIN được xóa/publish/unpublish cho tới khi có workflow đồng thuận đa khoa. |
| 6 | **Nếu Student chuyển Khoa, lịch sử điểm/registration cũ thuộc Khoa nào?** | History giữ nguyên Khoa tại thời điểm tham gia (denormalize `student_department_id` vào `activity_registrations` và `student_scores` để tránh thay đổi retrospectively). |
| 7 | **Manager có được thêm sinh viên vào khoa mình không?** | Có. Chỉ được tạo/cập nhật sinh viên với `department_id` nằm trong scope khoa của manager; nếu thêm vào lớp thì lớp phải thuộc khoa mình. Không được chuyển sinh viên từ khoa khác nếu không có quyền ADMIN hoặc workflow duyệt. |
| 8 | **Manager có được gửi email/thông báo toàn trường không?** | Không broadcast toàn trường. Manager chỉ gửi tới đơn vị/lớp trong scope, hoặc toàn bộ sinh viên đã đăng ký activity/series nếu đơn vị của manager là organizer của activity/series đó. |
| 9 | **Manager có được quản lý bài viết không?** | Có, nhưng bài phải có `owner_department_id` thuộc khoa manager. Public read vẫn toàn trường. |
| 10 | **Preparation finance của activity nhiều khoa ai được duyệt?** | Cần owner ngân sách. Khuyến nghị thêm `budget_owner_department_id`; nếu chưa có thì chỉ ADMIN duyệt action tài chính nhạy cảm. |
| 11 | **Manager khoa A có được xem/chấm bài nộp của sinh viên khoa B trong activity đồng tổ chức không?** | Không theo mặc định. Chỉ xem/chấm sinh viên thuộc scope đơn vị mình. Email thông báo theo danh sách đăng ký là ngoại lệ riêng và phải audit. |
| 12 | **Manager có được phân công task cho sinh viên khoa khác không?** | Không theo mặc định. Chỉ assign sinh viên thuộc scope khoa của manager; activity chung muốn assign cross-department cần ADMIN hoặc policy riêng. |
| 13 | **Endpoint danh sách sinh viên đã đăng ký cho task assignment hiển thị ai?** | Theo cùng registration policy: an toàn là chỉ sinh viên thuộc khoa manager; không trả toàn bộ danh sách nếu manager không có quyền xem toàn event. |

---

## 10. Security Checklist (Bắt buộc trước khi deploy)

- [ ] JWT không chứa department scope làm nguồn sự thật; Phase đầu `DepartmentScopeResolver` đọc DB-only bằng indexed query.
- [ ] Nếu thêm cache sau này, phải dùng Redis/distributed invalidation; không dùng in-process cache đơn độc trong multi-pod.
- [ ] ADMIN bypass scope phải được log audit.
- [ ] ADMIN gọi các endpoint manager-specific vẫn thấy full data khi không truyền filter khoa; không bị khóa bởi `departmentIds` rỗng.
- [ ] MANAGER chưa được gán khoa nhận 403 "Manager chưa được phân công Khoa"; không có query `IN (:empty)`.
- [ ] SecurityConfig endpoint audit đã xử lý `/api/upload/**`, `/api/participations`, `/api/admin/users/**`, `/api/admin/departments/**`, `/api/students/**`, `/api/minigames/**`, `/api/series/**`.
- [ ] Không có endpoint nào dùng `.findAll()` raw mà không qua Spec khi caller là MANAGER.
- [ ] Activity ManyToMany organizer specs dùng `distinct(true)` hoặc `EXISTS` subquery và có test pagination/count không duplicate.
- [ ] Tất cả `DELETE/PUT/POST` có path variable (activityId, studentId...) đều có `@PreAuthorize` kiểm tra ownership.
- [ ] Tất cả endpoint preparation/finance/export dùng activity department scope, không chỉ `hasAnyRole('ADMIN','MANAGER')`.
- [ ] Registration approval/check-in/report/export kiểm tra activity scope và student department scope theo policy.
- [ ] Task assignment không cho MANAGER assign hoặc xem assignee ngoài scope khoa, trừ policy event chung được audit.
- [ ] Submission list/detail/download/grade kiểm tra task → activity scope và student owner scope trước khi trả metadata/file.
- [ ] Tất cả endpoint score/ranking/recalculate kiểm tra student department scope và lưu requester scope cho async jobs.
- [ ] Email/notification không cho MANAGER broadcast toàn trường; `getRecipients()` và `getRecipientsForNotification()` validate `ALL_STUDENTS`, `BY_DEPARTMENT`, `BY_CLASS`, `BULK`, `ACTIVITY_REGISTRATIONS`, `SERIES_REGISTRATIONS` theo scope/policy.
- [ ] `ACTIVITY_REGISTRATIONS` và `SERIES_REGISTRATIONS`: MANAGER là organizer được gửi tới toàn bộ người đăng ký, kể cả ngoài scope đơn vị, và campaign phải audit departments represented.
- [ ] `ActivityScoreRuleServiceImpl` validate `targetDepartments`: MANAGER chỉ target departments là organizers của activity; cho phép target organizer ngoài scope manager khi đó là sự kiện đồng tổ chức.
- [ ] Article admin CRUD kiểm tra `owner_department_id`; public read không bị ảnh hưởng.
- [ ] Manager tạo/import/gán lớp sinh viên chỉ trong scope khoa; lớp đích và student department luôn đồng bộ.
- [ ] SQL Injection không thể qua department id list (dùng `IN` parameter binding, không nối chuỗi).
- [ ] JPQL/native statistics query include đầy đủ soft-delete/status filters; native SQL không bypass Hibernate enum/soft-delete mapping.
- [ ] Feature flags `department.scope.enforcement.enabled` và `department.scope.auditOnly` có rollback path rõ.
- [ ] Security integration tests bật `department.scope.enforcement.enabled=true` explicit bằng test properties; không phụ thuộc default flag.
- [ ] Nếu dùng request ThreadLocal cho code đồng bộ, `clear()` được gọi trong `finally`; async/job nhận `DepartmentScopeSnapshot` explicit.
- [ ] Test case: Manager Khoa A gọi API với `activityId` của Khoa B → expect 403 hoặc 404 (không leak existence).
- [ ] Test case: Manager gọi `/api/activities` không có param → expect chỉ list Khoa mình.
- [ ] Test case: Manager Khoa A export preparation/score/email history của Khoa B → expect 403 hoặc empty.
- [ ] Test case: Manager Khoa A tạo sinh viên với `departmentId` Khoa B → expect 403.
- [ ] Test case: Manager Khoa A xem/chấm/download submission của sinh viên Khoa B → expect 403 hoặc empty.
- [ ] Test case: Manager Khoa A assign task cho sinh viên Khoa B → expect reject và không tạo assignment.
- [ ] Test case: Manager gửi email `BY_CLASS` tới lớp ngoài scope → expect 403.
- [ ] Test case: Manager gửi email `ACTIVITY_REGISTRATIONS` cho activity đồng tổ chức → gửi được toàn bộ registrants và ghi audit.
- [ ] Test case: Manager tạo/sửa score rule target department không phải organizer của activity → expect 403.
- [ ] Test case: Manager tạo/sửa score rule target department là organizer đồng tổ chức ngoài scope manager → expect allowed.
- [ ] Test case: ADMIN gọi dashboard/statistics/students/activities → expect full data hoặc filter theo `departmentId` nếu truyền.
- [ ] Index trên `user_departments(user_id)`, `activity_departments(department_id)`, `students(department_id)` đã tạo.

---

## 11. Tóm tắt Hướng đi Khuyến nghị

1. **Mô hình Authorization**: Role-Based + Attribute-Based (RBAC + ABAC). Không bỏ role cũ, bổ sung `department_id` như attribute.
2. **Triển khai cốt lõi**: 
   - `user_departments` table.
   - `DepartmentScopeResolver` đọc DB-only ở giai đoạn đầu; Redis/distributed cache chỉ thêm sau nếu cần hiệu năng.
   - Request-scope context cho code đồng bộ; `DepartmentScopeSnapshot` explicit cho async/job.
   - `JpaSpecification` scope cho mọi query MANAGER.
3. **Không để Service tự scope tùy hứng**: Scope phải đi qua scoped repository/service methods bắt buộc; AOP chỉ là backstop dev/test.
4. **Dashboard**: Viết query aggregation riêng, scoped theo `DepartmentScope.departmentIds`, trả về grouped theo department.
5. **Module coverage**: Scope bắt buộc cho Activity, Series/MiniGame, Registration/Participation, Task/Assignment/Submission, Preparation/Finance/Export, Student/Class, Score/Ranking/Recalculate, Statistics, Email, Notification, Article admin.
6. **Migration**: Expand-Contract. Tạo bảng mới, giữ nguyên `users` cũ. Seed dữ liệu mapping trước khi bật enforcement.
7. **Ưu tiên bảo mật**: Tất cả endpoint MANAGER phải pass penetration test cross-department trước khi merge.

---

*Đây là giải pháp kiến trúc. Khi được duyệt, mới tiến hành triển khai từng Phase với code cụ thể.*
