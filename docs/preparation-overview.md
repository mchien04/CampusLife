# Báo cáo tổng quan - Module Chuẩn bị Sự kiện (Preparation) - v2

## 1. Mục tiêu
- Bật/tắt chế độ chuẩn bị cho từng Activity (`hasPreparation`).
- Quản lý danh sách sinh viên thuộc BTC (Organizer) theo Activity.
- Quản lý Task chuẩn bị theo Activity (Leader/Member, trạng thái tiến độ).
- Quản lý tài chính v2: ActivityBudget theo hạng mục + duyệt chi phí 2 cấp + tạm ứng + audit log.

## 2. Entity tham gia (v2)

### 2.1. Activity (trích phần liên quan)
```java
@Column(nullable = false)
private boolean hasPreparation = false;
```

### 2.2. ActivityOrganizer (BTC theo Activity)
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
@Table(name = "activity_organizers", uniqueConstraints = @UniqueConstraint(columnNames = { "activity_id", "student_id" }))
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreparationTaskMemberRole role = PreparationTaskMemberRole.MEMBER;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 2.3. ActivityBudget + BudgetCategory (ngân sách theo hạng mục)
```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
@EqualsAndHashCode(exclude = { "activity", "categories" })
@ToString(exclude = { "activity", "categories" })
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

```java
package vn.campuslife.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_categories", uniqueConstraints = @UniqueConstraint(columnNames = { "activity_budget_id", "name" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "activityBudget" })
@ToString(exclude = { "activityBudget" })
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

### 2.4. PreparationTask (Task) + Member
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

### 2.5. FundAdvance (tạm ứng)
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

### 2.6. Expense (chi phí) + AuditLog
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

## 3. Phân quyền (tóm tắt)
- ADMIN/MANAGER: tạo budget, allocate task, tạo fund advance, duyệt cấp cuối (PENDING_ADMIN → APPROVED/REJECTED)
- LEADER (PreparationTaskMember.role=LEADER): duyệt cấp 1 (PENDING_LEADER → PENDING_ADMIN/REJECTED), quản lý member trong task
- MEMBER (thuộc task): tạo expense, upload evidence

## 4. Luồng nghiệp vụ tài chính (v2)
- Allocate task:
  - Đặt “trần chi” cho task (kế hoạch phân bổ), không phải tiền đã phát.
- FundAdvance:
  - Là tiền tạm ứng thực tế theo (task, student), dùng để trừ dần khi APPROVED cấp cuối.
- Khi APPROVED cấp cuối (trong 1 transaction):
  - Kiểm tra không vượt: task.allocatedAmount, category.remaining, fundAdvance.remaining tổng
  - Trừ FIFO fund advances của người tạo expense theo task
  - Cộng BudgetCategory.usedAmount
  - Ghi AuditLog
  - Gửi Notification (expense chờ duyệt / ngân sách sắp cạn)

## 5. API chính (v2)
- Dashboard:
  - `GET /api/preparation/activities/{activityId}/dashboard`
- Budget theo hạng mục:
  - `PUT /api/preparation/activities/{activityId}/budget`
- Allocate cho task:
  - `PUT /api/preparation/tasks/{taskId}/allocation`
- Tạm ứng:
  - `POST /api/preparation/tasks/{taskId}/fund-advances`
- Member/Expense:
  - `POST /api/preparation/tasks/{taskId}/members/{studentId}`
  - `POST /api/preparation/tasks/{taskId}/expenses/evidence`
  - `POST /api/preparation/tasks/{taskId}/expenses`
  - `PUT /api/preparation/expenses/{expenseId}/leader-decision`
  - `PUT /api/preparation/expenses/{expenseId}/admin-decision`
- Report:
  - `GET /api/preparation/activities/{activityId}/financial-report`

## 6. FE TypeScript
Tài liệu FE v2 (types + flow + ví dụ gọi API):
- `docs/preparation-fe-guide.md`
