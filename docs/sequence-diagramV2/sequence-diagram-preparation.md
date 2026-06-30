# Sequence Diagram — Preparation Module (Chuẩn bị sự kiện)

**Hệ thống:** CampusLife (Spring Boot + React)  
**Nhóm chức năng:** Preparation (Chuẩn bị sự kiện)  
**Các thành phần tham gia:**
- `U` — User (Admin/Manager/Staff/Organizer/Supervisor)
- `C` — Client (React Frontend)
- `CT` — Controller (Spring Boot REST Controller)
- `S` — Service (Business Logic Layer)
- `R` — Repository (Data Access Layer)
- `DB` — Database (SQL/NoSQL)
- `NS` — NotificationService / EmailService

---

## 1. Bật/tắt chế độ chuẩn bị (I.35)

`PUT /api/admin/preparation/activities/{id}/toggle`

```mermaid
sequenceDiagram
    autonumber
    actor U as Admin/Manager
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database
    participant NS as NotificationService

    Note over U, NS: Luồng bật/tắt chế độ chuẩn bị cho Activity

    U->>C: Click "Bật chuẩn bị" cho Activity
    C->>CT: PUT /api/admin/preparation/activities/{id}/toggle
    CT->>S: togglePreparationMode(activityId)

    S->>R: findActivityById(activityId)
    R->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>R:' "Activity"'
    R-->>S:' "Optional<Activity>"'

    alt Activity không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "Activity không tồn tại""'
    else Activity tồn tại
        S->>S: activity.setPreparationMode(!activity.isPreparationMode())
        S->>S: activity.setIsPreparationMode(true)

        S->>R: findPreparationContextByActivityId(activityId)
        R->>DB: SELECT * FROM preparation_context WHERE activity_id = ?
        DB-->>R:' "PreparationContext (nếu có)"'
        R-->>S:' "Optional<PreparationContext>"'

        alt PreparationContext chưa tồn tại
            S->>S: create new PreparationContext()
            S->>S: ctx.setActivityId(activityId)
            S->>S: ctx.setStatus(PreparationStatus.PREPARING)
            S->>S: ctx.setCreatedAt(now)
            S->>R: save(ctx)
            R->>DB: INSERT INTO preparation_context ...
            DB-->>R:' "PreparationContext"'
            R-->>S:' "PreparationContext"'
        else Đã tồn tại
            S->>S: ctx.setStatus(PreparationStatus.PREPARING)
            S->>R: save(ctx)
            R->>DB: UPDATE preparation_context SET ...
            DB-->>R:' "PreparationContext"'
            R-->>S:' "PreparationContext"'
        end

        S->>R: save(activity)
        R->>DB: UPDATE activity SET is_preparation_mode = true ...
        DB-->>R:' "Activity"'
        R-->>S:' "Activity"'

        S->>S: getAssignedStaff(activityId)
        S->>R: findOrganizersByActivityId(activityId)
        R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ?
        DB-->>R:' "List<ActivityOrganizer>"'
        R-->>S:' "List<ActivityOrganizer>"'

        loop Gửi notification cho từng Staff được assign
            S->>NS: sendNotification(userId, "Chế độ chuẩn bị đã bật", "Activity " + activityId)
            NS->>NS: Tạo notification record / gửi email
            NS-->>S: Notification sent
        end

        S-->>CT: PreparationToggleResultDTO
        CT-->>C:' "200 OK + DTO"'
        C-->>U:' "Hiển thị "Đã bật chế độ chuẩn bị" + danh sách staff nhận thông báo"'
    end
```

---

## 2. Quản lý ban tổ chức (I.36)

`POST /api/admin/preparation/activities/{id}/organizers`  
`DELETE /api/admin/preparation/activities/{id}/organizers/{userId}`  
`PUT /api/admin/preparation/activities/{id}/organizers/{userId}/promote`

```mermaid
sequenceDiagram
    autonumber
    actor U as Admin/Manager
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database
    participant NS as NotificationService

    Note over U, NS: === LUỒNG 2A: THÊM ORGANIZER ===

    U->>C: Click "Thêm Organizer", nhập userId + role
    C->>CT: POST /api/admin/preparation/activities/{id}/organizers {userId, role}
    CT->>S: addOrganizer(activityId, userId, role)

    S->>R: findUserById(userId)
    R->>DB: SELECT * FROM user WHERE id = ?
    DB-->>R:' "User"'
    R-->>S:' "Optional<User>"'

    alt User không tồn tại
        S-->>CT:' throw NotFoundException("User không tồn tại")'
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "User không tồn tại""'
    else User tồn tại
        S->>R: findOrganizerByActivityAndUser(activityId, userId)
        R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND user_id = ?
        DB-->>R:' "ActivityOrganizer (nếu có)"'
        R-->>S:' "Optional<ActivityOrganizer>"'

        alt User đã là organizer của activity này
            S-->>CT:' throw ConflictException("User đã là organizer")'
            CT-->>C:' "409 Conflict"'
            C-->>U:' "Hiển thị lỗi "User đã là organizer của sự kiện này""'
        else User chưa là organizer
            S->>S: create new ActivityOrganizer()
            S->>S: ao.setActivityId(activityId)
            S->>S: ao.setUserId(userId)
            S->>S: ao.setRole(role) // ORGANIZER hoặc SUPERVISOR
            S->>S: ao.setJoinedAt(now)
            S->>R: save(ao)
            R->>DB: INSERT INTO activity_organizer ...
            DB-->>R:' "ActivityOrganizer"'
            R-->>S:' "ActivityOrganizer"'

            S->>NS: sendNotification(userId, "Bạn được thêm vào ban tổ chức", activityId)
            NS-->>S: Notification sent

            S-->>CT: ActivityOrganizerDTO
            CT-->>C:' "201 Created + DTO"'
            C-->>U:' "Hiển thị "Thêm organizer thành công""'
        end
    end

    Note over U, NS: === LUỒNG 2B: XÓA ORGANIZER ===

    U->>C: Click "Xóa Organizer" cho userId
    C->>CT: DELETE /api/admin/preparation/activities/{id}/organizers/{userId}
    CT->>S: removeOrganizer(activityId, userId)

    S->>R: findOrganizerByActivityAndUser(activityId, userId)
    R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND user_id = ?
    DB-->>R:' "ActivityOrganizer"'
    R-->>S:' "Optional<ActivityOrganizer>"'

    alt Organizer không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "Organizer không tồn tại""'
    else Organizer tồn tại
        S->>R: delete(ao)
        R->>DB: DELETE FROM activity_organizer WHERE id = ?
        DB-->>R:' "void"'
        R-->>S:' "void"'

        S->>NS: sendNotification(userId, "Bạn đã bị xóa khỏi ban tổ chức", activityId)
        NS-->>S: Notification sent

        S-->>CT: void
        CT-->>C:' "204 No Content"'
        C-->>U:' "Hiển thị "Xóa organizer thành công""'
    end

    Note over U, NS: === LUỒNG 2C: CẤP PREP-SUPERVISOR ===

    U->>C: Click "Cấp quyền Supervisor" cho userId
    C->>CT: PUT /api/admin/preparation/activities/{id}/organizers/{userId}/promote
    CT->>S: promoteOrganizer(activityId, userId)

    S->>R: findOrganizerByActivityAndUser(activityId, userId)
    R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND user_id = ?
    DB-->>R:' "ActivityOrganizer"'
    R-->>S:' "Optional<ActivityOrganizer>"'

    alt Organizer không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "Organizer không tồn tại""'
    else Organizer tồn tại
        S->>S: ao.setRole(OrganizerRole.PREP_SUPERVISOR)
        S->>S: ao.setUpdatedAt(now)
        S->>R: save(ao)
        R->>DB: UPDATE activity_organizer SET role = 'PREP_SUPERVISOR' ...
        DB-->>R:' "ActivityOrganizer"'
        R-->>S:' "ActivityOrganizer"'

        S->>NS: sendNotification(userId, "Bạn được cấp quyền Supervisor chuẩn bị", activityId)
        NS-->>S: Notification sent

        S-->>CT: ActivityOrganizerDTO
        CT-->>C:' "200 OK + DTO"'
        C-->>U:' "Hiển thị "Cấp quyền Supervisor thành công""'
    end
```

---

## 3. Phân công & cập nhật tiến độ nhiệm vụ chuẩn bị (I.37)

`POST /api/admin/preparation/tasks/{taskId}/assign`  
`PUT /api/preparation/tasks/assignments/{id}/status`

```mermaid
sequenceDiagram
    autonumber
    actor U1 as Admin/Manager/Supervisor
    actor U2 as Organizer/Assignee
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database
    participant NS as NotificationService

    Note over U1, NS: === LUỒNG 3A: PHÂN CÔNG NHIỆM VỤ ===

    U1->>C: Click "Phân công", chọn task + assignee + deadline
    C->>CT: POST /api/admin/preparation/tasks/{taskId}/assign {assigneeId, deadline}
    CT->>S: assignTask(taskId, assigneeId, deadline)

    S->>R: findTaskById(taskId)
    R->>DB: SELECT * FROM preparation_task WHERE id = ?
    DB-->>R:' "PreparationTask"'
    R-->>S:' "Optional<PreparationTask>"'

    alt Task không tồn tại
        S-->>CT:' throw NotFoundException("Task không tồn tại")'
        CT-->>C:' "404 Not Found"'
        C-->>U1:' "Hiển thị lỗi "Task không tồn tại""'
    else Task tồn tại
        S->>R: findUserById(assigneeId)
        R->>DB: SELECT * FROM user WHERE id = ?
        DB-->>R:' "User"'
        R-->>S:' "Optional<User>"'

        alt Assignee không tồn tại
            S-->>CT:' throw NotFoundException("Assignee không tồn tại")'
            CT-->>C:' "404 Not Found"'
            C-->>U1:' "Hiển thị lỗi "Assignee không tồn tại""'
        else Assignee tồn tại
            S->>R: findOrganizerByActivityAndUser(task.activityId, assigneeId)
            R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND user_id = ?
            DB-->>R:' "ActivityOrganizer"'
            R-->>S:' "Optional<ActivityOrganizer>"'

            alt Assignee không phải organizer của activity
                S-->>CT:' throw ForbiddenException("Assignee không thuộc ban tổ chức")'
                CT-->>C:' "403 Forbidden"'
                C-->>U1:' "Hiển thị lỗi "Assignee không thuộc ban tổ chức""'
            else Assignee là organizer hợp lệ
                S->>S: create new PreparationTaskAssignment()
                S->>S: pta.setTaskId(taskId)
                S->>S: pta.setAssigneeId(assigneeId)
                S->>S: pta.setDeadline(deadline)
                S->>S: pta.setStatus(AssignmentStatus.ASSIGNED)
                S->>S: pta.setAssignedAt(now)
                S->>S: pta.setAssignedBy(currentUserId)
                S->>R: save(pta)
                R->>DB: INSERT INTO preparation_task_assignment ...
                DB-->>R:' "PreparationTaskAssignment"'
                R-->>S:' "PreparationTaskAssignment"'

                S->>R: save(task) // cập nhật task status nếu cần
                R->>DB: UPDATE preparation_task SET ...
                DB-->>R:' "PreparationTask"'
                R-->>S:' "PreparationTask"'

                S->>NS: sendNotification(assigneeId, "Bạn được phân công nhiệm vụ chuẩn bị", taskId)
                NS-->>S: Notification sent

                S-->>CT: PreparationTaskAssignmentDTO
                CT-->>C:' "201 Created + DTO"'
                C-->>U1:' "Hiển thị "Phân công thành công""'
            end
        end
    end

    Note over U1, NS: === LUỒNG 3B: CẬP NHẬT TIẾN ĐỘ ===

    U2->>C: Click "Cập nhật tiến độ", chọn status
    C->>CT: PUT /api/preparation/tasks/assignments/{id}/status {newStatus}
    CT->>S: updateAssignmentStatus(assignmentId, newStatus, userId)

    S->>R: findAssignmentById(assignmentId)
    R->>DB: SELECT * FROM preparation_task_assignment WHERE id = ?
    DB-->>R:' "PreparationTaskAssignment"'
    R-->>S:' "Optional<PreparationTaskAssignment>"'

    alt Assignment không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U2:' "Hiển thị lỗi "Assignment không tồn tại""'
    else Assignment tồn tại
        alt Assignee không phải người cập nhật
            S-->>CT:' throw ForbiddenException("Không có quyền cập nhật")'
            CT-->>C:' "403 Forbidden"'
            C-->>U2:' "Hiển thị lỗi "Không có quyền cập nhật""'
        else Quyền hợp lệ
            S->>S: validateStatusTransition(currentStatus, newStatus)

            alt Transition không hợp lệ (ví dụ: ASSIGNED → COMPLETED)
                S-->>CT:' throw BadRequestException("Transition không hợp lệ")'
                CT-->>C:' "400 Bad Request"'
                C-->>U2:' "Hiển thị lỗi "Không thể chuyển trạng thái này""'
            else Transition hợp lệ
                S->>S: pta.setStatus(newStatus) // IN_PROGRESS → COMPLETED
                S->>S: pta.setUpdatedAt(now)

                alt newStatus == COMPLETED
                    S->>S: pta.setCompletedAt(now)
                end

                S->>R: save(pta)
                R->>DB: UPDATE preparation_task_assignment SET status = ?, updated_at = ? ...
                DB-->>R:' "PreparationTaskAssignment"'
                R-->>S:' "PreparationTaskAssignment"'

                alt newStatus == COMPLETED
                    S->>R: findTaskById(pta.taskId)
                    R->>DB: SELECT * FROM preparation_task WHERE id = ?
                    DB-->>R:' "PreparationTask"'
                    R-->>S:' "PreparationTask"'

                    S->>R: findSupervisorsByActivityId(task.activityId)
                    R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND role = 'PREP_SUPERVISOR'
                    DB-->>R:' "List<ActivityOrganizer>"'
                    R-->>S:' "List<ActivityOrganizer>"'

                    loop Gửi yêu cầu review cho từng Supervisor
                        S->>NS: sendNotification(supervisorId, "Nhiệm vụ đã hoàn thành, cần review", assignmentId)
                        NS-->>S: Notification sent
                    end
                end

                S->>NS: sendNotificationToAssigner(pta.assignedBy, "Tiến độ nhiệm vụ được cập nhật", assignmentId)
                NS-->>S: Notification sent

                S-->>CT: PreparationTaskAssignmentDTO
                CT-->>C:' "200 OK + DTO"'
                C-->>U2:' "Hiển thị "Cập nhật tiến độ thành công""'
            end
        end
    end
```

---

## 4. Quản lý ngân sách hoạt động (I.38)

`POST/PUT /api/admin/preparation/activities/{id}/budget`  
`PUT /api/admin/preparation/tasks/{taskId}/budget`

```mermaid
sequenceDiagram
    autonumber
    actor U as Admin/Manager
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database

    Note over U, DB: === LUỒNG 4A: TẠO/CẬP NHẬT BUDGET ===

    U->>C: Nhập thông tin Budget (totalAmount, categories[])
    C->>CT: POST/PUT /api/admin/preparation/activities/{id}/budget BudgetRequestDTO
    CT->>S: upsertBudget(activityId, budgetRequest)

    S->>R: findActivityById(activityId)
    R->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>R:' "Activity"'
    R-->>S:' "Optional<Activity>"'

    alt Activity không tồn tại
        S-->>CT:' throw NotFoundException("Activity không tồn tại")'
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "Activity không tồn tại""'
    else Activity tồn tại
        S->>R: findBudgetByActivityId(activityId)
        R->>DB: SELECT * FROM preparation_budget WHERE activity_id = ?
        DB-->>R:' "PreparationBudget (nếu có)"'
        R-->>S:' "Optional<PreparationBudget>"'

        alt Budget chưa tồn tại (POST)
            S->>S: create new PreparationBudget()
            S->>S: pb.setActivityId(activityId)
            S->>S: pb.setTotalAmount(budgetRequest.totalAmount)
            S->>S: pb.setCategories(budgetRequest.categories)
            S->>S: pb.setUsedAmount(0)
            S->>S: pb.setRemainingAmount(budgetRequest.totalAmount)
            S->>S: pb.setCreatedAt(now)
            S->>R: save(pb)
            R->>DB: INSERT INTO preparation_budget ...
            DB-->>R:' "PreparationBudget"'
            R-->>S:' "PreparationBudget"'
        else Budget đã tồn tại (PUT)
            S->>S: pb.setTotalAmount(budgetRequest.totalAmount)
            S->>S: pb.setCategories(budgetRequest.categories)
            S->>S: pb.setRemainingAmount(pb.totalAmount - pb.usedAmount)
            S->>S: pb.setUpdatedAt(now)
            S->>R: save(pb)
            R->>DB: UPDATE preparation_budget SET ...
            DB-->>R:' "PreparationBudget"'
            R-->>S:' "PreparationBudget"'
        end

        S->>R: saveActivityBudgetReference(activityId, pb.id)
        R->>DB: UPDATE activity SET budget_id = ? WHERE id = ?
        DB-->>R:' "Activity"'
        R-->>S:' "Activity"'

        S-->>CT: PreparationBudgetDTO
        CT-->>C:' "200/201 OK + DTO"'
        C-->>U:' "Hiển thị "Lưu ngân sách thành công""'
    end

    Note over U, DB: === LUỒNG 4B: PHÂN BỔ NGÂN SÁCH CHO TASK ===

    U->>C: Nhập allocatedAmount cho Task
    C->>CT: PUT /api/admin/preparation/tasks/{taskId}/budget {allocatedAmount}
    CT->>S: allocateTaskBudget(taskId, allocatedAmount)

    S->>R: findTaskById(taskId)
    R->>DB: SELECT * FROM preparation_task WHERE id = ?
    DB-->>R:' "PreparationTask"'
    R-->>S:' "Optional<PreparationTask>"'

    alt Task không tồn tại
        S-->>CT:' throw NotFoundException("Task không tồn tại")'
        CT-->>C:' "404 Not Found"'
        C-->>U:' "Hiển thị lỗi "Task không tồn tại""'
    else Task tồn tại
        S->>R: findBudgetByActivityId(task.activityId)
        R->>DB: SELECT * FROM preparation_budget WHERE activity_id = ?
        DB-->>R:' "PreparationBudget"'
        R-->>S:' "Optional<PreparationBudget>"'

        alt Budget không tồn tại
            S-->>CT:' throw NotFoundException("Budget chưa được tạo")'
            CT-->>C:' "404 Not Found"'
            C-->>U:' "Hiển thị lỗi "Chưa có ngân sách cho sự kiện này""'
        else Budget tồn tại
            S->>S: calculateTotalAllocated(task.activityId)
            S->>R: sumAllocatedAmountByActivityId(task.activityId)
            R->>DB: SELECT SUM(allocated_amount) FROM preparation_task WHERE activity_id = ?
            DB-->>R:' "BigDecimal (totalAllocated)"'
            R-->>S:' "totalAllocated"'

            S->>S: newTotalAllocated = totalAllocated - task.currentAllocatedAmount + allocatedAmount
            S->>S: validate newTotalAllocated <= budget.totalAmount

            alt Tổng phân bổ vượt quá ngân sách
                S-->>CT:' throw BadRequestException("Vượt quá ngân sách")'
                CT-->>C:' "400 Bad Request"'
                C-->>U:' "Hiển thị lỗi "Tổng phân bổ vượt quá ngân sách cho phép""'
            else Hợp lệ
                S->>S: task.setAllocatedAmount(allocatedAmount)
                S->>S: task.setUpdatedAt(now)
                S->>R: save(task)
                R->>DB: UPDATE preparation_task SET allocated_amount = ? ...
                DB-->>R:' "PreparationTask"'
                R-->>S:' "PreparationTask"'

                S->>S: updateBudgetRemaining(budget)
                S->>S: budget.setRemainingAmount(budget.totalAmount - newTotalAllocated)
                S->>R: save(budget)
                R->>DB: UPDATE preparation_budget SET remaining_amount = ? ...
                DB-->>R:' "PreparationBudget"'
                R-->>S:' "PreparationBudget"'

                S-->>CT: PreparationTaskDTO
                CT-->>C:' "200 OK + DTO"'
                C-->>U:' "Hiển thị "Phân bổ ngân sách thành công""'
            end
        end
    end
```

---

## 5. Tạm ứng & phê duyệt kinh phí (I.39)

`POST /api/preparation/fund-advances`  
`PUT /api/admin/preparation/fund-advances/{id}/approve`

```mermaid
sequenceDiagram
    autonumber
    actor U1 as Organizer
    actor U2 as Admin/Manager
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database
    participant NS as NotificationService

    Note over U1, NS: === LUỒNG 5A: TẠO YÊU CẦU TẠM ỨNG ===

    U1->>C: Nhập thông tin tạm ứng (amount, reason, evidence)
    C->>CT: POST /api/preparation/fund-advances FundAdvanceRequestDTO
    CT->>S: createFundAdvanceRequest(requestDTO, organizerId)

    S->>R: findActivityById(requestDTO.activityId)
    R->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>R:' "Activity"'
    R-->>S:' "Optional<Activity>"'

    alt Activity không tồn tại hoặc không ở chế độ chuẩn bị
        S-->>CT: throw NotFoundException / BadRequestException
        CT-->>C:' "404 / 400 Error"'
        C-->>U1:' "Hiển thị lỗi"'
    else Activity hợp lệ
        S->>R: findBudgetByActivityId(requestDTO.activityId)
        R->>DB: SELECT * FROM preparation_budget WHERE activity_id = ?
        DB-->>R:' "PreparationBudget"'
        R-->>S:' "Optional<PreparationBudget>"'

        alt Budget không tồn tại
            S-->>CT:' throw NotFoundException("Chưa có ngân sách")'
            CT-->>C:' "404 Not Found"'
            C-->>U1:' "Hiển thị lỗi "Chưa có ngân sách""'
        else Budget tồn tại
            alt requestAmount > budget.remainingAmount
                S-->>CT:' throw BadRequestException("Yêu cầu vượt quá ngân sách còn lại")'
                CT-->>C:' "400 Bad Request"'
                C-->>U1:' "Hiển thị lỗi "Số tiền tạm ứng vượt quá ngân sách còn lại""'
            else Hợp lệ
                S->>S: create new FundAdvanceRequest()
                S->>S: far.setActivityId(requestDTO.activityId)
                S->>S: far.setRequesterId(organizerId)
                S->>S: far.setAmount(requestDTO.amount)
                S->>S: far.setReason(requestDTO.reason)
                S->>S: far.setEvidenceUrls(requestDTO.evidence)
                S->>S: far.setStatus(FundAdvanceStatus.PENDING)
                S->>S: far.setRequestedAt(now)
                S->>R: save(far)
                R->>DB: INSERT INTO fund_advance_request ...
                DB-->>R:' "FundAdvanceRequest"'
                R-->>S:' "FundAdvanceRequest"'

                S->>R: findAdminsAndManagers()
                R->>DB: SELECT * FROM user WHERE role IN ('ADMIN', 'MANAGER')
                DB-->>R:' "List<User>"'
                R-->>S:' "List<User>"'

                loop Gửi notification cho từng Admin/Manager
                    S->>NS: sendNotification(adminId, "Yêu cầu tạm ứng mới cần phê duyệt", far.id)
                    NS-->>S: Notification sent
                end

                S-->>CT: FundAdvanceRequestDTO
                CT-->>C:' "201 Created + DTO"'
                C-->>U1:' "Hiển thị "Gửi yêu cầu tạm ứng thành công""'
            end
        end
    end

    Note over U1, NS: === LUỒNG 5B: PHÊ DUYỆT TẠM ỨNG ===

    U2->>C: Xem danh sách yêu cầu tạm ứng, chọn Approve/Reject
    C->>CT: PUT /api/admin/preparation/fund-advances/{id}/approve {decision: APPROVED/REJECTED, note}
    CT->>S: approveFundAdvance(fundAdvanceId, decision, note, approverId)

    S->>R: findFundAdvanceById(fundAdvanceId)
    R->>DB: SELECT * FROM fund_advance_request WHERE id = ?
    DB-->>R:' "FundAdvanceRequest"'
    R-->>S:' "Optional<FundAdvanceRequest>"'

    alt Request không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U2:' "Hiển thị lỗi "Yêu cầu không tồn tại""'
    else Request tồn tại
        alt Request status != PENDING
            S-->>CT:' throw BadRequestException("Yêu cầu đã được xử lý")'
            CT-->>C:' "400 Bad Request"'
            C-->>U2:' "Hiển thị lỗi "Yêu cầu đã được xử lý trước đó""'
        else Request đang PENDING
            S->>S: far.setStatus(decision) // APPROVED hoặc REJECTED
            S->>S: far.setApproverId(approverId)
            S->>S: far.setApprovalNote(note)
            S->>S: far.setApprovedAt(now)
            S->>R: save(far)
            R->>DB: UPDATE fund_advance_request SET status = ?, approver_id = ?, ...
            DB-->>R:' "FundAdvanceRequest"'
            R-->>S:' "FundAdvanceRequest"'

            alt decision == APPROVED
                S->>R: findBudgetByActivityId(far.activityId)
                R->>DB: SELECT * FROM preparation_budget WHERE activity_id = ?
                DB-->>R:' "PreparationBudget"'
                R-->>S:' "PreparationBudget"'

                S->>S: budget.setUsedAmount(budget.usedAmount + far.amount)
                S->>S: budget.setRemainingAmount(budget.totalAmount - budget.usedAmount)
                S->>S: budget.setUpdatedAt(now)
                S->>R: save(budget)
                R->>DB: UPDATE preparation_budget SET used_amount = ?, remaining_amount = ? ...
                DB-->>R:' "PreparationBudget"'
                R-->>S:' "PreparationBudget"'

                S->>S: createFundAdvanceLedger(far) // Tạo bản ghi tạm ứng đã duyệt
                S->>R: save(ledger)
                R->>DB: INSERT INTO fund_advance_ledger ...
                DB-->>R:' "FundAdvanceLedger"'
                R-->>S:' "FundAdvanceLedger"'
            end

            S->>NS: sendNotification(far.requesterId, "Yêu cầu tạm ứng " + decision, far.id)
            NS-->>S: Notification sent

            S-->>CT: FundAdvanceRequestDTO
            CT-->>C:' "200 OK + DTO"'
            C-->>U2:' "Hiển thị "Phê duyệt thành công" / "Từ chối thành công""'
        end
    end
```

---

## 6. Chi tiêu & báo cáo tài chính (I.40)

`POST /api/preparation/expenses`  
`PUT /api/admin/preparation/expenses/{id}/approve`  
`GET /api/admin/preparation/activities/{id}/financial-report`

```mermaid
sequenceDiagram
    autonumber
    actor U1 as Organizer
    actor U2 as Supervisor/Admin
    actor U3 as Admin/Manager
    participant C as Client
    participant CT as PreparationController
    participant S as PreparationService
    participant R as PreparationRepository
    participant DB as Database
    participant NS as NotificationService

    Note over U1, NS: === LUỒNG 6A: TẠO CHI TIÊU ===

    U1->>C: Nhập thông tin chi tiêu (description, amount, receiptImage, fundAdvanceId)
    C->>CT: POST /api/preparation/expenses ExpenseRequestDTO
    CT->>S: createExpense(requestDTO, organizerId)

    S->>R: findActivityById(requestDTO.activityId)
    R->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>R:' "Activity"'
    R-->>S:' "Optional<Activity>"'

    alt Activity không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U1:' "Hiển thị lỗi"'
    else Activity tồn tại
        alt requestDTO.fundAdvanceId != null
            S->>R: findFundAdvanceById(requestDTO.fundAdvanceId)
            R->>DB: SELECT * FROM fund_advance_request WHERE id = ?
            DB-->>R:' "FundAdvanceRequest"'
            R-->>S:' "Optional<FundAdvanceRequest>"'

            alt FundAdvance không tồn tại hoặc không APPROVED
                S-->>CT: throw NotFoundException / BadRequestException
                CT-->>C:' "404 / 400 Error"'
                C-->>U1:' "Hiển thị lỗi "Tạm ứng không hợp lệ""'
            else FundAdvance hợp lệ
                S->>S: continue
            end
        end

        S->>S: create new Expense()
        S->>S: exp.setActivityId(requestDTO.activityId)
        S->>S: exp.setReporterId(organizerId)
        S->>S: exp.setDescription(requestDTO.description)
        S->>S: exp.setAmount(requestDTO.amount)
        S->>S: exp.setReceiptImageUrl(requestDTO.receiptImage)
        S->>S: exp.setFundAdvanceId(requestDTO.fundAdvanceId)
        S->>S: exp.setStatus(ExpenseStatus.PENDING)
        S->>S: exp.setCreatedAt(now)
        S->>R: save(exp)
        R->>DB: INSERT INTO expense ...
        DB-->>R:' "Expense"'
        R-->>S:' "Expense"'

        S->>R: findSupervisorsByActivityId(requestDTO.activityId)
        R->>DB: SELECT * FROM activity_organizer WHERE activity_id = ? AND role = 'PREP_SUPERVISOR'
        DB-->>R:' "List<ActivityOrganizer>"'
        R-->>S:' "List<ActivityOrganizer>"'

        loop Gửi cho Supervisor review
            S->>NS: sendNotification(supervisorId, "Chi tiêu mới cần review", exp.id)
            NS-->>S: Notification sent
        end

        S-->>CT: ExpenseDTO
        CT-->>C:' "201 Created + DTO"'
        C-->>U1:' "Hiển thị "Gửi chi tiêu thành công, chờ phê duyệt""'
    end

    Note over U1, NS: === LUỒNG 6B: PHÊ DUYỆT CHI TIÊU ===

    U2->>C: Xem chi tiêu cần review, chọn Approve/Reject
    C->>CT: PUT /api/admin/preparation/expenses/{id}/approve {decision, note}
    CT->>S: approveExpense(expenseId, decision, note, approverId)

    S->>R: findExpenseById(expenseId)
    R->>DB: SELECT * FROM expense WHERE id = ?
    DB-->>R:' "Expense"'
    R-->>S:' "Optional<Expense>"'

    alt Expense không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U2:' "Hiển thị lỗi"'
    else Expense tồn tại
        alt Expense status != PENDING
            S-->>CT:' throw BadRequestException("Chi tiêu đã được xử lý")'
            CT-->>C:' "400 Bad Request"'
            C-->>U2:' "Hiển thị lỗi "Chi tiêu đã được xử lý trước đó""'
        else Expense đang PENDING
            S->>S: exp.setStatus(decision) // APPROVED / REJECTED
            S->>S: exp.setApproverId(approverId)
            S->>S: exp.setApprovalNote(note)
            S->>S: exp.setApprovedAt(now)
            S->>R: save(exp)
            R->>DB: UPDATE expense SET status = ?, approver_id = ?, ...
            DB-->>R:' "Expense"'
            R-->>S:' "Expense"'

            alt decision == APPROVED và exp.fundAdvanceId != null
                S->>R: findFundAdvanceById(exp.fundAdvanceId)
                R->>DB: SELECT * FROM fund_advance_request WHERE id = ?
                DB-->>R:' "FundAdvanceRequest"'
                R-->>S:' "FundAdvanceRequest"'

                S->>R: findFundAdvanceLedgerByRequestId(exp.fundAdvanceId)
                R->>DB: SELECT * FROM fund_advance_ledger WHERE request_id = ?
                DB-->>R:' "FundAdvanceLedger"'
                R-->>S:' "FundAdvanceLedger"'

                S->>S: ledger.setRemainingAmount(ledger.remainingAmount - exp.amount)
                S->>S: ledger.setUsedAmount(ledger.usedAmount + exp.amount)
                S->>S: ledger.setUpdatedAt(now)
                S->>R: save(ledger)
                R->>DB: UPDATE fund_advance_ledger SET remaining_amount = ?, used_amount = ? ...
                DB-->>R:' "FundAdvanceLedger"'
                R-->>S:' "FundAdvanceLedger"'
            end

            S->>NS: sendNotification(exp.reporterId, "Chi tiêu " + decision, exp.id)
            NS-->>S: Notification sent

            S-->>CT: ExpenseDTO
            CT-->>C:' "200 OK + DTO"'
            C-->>U2:' "Hiển thị "Phê duyệt chi tiêu thành công""'
        end
    end

    Note over U1, NS: === LUỒNG 6C: BÁO CÁO TÀI CHÍNH ===

    U3->>C: Click "Xem báo cáo tài chính"
    C->>CT: GET /api/admin/preparation/activities/{id}/financial-report
    CT->>S: getFinancialReport(activityId)

    S->>R: findActivityById(activityId)
    R->>DB: SELECT * FROM activity WHERE id = ?
    DB-->>R:' "Activity"'
    R-->>S:' "Optional<Activity>"'

    alt Activity không tồn tại
        S-->>CT: throw NotFoundException
        CT-->>C:' "404 Not Found"'
        C-->>U3:' "Hiển thị lỗi"'
    else Activity tồn tại
        S->>R: findBudgetByActivityId(activityId)
        R->>DB: SELECT * FROM preparation_budget WHERE activity_id = ?
        DB-->>R:' "PreparationBudget"'
        R-->>S:' "Optional<PreparationBudget>"'

        S->>R: findAllFundAdvancesByActivityId(activityId)
        R->>DB: SELECT * FROM fund_advance_request WHERE activity_id = ?
        DB-->>R:' "List<FundAdvanceRequest>"'
        R-->>S:' "List<FundAdvanceRequest>"'

        S->>R: findAllExpensesByActivityId(activityId)
        R->>DB: SELECT * FROM expense WHERE activity_id = ?
        DB-->>R:' "List<Expense>"'
        R-->>S:' "List<Expense>"'

        S->>S: calculateSummary()
        S->>S: totalBudget = budget.totalAmount
        S->>S: totalAllocated = budget.totalAmount - budget.remainingAmount (hoặc từ tasks)
        S->>S: totalAdvancesApproved = sum(far.amount WHERE status == APPROVED)
        S->>S: totalAdvancesPending = sum(far.amount WHERE status == PENDING)
        S->>S: totalExpensesApproved = sum(exp.amount WHERE status == APPROVED)
        S->>S: totalExpensesPending = sum(exp.amount WHERE status == PENDING)
        S->>S: remainingBudget = budget.remainingAmount
        S->>S: fundAdvanceRemaining = sum(ledger.remainingAmount)

        S->>S: create FinancialReportDTO()
        S->>S: report.setActivityId(activityId)
        S->>S: report.setBudget(budget)
        S->>S: report.setFundAdvances(listAdvances)
        S->>S: report.setExpenses(listExpenses)
        S->>S: report.setSummary(summary)
        S->>S: report.setGeneratedAt(now)

        S-->>CT: FinancialReportDTO
        CT-->>C:' "200 OK + DTO"'
        C-->>U3:' "Hiển thị báo cáo tài chính tổng hợp (Budget, Advances, Expenses, Remaining)"'
    end
```

---

## Tóm tắt thành phần và chức năng

| STT | Thành phần | Vai trò | Chức năng chính trong Preparation Module |
|-----|-----------|---------|------------------------------------------|
| 1 | **Client (React)** | Frontend | Hiển thị UI, gửi request, nhận response, thông báo kết quả cho User |
| 2 | **PreparationController** | REST Controller | Tiếp nhận HTTP request, validate input, gọi Service, trả về ResponseEntity |
| 3 | **PreparationService** | Business Logic | Xử lý nghiệp vụ: validate, tính toán, orchestrate Repository và NotificationService |
| 4 | **PreparationRepository** | Data Access | Thao tác CRUD với các entity: Activity, PreparationContext, ActivityOrganizer, PreparationTask, PreparationTaskAssignment, PreparationBudget, FundAdvanceRequest, Expense, FundAdvanceLedger |
| 5 | **Database** | Persistence | Lưu trữ dữ liệu: PostgreSQL/MySQL/NoSQL — bảng liên quan đến preparation |
| 6 | **NotificationService** | Cross-cutting | Gửi notification/email cho User khi có sự kiện: phân công, phê duyệt, cập nhật trạng thái |

### Các luồng nghiệp vụ tóm tắt

| STT | Luồng | API Endpoint | Actor | Kết quả chính |
|-----|-------|-------------|-------|--------------|
| 1 | Bật/tắt chế độ chuẩn bị | `PUT /api/admin/preparation/activities/{id}/toggle` | Admin/Manager | Tạo/cập nhật PreparationContext, gửi notification cho staff |
| 2 | Quản lý ban tổ chức | `POST/DELETE/PUT /api/admin/preparation/activities/{id}/organizers/...` | Admin/Manager | CRUD ActivityOrganizer, gửi notification |
| 3 | Phân công & cập nhật tiến độ | `POST /api/admin/preparation/tasks/{taskId}/assign`<br>`PUT /api/preparation/tasks/assignments/{id}/status` | Admin/Manager<br>Organizer | Tạo assignment, cập nhật status, gửi notification khi hoàn thành |
| 4 | Quản lý ngân sách | `POST/PUT /api/admin/preparation/activities/{id}/budget`<br>`PUT /api/admin/preparation/tasks/{taskId}/budget` | Admin/Manager | CRUD PreparationBudget, phân bổ amount cho task |
| 5 | Tạm ứng & phê duyệt | `POST /api/preparation/fund-advances`<br>`PUT /api/admin/preparation/fund-advances/{id}/approve` | Organizer<br>Admin/Manager | Tạo request, phê duyệt, cập nhật budget.usedAmount |
| 6 | Chi tiêu & báo cáo | `POST /api/preparation/expenses`<br>`PUT /api/admin/preparation/expenses/{id}/approve`<br>`GET /api/admin/preparation/activities/{id}/financial-report` | Organizer<br>Supervisor/Admin<br>Admin/Manager | Tạo expense, phê duyệt, tổng hợp financial report |

### Các ràng buộc nghiệp vụ (Business Rules)

1. **Chỉ Admin/Manager** mới có quyền bật/tắt chế độ chuẩn bị, quản lý organizer, phê duyệt tạm ứng/chi tiêu, xem báo cáo tài chính.
2. **Organizer** chỉ có thể cập nhật tiến độ nhiệm vụ được phân công cho mình.
3. **Supervisor** có thể review nhiệm vụ hoàn thành và phê duyệt chi tiêu.
4. **Phân bổ ngân sách** không được vượt quá `totalAmount` của budget.
5. **Tạm ứng** không được vượt quá `remainingAmount` của budget.
6. **Chi tiêu** chỉ có thể liên kết với `FundAdvance` đã được `APPROVED`.
7. **Status transition** của assignment phải hợp lệ (ASSIGNED → IN_PROGRESS → COMPLETED).
8. **Tất cả thao tác quan trọng** đều gửi notification cho người liên quan.
