# Comprehensive Backend Audit & Implementation Plan

> **Scope:** 8 core areas of the CampusLife Spring Boot 3.5.5 backend  
> **Baseline:** Current HEAD  
> **Methodology:** Three-perspective code audit (simplicity, performance, risk-reduction) synthesized into unified findings and plan

---

## PHASE 1 - AUDIT FINDINGS

---

### 1. Score History

**Current Implementation:**
- `ScoreServiceImpl.java` (lines 438-592) -- `getScoreHistory()` method
- `ScoreHistoryViewResponse`, `ScoreHistoryDetailResponse` DTOs
- Data source: `score_entries` table (ledger pattern) + `activity_participations` (secondary source)
- Endpoint: `GET /api/scores/history/student/{studentId}?semesterId=&scoreType=&page=&size=`

**Business Rules:**
- Score history is stored in a unified `score_entries` ledger with `score_type` column for filtering
- Running totals (`oldScore`/`newScore`) computed via in-memory accumulation loop (line 477-484)
- Response combines two data sources: ScoreEntry records AND ActivityParticipation records
- Pagination requested via `page`/`size` params but implemented as **in-memory slicing** (line 516-519)
- Students can only view their own history; Admins can view all

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **In-memory pagination**: Loads ALL score entries, reverses, then slices -- O(n) memory | HIGH | `ScoreServiceImpl.java` | 470-519 |
| **N+1 queries in loop**: `seriesRepository.findById()` and `progressRepository.findById()` called per entry | HIGH | `ScoreServiceImpl.java` | 496, 502 |
| **Dual source of truth**: ScoreEntry ledger + ActivityParticipation can drift | MEDIUM | `ScoreServiceImpl.java` | 524-565 |
| **`changedByFullName` always null**: Hardcoded to null (line 510) | LOW | `ScoreServiceImpl.java` | 510 |
| **`sourceType` enum mismatch**: DB stores `ScoreEntrySourceType` enum but DTO labels differ ("ACTIVITY", "MINIGAME") | LOW | `ScoreServiceImpl.java` | 548-552 |

**Risks of Modification:**
- Changing pagination to DB-level requires maintaining running-total calculation logic (oldScore/newScore depend on ordered iteration)
- Removing ActivityParticipation as secondary source may break frontend expectations

---

### 2. Registration Logic (`isImportant`, `mandatoryForFacultyStudents`)

**Current Implementation:**
- `ActivityServiceImpl.java` (lines 600-710) -- `autoRegisterStudents()` private method
- Entity: `Activity.java` (fields `isImportant`, `mandatoryForFacultyStudents`)
- Called during activity create/update when `isDraft = false`

**Business Rules:**
- `isImportant = true`: Auto-registers ALL active (non-deleted) students with `APPROVED` status
- `mandatoryForFacultyStudents = true`: Auto-registers students from organizing departments only
- Both flags can be true simultaneously (union with deduplication via `.distinct()`)
- Auto-registered students cannot cancel (status is APPROVED, immutable)
- Draft activities skip auto-registration entirely
- Each registration gets a unique ticketCode and initial `REGISTERED` participation

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **`findAll()` loads entire student table** into memory then filters with stream | HIGH | `ActivityServiceImpl.java` | 620-622 |
| **N+1 existence checks**: `existsByActivityIdAndStudentId()` called per student in stream | HIGH | `ActivityServiceImpl.java` | 645-646 |
| **N+1 `existsByTicketCode()`**: Ticket uniqueness checked per registration in loop | MEDIUM | `ActivityServiceImpl.java` | 663 |
| **No null-guard on organizers**: `activity.getOrganizers().isEmpty()` could NPE | LOW | `ActivityServiceImpl.java` | 630 |
| **No deregistration on flag toggle-off**: Changing `isImportant` from true->false doesn't unregister | LOW | `ActivityServiceImpl.java` | N/A |
| **Synchronous notification loop**: FCM sent per student sequentially (lines 709+) | MEDIUM | `ActivityServiceImpl.java` | 709-710 |

**Risks of Modification:**
- Batch optimizations must preserve duplicate-prevention semantics
- Auto-registration is coupled to the activity create/update transaction -- errors could roll back entire operation

---

### 3. Event Task Assignment

**Current Implementation:**
- `ActivityTaskServiceImpl.java` (lines 192-233) -- `assignTask()` method
- `ActivityTaskController.java` (`@RequestMapping("/api/tasks")`) -- endpoint `POST /api/tasks/assign`
- DTO: `TaskAssignmentRequest` (contains `taskId` + `studentIds`; NO `activityId` in request or URL)

**Business Rules:**
- Tasks assigned to specific students or all registered students
- Duplicate prevention via `existsByTaskIdAndStudentId()` (line 211)
- Status set to PENDING on assignment
- Quartz reminders created per assignment (line 223)

**CRITICAL BUG FOUND:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **No validation that assigned students belong to the task's owning activity** -- tasks can be assigned to any student regardless of registration | **CRITICAL** | `ActivityTaskServiceImpl.java` | 204-207 |
| **No task-activity ownership check**: `findByIdAndActivityIsDeletedFalse(taskId)` only checks task exists and parent activity is not deleted; does NOT verify which activity the task belongs to | **CRITICAL** | `ActivityTaskServiceImpl.java` | 195-196 |
| **Inconsistent validation across paths**: `assignTaskToRegisteredStudents()` (line 86, takes `activityId` + `taskId` as params) validates task-activity ownership; `assignTask()` does not | MEDIUM | `ActivityTaskServiceImpl.java` | 192 vs controller line 104-109 |

**Evidence:** Line 195-196 uses `findByIdAndActivityIsDeletedFalse(request.getTaskId())` which only validates the task exists and its parent activity is not deleted. The `ActivityTask` entity has a `getActivity()` relation, but `assignTask()` never reads `task.getActivity()` to verify student registrations. Note: Other endpoints like `POST /api/tasks/auto-assign/{activityId}` and `POST /api/tasks/assign-to-registered/{activityId}` DO take `activityId` as a path variable, but the main `POST /api/tasks/assign` does not.

**Risks of Modification:**
- Adding validation may reject previously-accepted requests if existing data has cross-activity assignments
- No audit trail exists for assignments, making cleanup of existing bad data difficult

---

### 4. Score Rule Preset vs Custom

**Current Implementation:**
- `ScorePresetServiceImpl.java` (lines 55-519) -- preset definitions and rule generation
- Enum: `ActivityPresetCode` (EVENT_BASIC, EVENT_WITH_SUBMISSION, ENTERPRISE_SEMINAR_BASIC, ENTERPRISE_SEMINAR_WITH_BONUS, MINIGAME_PASS_ONLY, CUSTOM)
- Flow: `previewActivityPreset()` -> `applyActivityPreset()` -> rules stored as `ActivityScoreRule` entities

**Business Rules:**
- Presets are **read-only templates** that generate `ActivityScoreRuleRequest` lists
- `CUSTOM` preset bypasses generation entirely -- user provides rules directly
- `ActivityPresetConfig` allows runtime overrides (participationPoints, noShowPenaltyEnabled, etc.)
- Preset-generated rules are stored in DB identically to manually-created rules (no differentiation)
- On update with preset, old rules are NOT automatically replaced

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **No immutability flag**: Preset-generated rules indistinguishable from manual rules post-creation | MEDIUM | `ScorePresetServiceImpl.java` | 134 |
| **No conflict detection**: User can select preset AND provide custom `scoreRules` simultaneously | MEDIUM | `ScorePresetServiceImpl.java` | 118-135 |
| **Update doesn't replace old rules**: Changing preset on update appends rather than replaces | MEDIUM | `ScorePresetServiceImpl.java` | 157-160 |
| **No validation on custom rules**: Invalid combinations (e.g., positive `failPoints` with PENALTY_POINTS) not rejected | LOW | N/A | N/A |

**Risks of Modification:**
- Making rules immutable breaks any existing workflow where admins adjust preset rules post-creation
- Replacing rules on update could delete manually-added bonus rules

---

### 5. Seminar Session Scoring Rules

**Current Implementation:**
- `ScorePresetServiceImpl.java` (lines 58-67, 311-330) -- `ENTERPRISE_SEMINAR_BASIC` preset
- Calculation: `COUNT_COMPLETION` with `BigDecimal.ONE` default points
- Score type: `CHUYEN_DE` (enterprise/specialized knowledge)
- No-show penalty: OFF by default for seminars

**Business Rules:**
- Each seminar session completion awards exactly 1 point of type `CHUYEN_DE`
- The "1 point per session" is an implicit default, NOT a hard constraint
- No validation prevents modifying the rule to award 2+ points after creation
- `ENTERPRISE_SEMINAR_WITH_BONUS` adds a second rule with bonus score type

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **No enforcement of "exactly 1 point"**: Rules can be modified post-creation to any value | LOW | `ScorePresetServiceImpl.java` | 315 |
| **Hardcoded Vietnamese in preset notes**: "Tich luy buoi chuyen de doanh nghiep" | LOW | `ScorePresetServiceImpl.java` | 61 |
| **No validation that seminar activities use the correct preset** | LOW | N/A | N/A |

**Risks of Modification:**
- Adding hard validation could reject legitimate custom seminar configurations
- Minimal risk as this is mostly a preset-level concern

> **Note:** This is a business-rule clarification, not a bug. A backend-only log warning is invisible to FE/admins. If enforcement is desired, it should either (a) return warnings in the API response via a `ValidationResult` wrapper, or (b) be documented as a preset UI constraint in FE. Defer until FE preset UI is finalized.

---

### 6. Statistics Module

**Current Implementation:**
- `StatisticsController.java` -- 6 endpoints (`/dashboard`, `/activities`, `/students`, `/scores`, `/series`, `/minigames`)
- `StatisticsServiceImpl.java` (616 lines) -- aggregation logic
- Response DTOs: `DashboardOverviewResponse`, `ActivityStatisticsResponse`, `StudentStatisticsResponse`, `ScoreStatisticsResponse`

**Business Rules:**
- Dashboard provides summary counts, monthly trends, top 5 activities/students
- Role-based: Students see personal stats; Admins see all
- Statistics computed on-the-fly per request (no caching)

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **No caching**: All aggregate queries execute on every request | MEDIUM* | `StatisticsServiceImpl.java` | All |
| **Unbounded GROUP BY queries**: No date/semester filters on aggregate queries | MEDIUM | Repository queries | Various |

> *Severity is MEDIUM pending measurement. Should profile actual query times before adding caching. If queries are fast at current scale, caching may be premature optimization.
| **Missing filters**: No time-range, activity type, source-type breakdown | MEDIUM | `StatisticsController.java` | All |
| **No score-source breakdown**: Cannot see how scores were earned (activity vs penalty vs milestone) | MEDIUM | N/A | N/A |
| **Untyped result casting**: Manual `Object[]` to BigDecimal conversions | LOW | `StatisticsServiceImpl.java` | 323-327 |

**Risks of Modification:**
- New endpoints are additive and low-risk
- Modifying existing queries could break frontend expectations if response shape changes

---

### 7. Score Management

**Current Implementation:**
- `ScoreController.java` -- recalculation and history endpoints
- `ScoreServiceImpl.java` (lines 319-425) -- `recalculateAllStudentScores()` method
- `ScoreEntryServiceImpl.java` -- entry upsert and reversal logic

**Business Rules:**
- Individual recalculation: `POST /api/scores/recalculate/student/{studentId}` (synchronous)
- Bulk recalculation: `POST /api/scores/recalculate/all` (synchronous, documented as potentially slow)
- Manual adjustments: Source type `MANUAL_ADJUSTMENT` with `reason` field
- Score entries have `ACTIVE`/`INACTIVE` status for soft-reversal

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **Synchronous bulk recalculation**: `studentRepository.findAll()` then loops all students | **CRITICAL** | `ScoreServiceImpl.java` | 380-406 |
| **No progress tracking**: Bulk recalculation has no status reporting during execution | HIGH | `ScoreServiceImpl.java` | 358-425 |
| **No date-range filtering** on score entries | MEDIUM | `ScoreEntryRepository.java` | All |
| **No keyword search** on score entry reasons | LOW | `ScoreEntryRepository.java` | All |
| **Deprecated method still present**: `calculateTrainingScore()` | LOW | `ScoreController.java` | 72-75 |

**Risks of Modification:**
- Converting to async requires job status tracking infrastructure
- Adding filters is additive and low-risk

---

### 8. Event Notifications

**Current Implementation:**
- `NotificationServiceImpl.java` (lines 89-146) -- `sendBulkNotification()` method
- `FcmService.java` -- Firebase Cloud Messaging integration
- `ReminderScheduleServiceImpl.java` -- Quartz-based reminder scheduling
- `ReminderDispatchService.java` -- reminder execution
- Channels: Database notifications + FCM push + Email

**Business Rules:**
- Bulk notifications: loop over userIds, per user fetch entity, save notification, send FCM per device token
- Reminders: Quartz scheduler triggers configurable days/hours before events
- Notification types: REMINDER_1_DAY, REMINDER_1_HOUR, TASK_OVERDUE, ACTIVITY_REGISTERED, etc.
- Deduplication: DB unique constraint `(user_id, target_type, target_id, reminder_code)`

**Identified Issues:**
| Issue | Severity | File | Line |
|-------|----------|------|------|
| **Synchronous FCM in loop**: Each device token = blocking network call (100-500ms) | HIGH | `NotificationServiceImpl.java` | 131-138 |
| **No @EnableAsync**: `SchedulingConfig.java` has @EnableScheduling but NOT @EnableAsync | HIGH | `SchedulingConfig.java` | 7 |
| **Hardcoded Vietnamese messages**: Notification text embedded in Java code | MEDIUM | `ActivityServiceImpl.java` | 694-707 |
| **N+1 user queries**: `userRepository.findById()` per user in bulk send loop | MEDIUM | `NotificationServiceImpl.java` | 103 |
| **No retry/backoff on FCM failure**: Failed sends are lost | MEDIUM | `NotificationServiceImpl.java` | 131-138 |
| **Inconsistent message formatting** across auto-registration, reminders, and task notifications | LOW | Multiple files | Various |

**Risks of Modification:**
- Async conversion requires careful error handling to prevent lost notifications
- Message template centralization is low-risk (string changes only)

---

## PHASE 2 - IMPLEMENTATION PLAN

---

### Priority Classification

| Priority | Category | Items |
|----------|----------|-------|
| P0 | **Bug Fix (Critical)** | Task Assignment validation gap |
| P1 | **Bug Fix (High)** | Score History pagination, Registration N+1 |
| P2 | **Business Logic** | Preset validation policy, Score management filters |
| P3 | **Enhancement** | Async notifications, Statistics enrichment, Notification templates |
| P4 | **Documentation/Refactoring** | Seminar scoring convention docs, Score history source consolidation, Notification architecture |

---

### Task 1: Fix Event Task Assignment Validation (P0 -- Bug Fix)

**Objective:** Prevent tasks from being assigned to students across activity boundaries.

**Root Cause:** `assignTask()` method validates task existence but NOT task-activity ownership.

**Proposed Solution:** Keep the current API (`POST /api/tasks/assign` with `taskId` + `studentIds` in body). Derive the owning activity from the resolved task entity, then validate that all assigned students are registered for that activity.

**Backend Changes:**
- `ActivityTaskServiceImpl.java` (line 195-207): After resolving `task` from `request.getTaskId()`, extract the owning activity via `task.getActivity()`. Then validate:
  1. Activity is not deleted (already covered by `findByIdAndActivityIsDeletedFalse`)
  2. All students in `request.getStudentIds()` are registered for `task.getActivity().getId()`
- Add batch registration check (replaces per-student N+1):
  ```java
  Activity owningActivity = task.getActivity();
  Set<Long> registeredStudentIds = activityRegistrationRepository
      .findStudentIdsByActivityId(owningActivity.getId());
  List<Long> unregisteredIds = request.getStudentIds().stream()
      .filter(sid -> !registeredStudentIds.contains(sid))
      .toList();
  if (!unregisteredIds.isEmpty()) {
      return new Response(false,
          "Students not registered for this activity: " + unregisteredIds, null);
  }
  ```
- `ActivityRegistrationRepository.java`: Add (or reuse from Task 3):
  ```java
  @Query("SELECT r.student.id FROM ActivityRegistration r WHERE r.activity.id = :activityId")
  Set<Long> findStudentIdsByActivityId(Long activityId);
  ```

**Database Changes:** None required.

**API/DTO Changes:** None -- current `POST /api/tasks/assign` contract unchanged. `TaskAssignmentRequest` unchanged.

**Validation Changes:**
```java
// After line 201 (task = taskOpt.get()), add:
Activity owningActivity = task.getActivity();
if (owningActivity == null || owningActivity.getIsDeleted()) {
    return new Response(false, "Task's owning activity is invalid", null);
}
// Validate students are registered for this activity
Set<Long> registeredStudentIds = activityRegistrationRepository
    .findStudentIdsByActivityId(owningActivity.getId());
List<Long> unregisteredIds = request.getStudentIds().stream()
    .filter(sid -> !registeredStudentIds.contains(sid)).toList();
if (!unregisteredIds.isEmpty()) {
    return new Response(false,
        "Students not registered for activity: " + unregisteredIds, null);
}
```

**Migration/Compatibility:** Forward-only; existing misaligned assignments remain (optional cleanup job).

**Risks:** May reject previously-valid requests if cross-activity assignments exist in production data. Add a one-time data audit query before deploying:
```sql
SELECT ta.id, t.id as task_id, a.id as activity_id, s.id as student_id
FROM task_assignments ta
JOIN activity_tasks t ON ta.task_id = t.id
JOIN activities a ON t.activity_id = a.id
LEFT JOIN activity_registrations ar ON ar.activity_id = a.id AND ar.student_id = ta.student_id
WHERE ar.id IS NULL;
```

**Testing Strategy:**
- Unit test: Assign task to student NOT registered for task's activity -- expect rejection
- Unit test: Assign task to registered student -- expect success
- Unit test: Task with deleted activity -- expect rejection
- Integration test: Verify existing valid assignments still work

---

### Task 2: Fix Score History Pagination & N+1 Queries (P1 -- Bug Fix)

**Objective:** Eliminate in-memory pagination and N+1 queries in score history retrieval.

**Root Cause:** All score entries loaded into memory, reversed, then sliced; series lookups per entry.

**Proposed Solution:** Database-level pagination with batch-loaded relationships. Note: `Activity` has `seriesId` (Long field), NOT a JPA relation to Series -- so `LEFT JOIN FETCH a.series` will NOT compile. Use separate batch queries instead.

**Design for Running Total with Pagination:**

The current `oldScore`/`newScore` values require an ordered accumulation. With DB pagination, we cannot simply slice a page and compute running totals independently. Two concrete options:

**Option A -- Aggregate-offset + DB page (Recommended):**
1. Query the current page of entries in `ORDER BY created_at DESC` with standard JPA `Pageable`.
2. Before the loop, compute the "prior total" = `SUM(points)` for all entries AFTER the current page (i.e., entries with `created_at` older than the page's oldest entry, same student/semester/type filters).
3. Use `priorTotal` as the starting accumulator for `oldScore`/`newScore` in the page loop.
4. This preserves absolute (not page-relative) running totals.
```java
// Step 1: Get page
Page<ScoreEntry> page = scoreEntryRepository.findByStudentAndSemester(
    studentId, semesterId, status, PageRequest.of(pageNum, pageSize, Sort.by("createdAt").descending()));
// Step 2: Compute offset from entries BEFORE this page (older entries)
BigDecimal priorTotal = scoreEntryRepository.sumPointsBeforeTimestamp(
    studentId, semesterId, status, page.getContent().isEmpty() ? null :
    page.getContent().get(page.getContent().size() - 1).getCreatedAt());
// Step 3: Iterate page with priorTotal as starting accumulator
BigDecimal runningTotal = priorTotal;
for (ScoreEntry entry : page.getContent()) {
    BigDecimal oldScore = runningTotal;
    runningTotal = runningTotal.add(entry.getPoints());
    // set oldScore, newScore = runningTotal
}
```

**Option B -- Native query with window function (Alternative):**
Use a native MySQL query with `SUM() OVER (ORDER BY created_at)` for running totals. More complex, ties to MySQL, but avoids the extra aggregate query.

**Option C -- Drop absolute oldScore/newScore:** Change semantics to show only the entry's `points` delta and the student's current total score (from `StudentScore` entity). Simpler but breaks frontend contract.

**Recommendation:** Use Option A. It adds one extra `SUM()` query per request but keeps the existing API contract and absolute running totals.

**Backend Changes:**
- `ScoreEntryRepository.java`: Add paginated query (NO series JOIN FETCH -- Activity has `seriesId` Long, not a JPA relation):
  ```java
  @Query("SELECT se FROM ScoreEntry se LEFT JOIN FETCH se.activity " +
         "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
         "AND se.status = :status ORDER BY se.createdAt DESC")
  Page<ScoreEntry> findWithActivity(..., Pageable pageable);

  @Query("SELECT COALESCE(SUM(se.points), 0) FROM ScoreEntry se " +
         "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
         "AND se.status = :status AND se.createdAt < :cutoff")
  BigDecimal sumPointsBeforeTimestamp(...);
  ```
- `ScoreServiceImpl.java` (lines 470-519): Replace unbounded load + in-memory slice with paginated query + aggregate offset.
- **Fix N+1 for series**: Collect all `activity.getSeriesId()` values from the page, batch-load series via `seriesRepository.findAllById(seriesIds)`, build a `Map<Long, ActivitySeries>` lookup.
- **Fix N+1 for progress**: Same batch approach -- `progressRepository.findAllById(progressIds)`.

**Database Changes:** Add index:
```sql
CREATE INDEX idx_score_entries_student_semester_type ON score_entries(student_id, semester_id, score_type, status, created_at);
```

**API/DTO Changes:** None -- response shape unchanged.

**Risks:** Option A adds one extra aggregate query per history request -- acceptable since it's indexed and bounded. If `created_at` values are identical for many entries, the offset calculation may need tie-breaking on `id`.

**Testing Strategy:**
- Unit test: Verify pagination returns correct page sizes
- Unit test: Verify running totals match legacy in-memory computation across pages
- Unit test: Empty page returns zero prior total
- Performance test: 10K entries should return in <1s

---

### Task 3: Optimize Auto-Registration Performance (P1 -- Bug Fix)

**Objective:** Eliminate memory explosion and N+1 queries during auto-registration.

**Root Cause:** `studentRepository.findAll()` + per-student existence checks.

**Proposed Solution:** Use targeted queries, batch existence checks, and null-safety.

**Backend Changes:**
- `ActivityServiceImpl.java` (line 620): Replace `findAll().stream().filter(!isDeleted)` with `studentRepository.findByIsDeletedFalse()`
- `ActivityServiceImpl.java` (line 630): Add null-guard: `activity.getOrganizers() != null && !activity.getOrganizers().isEmpty()`
- `ActivityServiceImpl.java` (line 645): Replace per-student `existsByActivityIdAndStudentId()` with batch query:
  ```java
  Set<Long> existingStudentIds = activityRegistrationRepository
      .findStudentIdsByActivityId(activity.getId());
  // Then filter: .filter(student -> !existingStudentIds.contains(student.getId()))
  ```
- `ActivityRegistrationRepository.java`: Add `@Query("SELECT r.student.id FROM ActivityRegistration r WHERE r.activity.id = :activityId") Set<Long> findStudentIdsByActivityId(Long activityId);`
- `ActivityServiceImpl.java` (line 658-663): Fix `existsByTicketCode()` N+1 loop. Replace with batch approach:
  1. Generate all ticket codes upfront in a `List<String>` (one per registration).
  2. Query `activityRegistrationRepository.findAllExistingTicketCodes(candidateCodes)` in one DB call.
  3. Regenerate only the colliding codes with controlled retry (max 3 attempts, then fail with clear error).
  - Alternatively, rely on DB unique constraint on `ticket_code` and use `saveAll()` with a try-catch per batch, regenerating only the failed ones. This is simpler and avoids the pre-check query entirely.
  ```java
  // Batch approach: generate all codes, attempt save, retry failures
  for (ActivityRegistration reg : registrations) {
      reg.setTicketCode(TicketCodeUtils.newTicketCode());
  }
  try {
      activityRegistrationRepository.saveAll(registrations);
  } catch (DataIntegrityViolationException e) {
      // Unique constraint violated; fall back to per-registration save with retry
      for (ActivityRegistration reg : registrations) {
          int attempts = 0;
          do {
              reg.setTicketCode(TicketCodeUtils.newTicketCode());
              attempts++;
          } while (activityRegistrationRepository.existsByTicketCode(reg.getTicketCode()) && attempts < 5);
          activityRegistrationRepository.save(reg);
      }
  }
  ```

**Database Changes:** None.

**API/DTO Changes:** None.

**Risks:** Race condition if two publishes happen simultaneously -- mitigated by DB unique constraint on `(activity_id, student_id)`.

**Testing Strategy:**
- Unit test: 10K students auto-registered without OOM
- Unit test: Null organizers list doesn't cause NPE
- Integration test: Duplicate registrations not created

---

### Task 4: Score Rule Preset Clarity & Validation (P2 -- Business Logic)

**Objective:** Clarify preset vs custom behavior; prevent silent rule overrides; expose structured `supportedRules` so FE can render preset-specific inputs without guessing.

**Root Cause:** No differentiation between preset-generated and manual rules; `applyActivityPreset()` silently overwrites `request.scoreRules` when preset != CUSTOM, so any custom rules sent by FE are replaced without warning. FE already renders the 4 milestone blocks, so `supportedRules` only needs to describe them consistently with other preset rules rather than introduce a separate milestone UI model.

**Where the mapping happens (request -> entity):**
- `ScorePresetServiceImpl.applyActivityPreset(CreateActivityRequest)` (line 119-135): Calls `previewActivityPreset()` then `request.setScoreRules(preview.getScoreRules())` -- this OVERWRITES any scoreRules the FE sent alongside a non-CUSTOM preset.
- `ScorePresetServiceImpl.applyActivityPreset(StandardActivityCreateRequest)` (line 138-154): Same overwrite pattern.
- `ScorePresetServiceImpl.applyActivityPreset(StandardActivityUpdateRequest)` (line 157-175): Same overwrite on update.
- The actual DB persistence happens downstream in `ActivityServiceImpl` / `ActivityScoreRuleService` when the request's `scoreRules` list is mapped to `ActivityScoreRule` entities and saved.

**Proposed Solution:**

**Preset metadata contract:**
- Add `supportedRules` to `ActivityPresetDefinitionResponse` and `SeriesPresetDefinitionResponse`.
- Each descriptor should identify the rule, whether it is mandatory or optional, whether it is enabled by default, and which config fields FE should render.
- For milestone presets, keep using the same descriptor shape. FE already knows how to render the 4 milestone blocks, so the backend only needs to mark them as supported rules and list the related config fields.

**Validation Policy (choose one and document):**
- **Policy A -- Reject (Recommended):** If `presetCode != CUSTOM` AND `request.scoreRules` is non-empty, return 400 with message: "Cannot send custom scoreRules with a non-CUSTOM preset. Select CUSTOM preset to provide manual rules."
- **Policy B -- Accept and document override:** If both are present, preset rules override custom rules. Document this clearly in API docs and FE preset UI.

**Backend Changes:**
- `ScorePresetServiceImpl.java`: Before `request.setScoreRules(...)`, add validation:
  ```java
  // Policy A: Reject conflicting input
  if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot send custom scoreRules with preset " + request.getPresetCode() +
          ". Use CUSTOM preset for manual rules.");
  }
  ```
  Add the same check in all three `applyActivityPreset()` overloads.
- `ScorePresetServiceImpl.java`: Populate `supportedRules` in preset definition responses from the same preset source as `previewActivityPreset()` so metadata and generated rules do not drift.
- `ActivityScoreRule` entity: Add `Boolean isPresetGenerated` field (default `false`)
- `ScorePresetServiceImpl.java` (line 134): Mark generated rules with `isPresetGenerated = true` when persisting
- `ActivityScoreRuleRequest.java`: Add `@AssertTrue` validation for `semesterPolicy = EXPLICIT_SEMESTER` requiring `explicitSemesterId`
- `ActivityScoreRuleResponse.java`: Expose `isPresetGenerated` field

**Database Changes:**
```sql
ALTER TABLE activity_score_rules ADD COLUMN is_preset_generated BOOLEAN DEFAULT FALSE;
```

**Frontend Changes (Recommended):**
- When preset selected: Show generated rules as read-only preview, disable scoreRules editor
- Render `supportedRules` as the source of truth for which controls appear for each preset
- Keep milestone rendering as-is; only bind its config inputs to the new rule descriptor metadata
- When CUSTOM selected: Show empty editable rules form
- Display "Preset" badge on preset-generated rules in edit view
- Allow "Unlock" action for admins to modify preset rules (sets `isPresetGenerated = false`, switches preset to CUSTOM)

**API/DTO Changes:**
- `ActivityScoreRuleResponse`: Add `isPresetGenerated: boolean` field
- Error response: 400 when preset != CUSTOM and scoreRules non-empty (Policy A)

**Risks:** Migration adds nullable column -- zero risk to existing data. Policy A is a breaking change if FE currently sends both -- coordinate with FE team.

**Testing Strategy:**
- Unit test: Preset rules marked correctly on creation
- Unit test: Sending preset + custom scoreRules returns 400 (Policy A)
- Unit test: CUSTOM preset with custom scoreRules succeeds
- Unit test: Validation rejects EXPLICIT_SEMESTER without semesterId
- Unit test: `supportedRules` is returned for all preset definitions, including milestone presets
- Integration test: Update with preset replaces old preset rules but preserves manual ones

---

### Task 5: Seminar Session Scoring -- Documentation & Preset UI (P4 -- Documentation)

> **Priority lowered from P2 to P4.** This is a business-rule clarification, not a bug. A backend-only log warning is invisible to FE/admins and provides no real enforcement value.

**Objective:** Document the "1 point per session" convention clearly so FE preset UI can enforce it visually.

**Root Cause:** The ENTERPRISE_SEMINAR_BASIC preset defaults to 1 point per session, but this is a convention, not a hard constraint.

**Proposed Solution -- Documentation + Preset UI constraint (no backend enforcement):**
- Document in API docs and preset descriptions that ENTERPRISE_SEMINAR_BASIC = 1 CHUYEN_DE point per session by convention.
- FE preset UI should show "1 point per session" as a non-editable default when ENTERPRISE_SEMINAR_BASIC is selected.
- If admin switches to CUSTOM preset, they can freely modify the value.
- No backend validation added -- it adds complexity without proportional benefit, and legitimate custom seminar configs would be blocked.

**Backend Changes:** None.

**Database Changes:** None.

**Frontend Changes:**
- Preset UI shows "1 diem/buoi" as locked default for ENTERPRISE_SEMINAR_BASIC
- Tooltip explaining the convention
- CUSTOM preset unlocks all fields

**Risks:** None -- purely a UI/documentation concern.

**Testing Strategy:**
- Manual test: Preset UI shows correct default and lock state

**Revisit if:** Stakeholder requests hard enforcement. In that case, add a `ValidationResult` wrapper to the API response that includes warnings (not just errors), and have FE display them inline.

---

### Task 6: Statistics Module Enhancement (P3 -- Enhancement)

**Objective:** Add missing metrics, filters, and score-source breakdown.

**Root Cause:** Existing stats lack filtering and source-type breakdown. Caching is absent but should be measured before adding.

**Proposed Solution:** Additive-only new endpoints and filters. Profile before caching.

**Caching Strategy -- Measure First:**
- Before adding any cache, profile the 6 existing statistics endpoints under realistic load (concurrent users, typical data volume).
- If queries return in <200ms at current scale, caching is premature optimization -- skip it.
- If queries exceed 500ms or are called >10 times/minute by the same user, add Spring `@Cacheable` with local cache (Caffeine or ConcurrentHashMap).
- **Cache invalidation triggers** (if caching is added):
  - `activity_registrations` INSERT/DELETE -> evict activity stats, dashboard
  - `score_entries` INSERT/UPDATE -> evict score stats, dashboard, student stats
  - `activities` INSERT/UPDATE/DELETE -> evict activity stats, dashboard
  - `semester` change -> evict all stats for that semester
  - TTL: 5 minutes max as a safety net

**Backend Changes:**
- `StatisticsController.java`: Add new endpoint `GET /api/statistics/scores/breakdown?semesterId=&studentId=&departmentId=` -- returns score totals grouped by `ScoreEntrySourceType`
- `StatisticsServiceImpl.java`: Implement breakdown logic using existing `ScoreEntryRepository`
- Add optional query params to existing endpoints: `startDate`, `endDate`, `activityType`, `scoreType`
- `ScoreEntryRepository.java`: Add:
  ```java
  List<ScoreEntry> findByStudentIdAndSemesterIdAndSourceTypeAndStatus(...);
  List<ScoreEntry> findByStudentIdAndSemesterIdAndCreatedAtBetween(...);
  ```

**Database Changes:** Add indexes for common filter patterns:
```sql
CREATE INDEX idx_score_entries_source_type ON score_entries(source_type, status, semester_id);
CREATE INDEX idx_score_entries_created_at ON score_entries(student_id, semester_id, created_at);
```

**API/DTO Changes:** New response DTO `ScoreBreakdownResponse` with per-source-type totals.

**Frontend Changes:**
- New statistics dashboard cards showing score composition
- Filter dropdowns for time range, activity type, department

**Risks:** Additive endpoints -- zero risk to existing functionality.

**Testing Strategy:**
- Unit test: Breakdown returns correct totals per source type
- Performance test: Profile existing endpoints first; if >500ms, add caching and re-test

---

### Task 7: Score Management Filtering & Async Recalculation (P2/P3 -- Business Logic + Enhancement)

**Objective:** Add filtering capabilities and convert bulk recalculation to async with proper job semantics.

**Root Cause:** No date-range/keyword filters; synchronous bulk recalculation causes timeouts.

**Proposed Solution:**

**Backend Changes (Filtering -- P2):**
- `ScoreEntryRepository.java`: Add query methods for date-range and reason-keyword filtering
- `ScoreController.java`: Add optional query params `startDate`, `endDate`, `keyword` to history endpoint

**Backend Changes (Async Recalculation -- P3):**
- `SchedulingConfig.java`: Add `@EnableAsync` and configure `ThreadPoolTaskExecutor`
- `ScoreServiceImpl.java`: Create async variant of `recalculateAllStudentScores()` that:
  1. Returns immediately with job ID
  2. Processes students in batches of 100
  3. Tracks progress in a `RecalculationJob` entity
- New endpoint: `GET /api/scores/recalculate/status/{jobId}` -- returns job progress
- Keep existing synchronous endpoint for backward compatibility but add student-count threshold warning

**Job Semantics (required for durability and correctness):**

| Concern | Design |
|---------|--------|
| **Transaction boundary** | Each batch of 100 students is one transaction. If a batch fails, log the error, increment `error_count`, and continue with the next batch. Do NOT roll back the entire job. |
| **Idempotency** | Recalculation is naturally idempotent -- it recomputes from `ScoreEntry` ledger. Running the same student twice produces the same result. The job record's `semester_id` + `created_by` + `status` combination prevents accidental double-submission. |
| **Concurrency lock** | Before starting, check `SELECT COUNT(*) FROM recalculation_jobs WHERE semester_id = ? AND status IN ('PENDING', 'RUNNING')`. If > 0, reject with 409 Conflict: "A recalculation job is already running for this semester." Additionally, use a DB-level advisory lock or `SELECT ... FOR UPDATE` on the job row during execution. |
| **Retry on app restart** | On application startup, scan for `status = 'RUNNING'` jobs (indicates crash mid-execution). Set them to `'FAILED'` with a note. Provide an admin endpoint `POST /api/scores/recalculate/retry/{jobId}` that creates a NEW job for the same semester, skipping already-processed students (check `processed_students` count and resume from offset). |
| **Progress tracking** | After each batch: `UPDATE recalculation_jobs SET processed_students = processed_students + batchSize WHERE id = ?`. Status transitions: PENDING -> RUNNING -> COMPLETED (or FAILED if error_count > threshold). |
| **Timeout protection** | Set a max job duration (e.g., 30 minutes). If exceeded, set status to `'TIMEOUT'` and stop processing. Admin can retry. |

**Database Changes:**
```sql
CREATE TABLE recalculation_jobs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  total_students INT NOT NULL,
  processed_students INT DEFAULT 0,
  error_count INT DEFAULT 0,
  error_details TEXT,
  started_at DATETIME,
  completed_at DATETIME,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_semester_active (semester_id, status)
);
```
Note: The `UNIQUE KEY uk_semester_active` only partially prevents duplicates (since status values differ). The concurrency check in application code is the primary guard.

**API/DTO Changes:**
- New `RecalculationJobResponse` DTO (id, semesterId, status, totalStudents, processedStudents, errorCount, startedAt, completedAt)
- Existing recalculate endpoint unchanged (backward compatible)
- New: `POST /api/scores/recalculate/retry/{jobId}` -- retry failed job

**Risks:** Async introduces eventual consistency -- admin must understand scores may take minutes to update. Job failure leaves partial results that need admin attention.

**Testing Strategy:**
- Unit test: Batch processing handles individual student failures gracefully (continues to next batch)
- Unit test: Concurrent job submission for same semester returns 409
- Integration test: Job status transitions correctly: PENDING -> RUNNING -> COMPLETED
- Integration test: App restart marks RUNNING jobs as FAILED
- Load test: 10K students recalculated within 5 minutes

---

### Task 8: Notification System Improvements (P3 -- Enhancement)

**Objective:** Centralize message templates, enable async FCM, and unify Vietnamese localization.

**Root Cause:** Hardcoded Vietnamese strings, synchronous FCM calls, inconsistent messaging.

**Proposed Solution (Phased):**

**Phase A -- Message Template Centralization (Low Risk):**
- Create `src/main/resources/messages_vi.properties` with all notification message keys
- Create `NotificationMessageTemplate.java` enum or use Spring `MessageSource`
- Replace hardcoded strings in:
  - `ActivityServiceImpl.java` (lines 694-707)
  - `ReminderScheduleServiceImpl.java` (reminder content)
  - `ActivityRegistrationServiceImpl.java` (status change messages)

**Phase B -- Async Notification (Medium Risk):**
- `SchedulingConfig.java`: Add `@EnableAsync` (shared with Task 7)
- `NotificationServiceImpl.java`: Create `@Async sendBulkNotificationAsync()` method
- Batch device tokens and use `CompletableFuture` for parallel FCM sends
- Add retry with exponential backoff for failed FCM calls

**Phase C -- Notification Consistency Audit (Low Risk):**
- Review all notification types for consistent:
  - Title format: "Action -- Context" pattern
  - Content format: Brief description with activity name
  - Vietnamese grammar and terminology alignment
- Document notification style guide

**Database Changes:** None for Phase A/C. Phase B optionally adds `notification_failures` table for DLQ.

**Frontend Changes:** None required -- notifications rendered from stored content.

**Risks:** Phase A is string-only changes (very low risk). Phase B requires careful async error handling.

**Testing Strategy:**
- Unit test: All message templates render correctly with parameters
- Integration test: Async bulk send completes without lost notifications
- Manual review: Vietnamese localization consistency

---

## IMPLEMENTATION ORDER & DEPENDENCIES

```
Task 1  (Task Assignment Fix)       -- [Independent, P0]
Task 3  (Registration Optimization) -- [Independent, P1]
Task 2  (Score History Pagination)  -- [Independent, P1]
Task 4  (Preset Clarity)            -- [Independent, P2]
Task 7a (Score Filtering)           -- [Depends on Task 2 index, P2]
Task 6  (Statistics Enhancement)    -- [Depends on Task 7a repo methods, P3]
Task 7b (Async Recalculation)       -- [Independent, P3]
Task 8a (Message Templates)         -- [Independent, P3]
Task 8b (Async Notifications)       -- [Depends on Task 7b @EnableAsync, P3]
Task 8c (Notification Consistency)  -- [Depends on Task 8a, P3]
Task 5  (Seminar Documentation)     -- [No code deps, P4 -- can be done anytime]
```

**Recommended Execution Waves:**

| Wave | Tasks | Duration | Risk |
|------|-------|----------|------|
| **Wave 1** | Task 1 + Task 3 + Task 2 | 2-3 days | Low (isolated fixes) |
| **Wave 2** | Task 4 + Task 7a + Task 8a | 2-3 days | Low-Medium |
| **Wave 3** | Task 6 + Task 7b + Task 8b + Task 8c | 4-5 days | Medium |
| **Wave 4** | Task 5 (docs only) | <1 day | None |

---

## REJECTED ALTERNATIVES

| Alternative | Rejected Because |
|-------------|-----------------|
| **Redis caching for statistics** | Adds infrastructure dependency; Spring `@Cacheable` with local cache sufficient at current scale (&lt;100K students) |
| **Kafka/RabbitMQ for notifications** | Overengineered; `@Async` with ThreadPoolExecutor achieves same result without new infrastructure |
| **Full score history source consolidation** (removing ActivityParticipation source) | Breaking change; frontend may depend on participation details. Defer to separate refactor after confirming FE doesn't use this data |
| **Manual adjustment approval workflow** | Significant new feature (new tables, new UI, new endpoints); out of scope for current audit. Can be Phase 2 enhancement |
| **Notification preferences/opt-out** | New feature requiring table, UI, and logic changes; defer to separate feature request |
| **Database-level running totals via window functions** | MySQL window functions in JPA require native queries; aggregate-offset approach (Option A) achieves same result with standard JPA pagination. Window function approach (Option B) kept as alternative for future optimization |

---

## AMBIGUITIES & QUESTIONS FOR STAKEHOLDER

1. ~~**Task Assignment -- Should we also require students be registered for the activity?**~~ **RESOLVED**: Yes, the plan now validates that all assigned students are registered for the task's owning activity. See Task 1 for details.

2. **Score History -- Should we deprecate the ActivityParticipation section?** The response currently returns BOTH score entries AND participation details. If the score entries ledger is the source of truth, the participation section may be redundant.

3. **Preset Rules -- Policy A (reject) or Policy B (document override)?** Task 4 now presents two concrete policies. Recommend Policy A (reject custom scoreRules when preset != CUSTOM). Stakeholder should confirm.

4. ~~**Seminar Points -- Should the "exactly 1 point" constraint be hard (rejection) or soft (warning)?**~~ **RESOLVED**: Neither. Downgraded to documentation/preset UI convention only (Task 5, P4). No backend enforcement.

5. **Async Recalculation -- What's the acceptable threshold before forcing async?** The plan suggests >1000 students triggers async-only. Should this be configurable?
