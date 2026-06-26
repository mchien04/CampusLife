# Check-in QR Score Rule + Submission Window Plan

> Scope: integrate QR check-in completion with the new score rule engine reliably, return computed `pointsEarned`, and block task submissions outside the configured submission window.
>
> Decision: backend will use the new score rule engine as the source of truth. Frontend is currently still following the old engine mental model and will be updated after the backend contract is fixed.

---

## Current Findings

### 1. QR check-in flow

Endpoint:
- `POST /api/registrations/checkin/qr`
- Controller: `ActivityRegistrationController.checkInByQrCode(...)`
- Service: `ActivityRegistrationServiceImpl.checkInByQrCode(...)`

Current flow in `ActivityRegistrationServiceImpl.checkInByQrCode(...)`:
1. Find activity by `checkInCode`.
2. Reject draft activity.
3. Find approved registration for authenticated student.
4. Find or create `ActivityParticipation` with `pointsEarned = 0`.
5. Validate check-in window using activity start/end time.
6. Mark participation as `ATTENDED`.
7. Call `finalizeAttendanceOutcome(...)`.
8. Return `ActivityParticipationResponse`.

Important nuance:
- The current working tree already calls `finalizeAttendanceOutcome(...)` from QR check-in.
- For non-submission, non-series activities, `finalizeAttendanceOutcome(...)` calls `scoreRuleEngine.applyActivityCompleted(...)`.
- However, `markParticipationCompleted(...)` still sets `pointsEarned = 0`, and `ScoreRuleEngineImpl.applyActivityCompleted(...)` writes score entries but does not update `ActivityParticipation.pointsEarned`.
- Result: score entry may be created, but response can still show `pointsEarned = 0`.

### 2. Score trigger naming

`ScoreRuleTrigger` currently has:
- `PARTICIPATION_COMPLETED`
- `NO_SHOW`
- `SUBMISSION_GRADED`
- `MINIGAME_PASSED`
- `MINIGAME_EXHAUSTED_ATTEMPTS`
- `SERIES_MILESTONE_REACHED`
- `TASK_OVERDUE`

There is no `ATTENDED` trigger today.

Recommendation:
- Do not add `ATTENDED` in this change unless the product explicitly wants a separate scoring concept.
- Treat successful QR attendance as the same scoring event as `PARTICIPATION_COMPLETED`, because the current engine already uses that trigger and presets are built around it.

Compatibility decision:
- Do not support an old FE/legacy engine trigger alias in backend.
- Do not accept `pointsEarned` from the frontend request as scoring input.
- Backend computes points only from persisted `ActivityScoreRule` rows through `ScoreRuleEngine`.
- FE can continue displaying `ActivityParticipationResponse.pointsEarned`; the value will come from the new engine after this backend change.

### 3. Submission window

Submission entity:
- `TaskSubmission.submittedAt` exists.

Task deadline:
- `ActivityTask.deadline` exists.

Activity entity:
- `Activity.requiresSubmission` exists.
- There are no explicit activity-level fields like `submissionStartAt` / `submissionDeadline`.

Current `TaskSubmissionServiceImpl.submitTask(...)`:
- Validates task exists.
- Validates student exists.
- Rejects duplicate submission.
- Saves submission.
- Updates assignment status.
- Does not check whether the activity/task submission window is open.

Recommendation for phase 1:
- Use `ActivityTask.deadline` as the configured submission deadline.
- Use activity attendance/check-in state as the opening condition if needed.
- Do not add new DB fields unless FE/product requires an independent submission start/end window.

---

## Implementation Plan

### Task 1: Make QR check-in scoring deterministic with the new engine

Objective:
- When QR check-in completes a non-submission standalone activity, apply the matching score rules and return computed `pointsEarned`.

Backend changes:
- Keep `checkInByQrCode(...) -> finalizeAttendanceOutcome(...)` as the main path.
- Keep using `ScoreRuleTrigger.PARTICIPATION_COMPLETED` for QR attendance completion.
- Update score application so the total awarded points is reflected back on `ActivityParticipation.pointsEarned`.
- Ignore `ActivityParticipationRequest.pointsEarned` for check-in scoring. This field is not trusted input in the new engine flow.

Recommended implementation:
1. Change `ScoreRuleEngine.applyActivityCompleted(...)` from `void` to returning `BigDecimal awardedPoints`.
2. In `ScoreRuleEngineImpl.applyActivityCompleted(...)`, sum every applied rule's points.
3. Continue writing score entries via `scoreEntryService.upsertEntry(...)`.
4. Return the total awarded points.
5. In `ActivityRegistrationServiceImpl.applyStandaloneOrSeriesAttendanceResult(...)`, assign that returned value to `participation.pointsEarned` and save participation.

Implementation direction:
- Change the engine contract directly to return applied points. Avoid compatibility wrappers for the old engine flow unless tests reveal a caller that genuinely cannot move yet.
- Keep all score writes inside `ScoreRuleEngineImpl`; service layers should not manually calculate points.

Files:
- `src/main/java/vn/campuslife/service/ScoreRuleEngine.java`
- `src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java`
- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
- Tests under `src/test/java/vn/campuslife/service/impl/`

Acceptance criteria:
- QR check-in for standalone activity with `PARTICIPATION_COMPLETED/FIXED_POINTS` rule creates score entry.
- Response `data.pointsEarned` equals the sum of applied rules.
- Response points are computed from `ActivityScoreRule`, not from request payload or old FE logic.
- Re-scanning completed QR does not create duplicate score entries and does not double points.
- Series activity behavior remains unchanged: series progress handles scoring.
- Submission-required activity still waits for graded submission before completion scoring.

---

### Task 2: Keep rule trigger policy explicit

Objective:
- Avoid mismatch between old FE wording "ATTENDED" and the new backend trigger enum.

Policy:
- Backend scoring trigger for QR attendance completion is `PARTICIPATION_COMPLETED`.
- `ATTENDED` remains a participation state, not a score rule trigger.
- Backend will not introduce a temporary `ATTENDED -> PARTICIPATION_COMPLETED` compatibility layer.

Documentation changes:
- Update FE handoff/API docs to say:
  - QR check-in marks participation as `ATTENDED`.
  - If activity does not require submission, backend finalizes it as `COMPLETED`.
  - Completion scoring uses `ScoreRuleTrigger.PARTICIPATION_COMPLETED`.
  - FE should render points from `response.body.pointsEarned` and should not infer points from old preset/engine fields.

Only add `ScoreRuleTrigger.ATTENDED` in a later backend migration if product needs separate rules for:
- Award points immediately on attendance but before completion.
- Award different points for `ATTENDED` vs `COMPLETED`.

---

### Task 3: Enforce submission time window

Objective:
- Prevent students from submitting proof/report outside the configured window.

Phase 1 rule:
- A task submission is allowed only when:
  1. Task exists and belongs to a non-deleted activity.
  2. Activity is published.
  3. Student has an approved/attended registration for the task's activity.
  4. If `task.deadline` is set, `now <= task.deadline`.
  5. If activity requires attendance before submission, registration status must be `ATTENDED` or participation must be `ATTENDED/COMPLETED`.

Backend changes:
- In `TaskSubmissionServiceImpl.submitTask(...)`, after loading task/student and before storing files:
  - Resolve `Activity activity = task.getActivity()`.
  - Reject if `activity == null`, deleted, or draft.
  - Reject if `task.getDeadline() != null && now.isAfter(task.getDeadline())`.
  - Validate registration exists for `(activityId, studentId)`.
  - For `activity.requiresSubmission == true`, require attendance before submission unless product explicitly allows pre-attendance submission.

Update submission:
- Apply the same deadline check to `updateSubmission(...)`.
- Allow manager/admin grading after deadline; only student create/update should be blocked.

Files:
- `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`
- `src/main/java/vn/campuslife/repository/ActivityRegistrationRepository.java` if a helper query is needed
- Tests under `TaskSubmissionServiceImplTest`

Acceptance criteria:
- Submit before deadline succeeds.
- Submit after deadline returns 400 with clear message.
- Update after deadline returns 400 unless product decides edits are allowed after deadline.
- Grading after deadline still succeeds.
- Submission for draft/deleted activity is rejected.
- Submission by student not registered for activity is rejected.

---

### Task 4: Sync submission grading with participation points

Objective:
- For submission-required activities, when grading finalizes participation, response/reporting should reflect points from `SUBMISSION_GRADED`.

Current behavior:
- `TaskSubmissionServiceImpl.finalizeSubmissionResultIfEligible(...)` sets `participation.pointsEarned = 0`.
- Then calls `scoreRuleEngine.applySubmissionGraded(...)`.
- Engine writes score entries but does not return/update `pointsEarned`.

Backend changes:
- Apply the same awarded-points pattern as Task 1 to `applySubmissionGraded(...)`.
- When participation is finalized from graded submission, set `participation.pointsEarned` to the returned total.
- Do not preserve old-engine scoring behavior for submissions. `SUBMISSION_GRADED` rules in the new engine are the only source of score entries.

Acceptance criteria:
- Grading a passed submission writes score entry and updates participation `pointsEarned`.
- Grading a failed submission applies `failPoints` according to the rule calculation/sign policy and updates participation `pointsEarned`.
- Existing `TaskSubmissionResponse.score = 0.0` remains backward-compatible unless deliberately replaced later.
- No score is derived from old FE fields or manually submitted `pointsEarned`.

---

## Test Plan

### Unit tests

- `ActivityRegistrationServiceImplTest`
  - QR check-in applies `PARTICIPATION_COMPLETED` rule.
  - QR check-in response includes numeric `pointsEarned`.
  - QR check-in does not double-award on duplicate scan.
  - QR check-in for `requiresSubmission=true` marks attendance but does not complete until graded submission exists.

- `ScoreRuleEngineImplTest`
  - `applyActivityCompleted` returns total applied points.
  - Disabled rules are ignored.
  - Audience-ineligible rules are ignored.
  - Existing score entry is upserted, not duplicated.

- `TaskSubmissionServiceImplTest`
  - Submit before deadline succeeds.
  - Submit after deadline fails.
  - Update after deadline fails.
  - Grade after deadline succeeds.
  - Submission without registration fails.

### Integration tests

- End-to-end QR check-in:
  - Register student.
  - Approve registration.
  - Configure `PARTICIPATION_COMPLETED/FIXED_POINTS` rule.
  - Call `POST /api/registrations/checkin/qr`.
  - Assert participation response points and score entry.

- End-to-end submission activity:
  - Create submission-required activity and task with deadline.
  - Student submits within deadline.
  - Manager grades.
  - Assert `SUBMISSION_GRADED` score entry and participation points.

---

## Migration / API Notes

- No DB migration required for phase 1 if using existing `ActivityTask.deadline`.
- No new trigger required if using `PARTICIPATION_COMPLETED`.
- API response shape remains unchanged; `ActivityParticipationResponse.pointsEarned` is already `BigDecimal` and serializes as JSON number.
- Backend contract uses the new engine only:
  - Activity attendance score: `PARTICIPATION_COMPLETED`
  - Submission score: `SUBMISSION_GRADED`
  - Score history source: score entries written by `ScoreEntryService`
- FE follow-up:
  - Stop expecting an `ATTENDED` score trigger from backend.
  - Keep reading `pointsEarned` from QR check-in response.
  - Update preset/rule UI to send new-engine rules, not old-engine assumptions.
- If product later needs independent submission windows, add fields in a separate migration:
  - `ActivityTask.submissionStartAt`
  - `ActivityTask.submissionDeadline`
  - or activity-level equivalents if one window applies to all tasks.

---

## Open Decisions

1. Should submission be allowed before attendance, or only after QR/check-in confirms attendance?
   - Recommended: require attendance for `requiresSubmission=true` activities.

2. Should students be allowed to edit/update a submission after deadline if the first submission was before deadline?
   - Recommended: block student updates after deadline unless an admin reopens the task.

3. ~~Should backend introduce `ScoreRuleTrigger.ATTENDED`?~~
   - Resolved for this scope: no. Backend uses the new engine with `PARTICIPATION_COMPLETED`; FE will align later.
