# Báo cáo tổng quan - Module Chuẩn bị Sự kiện (Preparation)

## 1. Mục tiêu
Module Chuẩn bị Sự kiện hỗ trợ:
- Bật/tắt chế độ chuẩn bị cho từng Activity (`hasPreparation`).
- Quản lý danh sách sinh viên thuộc BTC (Organizer) theo từng Activity.
- Giao việc chuẩn bị theo từng Activity (PreparationTask) và cho phép người được giao tự cập nhật trạng thái.
- Quản lý tài chính theo mô hình ngân sách theo hạng mục và duyệt chi phí 2 cấp (Leader → Admin/Manager).

## 1.1. Cập nhật mới (Tài chính v2 - Ngân sách theo hạng mục + duyệt 2 cấp)

### 1.1.1. Tóm tắt thay đổi
- Thay mô hình `Budget (1-1 Activity) + Expense.approved (Boolean|null)` bằng `ActivityBudget + BudgetCategory + ExpenseStatus`.
- Bổ sung cơ chế `FundAdvance` (tạm ứng) và tự động trừ tạm ứng khi Expense được duyệt cấp cuối.
- Bổ sung `AuditLog` để lưu mọi thay đổi liên quan tài chính.
- Nâng cấp `PreparationTask` để hỗ trợ Leader/Member và kiểm soát hạn mức chi theo Task.

### 1.1.2. Entity/Enum mới và Entity đã nâng cấp

#### ActivityBudget (1-1 Activity)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "activity_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", unique = true, nullable = false)
    private Activity activity;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "activityBudget", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BudgetCategory> categories = new LinkedHashSet<>();

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### BudgetCategory (hạng mục ngân sách)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_categories", uniqueConstraints = @UniqueConstraint(columnNames = { "activity_budget_id", "name" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BudgetCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_budget_id", nullable = false)
    private ActivityBudget activityBudget;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "used_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal usedAmount = BigDecimal.ZERO;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### PreparationTask (Task) - nâng cấp (Leader + hạn mức)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PreparationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Student owner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Column(precision = 19, scale = 2)
    private BigDecimal budgetLimit;

    @Column(precision = 19, scale = 2)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean isFinancial = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreparationTaskStatus status = PreparationTaskStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### PreparationTaskMember (Member trong Task)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_task_members", uniqueConstraints = @UniqueConstraint(columnNames = { "task_id", "student_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PreparationTaskMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private PreparationTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### FundAdvance (tạm ứng)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.FundAdvanceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_advances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FundAdvance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private PreparationTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "remaining_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundAdvanceStatus status = FundAdvanceStatus.HOLDING;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### Expense (chi phí) - bảng mới `preparation_expenses`
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.ExpenseStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private PreparationTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private BudgetCategory category;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String evidenceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Student createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseStatus status = ExpenseStatus.PENDING_LEADER;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### AuditLog (ghi nhận thay đổi tài chính)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 50)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

#### Enum trạng thái
```java
package vn.campuslife.enumeration;

public enum ExpenseStatus {
    PENDING_LEADER,
    PENDING_ADMIN,
    APPROVED,
    REJECTED
}
```

```java
package vn.campuslife.enumeration;

public enum FundAdvanceStatus {
    HOLDING,
    SETTLED
}
```

### 1.1.3. Phân quyền và luồng duyệt 2 cấp
- MEMBER (thuộc Task): tạo Expense + upload evidence
- LEADER (owner của Task): duyệt cấp 1 (`PENDING_LEADER → PENDING_ADMIN` hoặc `REJECTED`)
- ADMIN/MANAGER: duyệt cấp cuối (`PENDING_ADMIN → APPROVED/REJECTED`)

Khi APPROVED cấp cuối (trong 1 transaction):
- Trừ `FundAdvance.remainingAmount` theo task + member (FIFO theo thời gian tạo)
- Cộng `BudgetCategory.usedAmount`
- Ghi `AuditLog`
- Gửi Notification:
  - Có expense chờ duyệt → notify đúng role (LEADER/ADMIN)
  - Ngân sách sắp cạn (<= 10% category) → notify LEADER + ADMIN

### 1.1.4. API v2 (tài chính)
Các endpoint chính:
- `PUT /api/preparation/activities/{activityId}/budget` (ADMIN/MANAGER)
- `PUT /api/preparation/tasks/{taskId}/allocation` (ADMIN/MANAGER)
- `POST /api/preparation/tasks/{taskId}/fund-advances` (ADMIN/MANAGER)
- `POST /api/preparation/tasks/{taskId}/members/{studentId}` (ADMIN/MANAGER hoặc LEADER)
- `POST /api/preparation/tasks/{taskId}/expenses/evidence` (MEMBER)
- `POST /api/preparation/tasks/{taskId}/expenses` (MEMBER)
- `PUT /api/preparation/expenses/{expenseId}/leader-decision` (LEADER)
- `PUT /api/preparation/expenses/{expenseId}/admin-decision` (ADMIN/MANAGER)
- `GET /api/preparation/activities/{activityId}/expenses?status=...` (ADMIN/MANAGER hoặc Organizer)
- `GET /api/preparation/activities/{activityId}/financial-report` (ADMIN/MANAGER hoặc Organizer)

## 2. Entity & quan hệ dữ liệu

### 2.1. Activity
- Bổ sung `hasPreparation` (boolean).
- Khi `hasPreparation = false`, các API dashboard Preparation sẽ trả lỗi FeatureNotEnabled.

Mã nguồn `Activity.java`:

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.Comment;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Activity {

    /** Khóa chính (tự tăng). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Khóa chính")
    private Long id;

    /** Loại hoạt động (TRAINING, BUSINESS, SOCIAL, ...). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Comment("Loại hoạt động (enum) - null nếu thuộc series")
    private ActivityType type;

    /** Kiểu tính điểm cho hoạt động (tham gia, nộp minh chứng, sản phẩm, ...). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @Comment("Kiểu tính điểm - null nếu thuộc series (lấy từ series)")
    private ScoreType scoreType;

    /** Tên hoạt động hiển thị cho sinh viên. */
    @Column(nullable = false)
    @Comment("Tên hoạt động")
    private String name;

    /** Mô tả chi tiết hoạt động. */
    @Column(columnDefinition = "TEXT")
    @Comment("Mô tả chi tiết")
    private String description;

    /** Ngày bắt đầu. */
    @Comment("Ngày bắt đầu")
    private LocalDateTime startDate;

    /** Ngày kết thúc. */
    @Comment("Ngày kết thúc")
    private LocalDateTime endDate;

    /** Có yêu cầu nộp minh chứng/báo cáo sau khi tham gia hay không. */
    @Column(nullable = false)
    @Comment("Yêu cầu nộp minh chứng")
    private boolean requiresSubmission = false;

    @Column(nullable = false)
    private boolean hasPreparation = false;

    /** Điểm tối đa sinh viên có thể đạt được. */
    @Comment("Điểm tối đa")
    private BigDecimal maxPoints;

    /** Ngày mở đăng ký tham gia. */
    @Comment("Ngày bắt đầu đăng ký")
    private LocalDateTime registrationStartDate;

    /** Hạn cuối đăng ký tham gia. */
    @Comment("Hạn chót đăng ký")
    private LocalDateTime registrationDeadline;

    /** Đường dẫn chia sẻ hoạt động. */
    @Comment("Link chia sẻ")
    private String shareLink;

    /** Đánh dấu hoạt động quan trọng/ưu tiên. */
    @Column(nullable = false)
    @Comment("Hoạt động quan trọng")
    private boolean isImportant = false;

    /** Bản nháp (true = chưa công bố). */
    @Column(nullable = false)
    @Comment("Bản nháp")
    private boolean isDraft = true;

    /** Đường dẫn ảnh banner. */
    @Comment("Ảnh banner")
    private String bannerUrl;

    /** Địa điểm tổ chức (có thể là 'Online'). */
    @Comment("Địa điểm tổ chức")
    private String location;

    /** Trạng thái xóa mềm (true = đã xóa logic). */
    @Column(nullable = false)
    @Comment("Cờ xóa mềm")
    private boolean isDeleted = false;

    /** ID chuỗi sự kiện (null = sự kiện đơn lẻ). */
    @Column
    @Comment("ID chuỗi sự kiện")
    private Long seriesId;

    /** Thứ tự trong chuỗi (1, 2, 3...). */
    @Column
    @Comment("Thứ tự trong chuỗi")
    private Integer seriesOrder;

    /** Số lượng vé/slot có thể đăng ký (null = không giới hạn). */
    @Comment("Số lượng vé (null = không giới hạn)")
    private Integer ticketQuantity;

    /** Quyền lợi khi tham gia (vd: chứng nhận, quà tặng). */
    @Column(columnDefinition = "TEXT")
    @Comment("Quyền lợi khi tham gia")
    private String benefits;

    /** Yêu cầu đối với người tham gia (điều kiện, chuẩn bị). */
    @Column(columnDefinition = "TEXT")
    @Comment("Yêu cầu tham gia")
    private String requirements;

    /** Thông tin liên hệ hỗ trợ (email/số điện thoại). */
    @Comment("Thông tin liên hệ")
    private String contactInfo;

    /** Mã QR code unique để check-in nhanh. */
    @Column(name = "check_in_code", length = 50, unique = true)
    @Comment("Mã QR code unique để check-in nhanh")
    private String checkInCode;

    /** Đăng ký có cần duyệt hay không. */
    @Column(nullable = false)
    @Comment("Cần duyệt đăng ký")
    private boolean requiresApproval = true;

    /** Có bắt buộc cho sinh viên thuộc khoa tham gia hay không. */
    @Column(nullable = false)
    @Comment("Bắt buộc cho sinh viên thuộc khoa")
    private boolean mandatoryForFacultyStudents = false;

    /** Điểm trừ khi tham gia nhưng không hoàn thành yêu cầu. */
    @Column(precision = 10, scale = 2)
    @Comment("Điểm trừ khi không hoàn thành")
    private BigDecimal penaltyPointsIncomplete;

    /** Danh sách đơn vị tổ chức (nhiều Department cho 1 Activity). */
    @ManyToMany
    @JoinTable(name = "activity_departments", joinColumns = @JoinColumn(name = "activity_id"), inverseJoinColumns = @JoinColumn(name = "department_id"))
    @Comment("Danh sách đơn vị tổ chức")
    private Set<Department> organizers = new LinkedHashSet<>();

    /** Ngày tạo bản ghi. */
    @CreatedDate
    @Comment("Ngày tạo")
    private LocalDateTime createdAt;

    /** Ngày chỉnh sửa gần nhất. */
    @LastModifiedDate
    @Comment("Ngày chỉnh sửa")
    private LocalDateTime updatedAt;

    /** Người tạo (username/email). */
    @CreatedBy
    @Comment("Người tạo")
    private String createdBy;

    /** Người chỉnh sửa cuối cùng (username/email). */
    @LastModifiedBy
    @Comment("Người chỉnh sửa cuối cùng")
    private String lastModifiedBy;
}
```

### 2.2. ActivityOrganizer
Lưu danh sách sinh viên thuộc BTC của từng Activity.
- 1 Activity có nhiều Organizer
- 1 Student có thể làm Organizer cho nhiều Activity

Ràng buộc unique `(activity_id, student_id)`.

Mã nguồn `ActivityOrganizer.java`:

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_organizers", uniqueConstraints = @UniqueConstraint(columnNames = { "activity_id",
        "student_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ActivityOrganizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 2.3. PreparationTask (đổi tên từ “Task” để tránh trùng)
Nhiệm vụ chuẩn bị theo Activity.
- `activity_id` (many-to-one)
- `assignee_id` (Student, many-to-one)
- `status`: `PENDING | ACCEPTED | COMPLETED`

Mã nguồn `PreparationTask.java`:

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "preparation_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PreparationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private Student assignee;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreparationTaskStatus status = PreparationTaskStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 2.4. Budget
Ngân sách gắn với Activity (1-1, có thể null).
- `activity_id` (one-to-one, nullable)
- `totalAmount`

Mã nguồn `Budget.java`:

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", unique = true)
    private Activity activity;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 2.5. Expense
Khoản chi do BTC báo cáo, có ảnh minh chứng tùy chọn.
- `budget_id` (many-to-one)
- `reported_by_id` (Student)
- `evidenceUrl` (string, optional)
- Trạng thái duyệt: `approved` kiểu `Boolean`
  - `null`: WAITING_APPROVAL (chờ duyệt)
  - `true`: APPROVED
  - `false`: REJECTED

Chỉ khi APPROVED thì khoản chi mới được tính vào tổng đã chi.

Mã nguồn `Expense.java`:

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String evidenceUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private Student reportedBy;

    @Column(name = "is_approved")
    private Boolean approved;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 2.6. Enum trạng thái Task
Mã nguồn `PreparationTaskStatus.java`:

```java
package vn.campuslife.enumeration;

public enum PreparationTaskStatus {
    PENDING,
    ACCEPTED,
    COMPLETED
}
```

## 3. Phân quyền
Hệ thống đang dùng Role: `ADMIN`, `MANAGER`, `STUDENT`.
- Admin/Teacher trong mô tả được map sang `ADMIN/MANAGER` (backend hiện chưa có `TEACHER`).
- Organizer là một “quyền mềm” trên Role STUDENT, xác định bằng bảng `activity_organizers`.

## 4. API & nghiệp vụ chính
Các endpoint thuộc controller:
- [PreparationController.java](file:///d:/2025-2026%20HKI/TLCN/campuslife/src/main/java/vn/campuslife/controller/PreparationController.java)

### 4.1. Toggle Preparation
- `PUT /api/preparation/activities/{activityId}/toggle?enabled=true|false`
- Quyền: `ADMIN/MANAGER`
- Tác động: cập nhật `Activity.hasPreparation`

### 4.2. Dashboard Preparation
- `GET /api/preparation/activities/{activityId}/dashboard`
- Quyền: `ADMIN/MANAGER` hoặc Organizer của activity
- Nếu `hasPreparation=false` -> lỗi FeatureNotEnabled
- Trả về:
  - Danh sách `PreparationTask`
  - Budget:
    - Nếu chưa có Budget -> `financeMessage = "No Budget Assigned"`
    - Nếu có Budget -> trả `totalAmount`, `spentAmount`, `remainingAmount`
      - `spentAmount = SUM(expense.amount WHERE expense.approved = true)`
      - `remainingAmount = totalAmount - spentAmount`

DTO dùng trong module Preparation được nhúng đầy đủ ở mục 7.

### 4.3. Quản lý Organizer
- List:
  - `GET /api/preparation/activities/{activityId}/organizers`
  - Quyền: `ADMIN/MANAGER` hoặc Organizer
- Add:
  - `POST /api/preparation/activities/{activityId}/organizers/{studentId}`
  - Quyền: `ADMIN/MANAGER`
- Remove:
  - `DELETE /api/preparation/activities/{activityId}/organizers/{studentId}`
  - Quyền: `ADMIN/MANAGER`

### 4.4. Quản lý Task (PreparationTask)
- Assign task:
  - `POST /api/preparation/activities/{activityId}/tasks`
  - Quyền: `ADMIN/MANAGER`
  - Ràng buộc: `assigneeId` phải là Organizer của activity
- Assignee cập nhật trạng thái:
  - `PUT /api/preparation/tasks/{taskId}/status`
  - Quyền: chỉ assignee của task

### 4.5. Upload ảnh minh chứng (hóa đơn)
- `POST /api/preparation/activities/{activityId}/expenses/evidence`
- Quyền: Organizer
- Upload `multipart/form-data` file -> trả URL để FE lưu vào `Expense.evidenceUrl`

### 4.6. Báo cáo chi phí (Expense) và duyệt chi phí
- BTC báo cáo:
  - `POST /api/preparation/activities/{activityId}/expenses`
  - Quyền: Organizer
  - Tạo expense trạng thái `WAITING_APPROVAL` (`approved = null`)
  - Gửi notification cho `ADMIN/MANAGER` để vào dashboard duyệt
- Admin duyệt:
  - `PUT /api/preparation/expenses/{expenseId}/approval`
  - Quyền: `ADMIN/MANAGER`
  - Khi APPROVE:
    - Kiểm tra remaining tại thời điểm duyệt (tính theo approved=true)
    - Nếu đủ -> chuyển trạng thái APPROVED, notify người báo cáo
  - Khi REJECT:
    - Chuyển trạng thái REJECTED, notify người báo cáo

### 4.7. Xem danh sách chi phí theo trạng thái
- `GET /api/preparation/activities/{activityId}/expenses?status=ALL|PENDING|APPROVED|REJECTED`
- Quyền: `ADMIN/MANAGER` hoặc Organizer

## 5. Chuẩn response & lỗi
- Response wrapper: `Response{ status, message, body }`
  - [Response.java](file:///d:/2025-2026%20HKI/TLCN/campuslife/src/main/java/vn/campuslife/model/Response.java)
- Global handler chuẩn hóa lỗi:
  - [GlobalExceptionHandler.java](file:///d:/2025-2026%20HKI/TLCN/campuslife/src/main/java/vn/campuslife/exception/GlobalExceptionHandler.java)

## 6. Thông báo
- Khi BTC tạo Expense (WAITING_APPROVAL): notify bulk `ADMIN/MANAGER`.
- Khi Admin approve/reject: notify lại `reportedBy`.

## 7. DTO - mã nguồn đầy đủ

### 7.1. PreparationDashboardDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationDashboardDto {
    private Long activityId;
    private boolean hasPreparation;
    private List<PreparationTaskDto> tasks;
    private BudgetDto budget;
    private String financeMessage;
}
```

### 7.2. PreparationTaskDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparationTaskDto {
    private Long id;
    private Long activityId;
    private Long assigneeId;
    private String assigneeName;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private PreparationTaskStatus status;
}
```

### 7.3. BudgetDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {
    private Long id;
    private Long activityId;
    private BigDecimal totalAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private String description;
}
```

### 7.4. ExpenseDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {
    private Long id;
    private Long activityId;
    private Long budgetId;
    private BigDecimal amount;
    private String description;
    private String evidenceUrl;
    private Long reportedById;
    private String reportedByName;
    private Boolean approved;
    private LocalDateTime createdAt;
}
```

### 7.5. OrganizerDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerDto {
    private Long studentId;
    private String fullName;
}
```

### 7.6. CreatePreparationTaskRequest

```java
package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePreparationTaskRequest {
    private Long activityId;

    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private LocalDateTime deadline;
}
```

### 7.7. UpdatePreparationTaskStatusRequest

```java
package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.campuslife.enumeration.PreparationTaskStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreparationTaskStatusRequest {
    @NotNull(message = "Status is required")
    private PreparationTaskStatus status;
}
```

### 7.8. UpsertBudgetRequest

```java
package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertBudgetRequest {
    private Long activityId;

    @NotNull(message = "Total amount is required")
    @PositiveOrZero(message = "Total amount must be >= 0")
    private BigDecimal totalAmount;

    private String description;
}
```

### 7.9. CreateExpenseRequest

```java
package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {
    private Long activityId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be > 0")
    private BigDecimal amount;

    private String description;

    private String evidenceUrl;
}
```

### 7.10. UploadResultDto

```java
package vn.campuslife.model.preparation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResultDto {
    private String url;
}
```

### 7.11. ApproveExpenseRequest

```java
package vn.campuslife.model.preparation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveExpenseRequest {
    @NotNull(message = "Approved is required")
    private Boolean approved;
}
```
