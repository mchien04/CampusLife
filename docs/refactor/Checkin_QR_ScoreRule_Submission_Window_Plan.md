# Check-in QR Score Award Breakdown + Submission Window Plan

> Scope: integrate QR check-in completion with the new score rule engine reliably, return concrete score awards by score type, and block task submissions outside the configured submission window.
>
> Decision: backend will use the new score rule engine as the source of truth. Do not support the old frontend/old-engine mental model. Frontend will be updated later to render the new award breakdown contract.

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
- More importantly, `pointsEarned` is a single aggregate number and cannot represent mixed awards such as `+5 diem ren luyen` and `+1 buoi chuyen de`. The new backend contract must return an award breakdown, not only a total.

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
- FE must move away from displaying only `ActivityParticipationResponse.pointsEarned` for the QR success screen. Backend will return a concrete award list from the new engine so FE can render each award line separately.

### 3. Award display requirement

Target UI examples:
- `+5 diem ren luyen`
- `+1 buoi chuyen de`

Backend should return structured data for this, not a pre-summed cross-type total. A single scan/submit can apply multiple rules with different `scoreType` values.

Recommended response addition:
```json
{
  "scoreAwards": [
    {
      "scoreType": "REN_LUYEN",
      "scoreTypeLabel": "Diem ren luyen",
      "points": 5,
      "displayUnit": "diem",
      "displayText": "+5 diem ren luyen",
      "triggerType": "PARTICIPATION_COMPLETED",
      "ruleId": 101
    },
    {
      "scoreType": "CHUYEN_DE",
      "scoreTypeLabel": "Buoi chuyen de",
      "points": 1,
      "displayUnit": "buoi",
      "displayText": "+1 buoi chuyen de",
      "triggerType": "PARTICIPATION_COMPLETED",
      "ruleId": 102
    }
  ]
}
```

Notes:
- `displayText` is optional convenience. FE can render from `points`, `scoreTypeLabel`, and `displayUnit`.
- `pointsEarned` may remain on `ActivityParticipationResponse` for existing data shape, but it must not be the primary success-display field for the new engine flow.
- Do not sum across score types for user-facing display.

### 4. Submission window

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
- Allow submission before attendance/check-in is completed.
- Treat scoring completion as a two-condition gate: attendance confirmed AND submission graded.
- Do not add new DB fields unless FE/product requires an independent submission start/end window.

---

## Implementation Plan

### Task 1: Return QR check-in award breakdown from the new engine

Objective:
- When QR check-in completes a non-submission standalone activity, apply matching score rules and return the exact awards grouped by rule/score type.

Backend changes:
- Keep `checkInByQrCode(...) -> finalizeAttendanceOutcome(...)` as the main path.
- Keep using `ScoreRuleTrigger.PARTICIPATION_COMPLETED` for QR attendance completion.
- Update score application so applied rules are returned as structured award items.
- Add award breakdown to the response DTO used by QR check-in.
- Do not use a total-only value for FE success display.
- Ignore `ActivityParticipationRequest.pointsEarned` for check-in scoring. This field is not trusted input in the new engine flow.

Recommended implementation:
1. Add a new DTO/model such as `ScoreAwardResponse` or `AppliedScoreAward`.
2. Change `ScoreRuleEngine.applyActivityCompleted(...)` from `void` to returning `List<AppliedScoreAward>`.
3. In `ScoreRuleEngineImpl.applyActivityCompleted(...)`, create one award item per applied rule.
3. Continue writing score entries via `scoreEntryService.upsertEntry(...)`.
4. Return the applied award list.
5. In `ActivityRegistrationServiceImpl.applyStandaloneOrSeriesAttendanceResult(...)`, attach the returned awards to `ActivityParticipationResponse`.
6. Optionally set `participation.pointsEarned` to the sum of awards for persistence/backward-compatible reporting, but do not rely on it for the QR success display.

Implementation direction:
- Change the engine contract directly to return applied award items. Avoid compatibility wrappers for the old engine flow.
- Keep all score writes inside `ScoreRuleEngineImpl`; service layers should not manually calculate points.
- Use score entries as the audit/source-of-truth record. The returned awards should mirror the entries written by `ScoreEntryService`.

Suggested award DTO:
```java
public class AppliedScoreAward {
    private Long ruleId;
    private ScoreType scoreType;
    private String scoreTypeLabel;
    private BigDecimal points;
    private String displayUnit;
    private String displayText;
    private ScoreRuleTrigger triggerType;
    private Long scoreEntryId; // optional if upsertEntry returns entity/id later
}
```

Display mapping:
- `REN_LUYEN` -> label `Diem ren luyen`, unit `diem`
- `CHUYEN_DE` -> label `Buoi chuyen de`, unit `buoi`
- Other score types -> label from enum/name mapping, unit `diem` unless business defines otherwise

Files:
- `src/main/java/vn/campuslife/service/ScoreRuleEngine.java`
- `src/main/java/vn/campuslife/service/impl/ScoreRuleEngineImpl.java`
- `src/main/java/vn/campuslife/model/score/AppliedScoreAward.java` or equivalent
- `src/main/java/vn/campuslife/model/activity/ActivityParticipationResponse.java`
- `src/main/java/vn/campuslife/service/impl/ActivityRegistrationServiceImpl.java`
- Tests under `src/test/java/vn/campuslife/service/impl/`

Acceptance criteria:
- QR check-in for standalone activity with `PARTICIPATION_COMPLETED/FIXED_POINTS` rule creates score entry.
- Response includes `data.scoreAwards`.
- Response can represent mixed awards, e.g. `REN_LUYEN +5` and `CHUYEN_DE +1`, without collapsing them into one display value.
- Response points are computed from `ActivityScoreRule`, not from request payload or old FE logic.
- Re-scanning completed QR does not create duplicate score entries and does not double points.
- Series activity behavior remains unchanged: series progress handles scoring.
- Submission-required activity still waits until both attendance is confirmed and submission is graded before awarding score.

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
  - FE should render each item from `response.body.scoreAwards`.
  - FE should not display a cross-type total as the primary result.
  - FE should not infer points from old preset/engine fields.

Only add `ScoreRuleTrigger.ATTENDED` in a later backend migration if product needs separate rules for:
- Award points immediately on attendance but before completion.
- Award different points for `ATTENDED` vs `COMPLETED`.

---

### Task 3: Enforce submission time window

Objective:
- Prevent students from submitting proof/report outside the configured window.
- Allow students to submit before QR/check-in completion, while keeping scoring gated by attendance + grading.

Phase 1 rule:
- A task submission is allowed only when:
  1. Task exists and belongs to a non-deleted activity.
  2. Activity is published.
  3. Student has a valid registration for the task's activity.
  4. If `task.deadline` is set, `now <= task.deadline`.
  5. Attendance is NOT required at submit time.
  6. Score awards are created only after both conditions are true: submission is graded and attendance is confirmed.

Backend changes:
- In `TaskSubmissionServiceImpl.submitTask(...)`, after loading task/student and before storing files:
  - Resolve `Activity activity = task.getActivity()`.
  - Reject if `activity == null`, deleted, or draft.
  - Reject if `task.getDeadline() != null && now.isAfter(task.getDeadline())`.
  - Validate registration exists for `(activityId, studentId)`.
  - Do not require registration status `ATTENDED` at submit time.
  - After saving a submission, if it is immediately graded by business logic, call the same finalization helper; if attendance is not confirmed yet, store the graded submission and defer scoring.

Update submission:
- Apply the same deadline check to `updateSubmission(...)`.
- Allow manager/admin grading after deadline; only student create/update should be blocked.

Order-independent finalization:
- If submission is graded before attendance:
  - Save the submission as `GRADED`.
  - Do not create score entries yet.
  - Do not mark participation `COMPLETED` yet.
  - When QR/check-in later marks attendance, `finalizeAttendanceOutcome(...)` should find the latest graded submission and apply `SUBMISSION_GRADED` awards.
- If attendance happens before submission grading:
  - QR/check-in marks participation `ATTENDED`.
  - No score awards are returned yet.
  - When grading happens later, `finalizeSubmissionResultIfEligible(...)` should complete participation and apply `SUBMISSION_GRADED` awards.
- If submission is auto-graded immediately during submit:
  - Run `finalizeSubmissionResultIfEligible(...)` immediately after saving/grading.
  - If attendance already exists, award immediately.
  - If attendance does not exist, return submission success with no score awards and let QR/check-in award later.

Files:
- `src/main/java/vn/campuslife/service/impl/TaskSubmissionServiceImpl.java`
- `src/main/java/vn/campuslife/repository/ActivityRegistrationRepository.java` if a helper query is needed
- Tests under `TaskSubmissionServiceImplTest`

Acceptance criteria:
- Submit before deadline succeeds.
- Submit before attendance succeeds.
- Submit after deadline returns 400 with clear message.
- Update after deadline returns 400 unless product decides edits are allowed after deadline.
- Grading after deadline still succeeds.
- Submission for draft/deleted activity is rejected.
- Submission by student not registered for activity is rejected.
- Auto-graded submission before attendance does not create score entries until QR/check-in attendance is confirmed.
- Auto-graded submission after attendance creates `SUBMISSION_GRADED` score awards immediately.

---

### Task 4: Sync submission grading with participation points

Objective:
- For submission-required activities, when grading finalizes participation, response/reporting should reflect concrete award items from `SUBMISSION_GRADED`.

Current behavior:
- `TaskSubmissionServiceImpl.finalizeSubmissionResultIfEligible(...)` sets `participation.pointsEarned = 0`.
- Then calls `scoreRuleEngine.applySubmissionGraded(...)`.
- Engine writes score entries but does not return/update `pointsEarned`.

Backend changes:
- Apply the same award-list pattern as Task 1 to `applySubmissionGraded(...)`.
- When participation is finalized from graded submission, attach returned awards to the relevant response path.
- Optionally persist aggregate `participation.pointsEarned` as the sum for legacy reporting, but user-facing display should consume `scoreAwards`.
- Do not preserve old-engine scoring behavior for submissions. `SUBMISSION_GRADED` rules in the new engine are the only source of score entries.

Acceptance criteria:
- Grading a passed submission writes score entries and returns/stores award breakdown.
- Grading a failed submission applies `failPoints` according to the rule calculation/sign policy and returns/stores award breakdown.
- Existing `TaskSubmissionResponse.score = 0.0` remains backward-compatible unless deliberately replaced later.
- No score is derived from old FE fields or manually submitted `pointsEarned`.

---

## Test Plan

### Unit tests

- `ActivityRegistrationServiceImplTest`
  - QR check-in applies `PARTICIPATION_COMPLETED` rule.
  - QR check-in response includes `scoreAwards`.
  - QR check-in response can include multiple awards with different `scoreType` values.
  - QR check-in does not double-award on duplicate scan.
  - QR check-in for `requiresSubmission=true` marks attendance but does not complete until graded submission exists.
  - QR check-in after a pre-graded submission completes participation and returns `SUBMISSION_GRADED` awards.

- `ScoreRuleEngineImplTest`
  - `applyActivityCompleted` returns one award item per applied rule.
  - Disabled rules are ignored.
  - Audience-ineligible rules are ignored.
  - Existing score entry is upserted, not duplicated.

- `TaskSubmissionServiceImplTest`
  - Submit before deadline succeeds.
  - Submit before attendance succeeds.
  - Submit after deadline fails.
  - Update after deadline fails.
  - Grade after deadline succeeds.
  - Submission without registration fails.
  - Auto-graded submission before attendance defers score entry creation.
  - Auto-graded submission after attendance creates score awards immediately.

### Integration tests

- End-to-end QR check-in:
  - Register student.
  - Approve registration.
  - Configure two `PARTICIPATION_COMPLETED/FIXED_POINTS` rules with different score types.
  - Call `POST /api/registrations/checkin/qr`.
  - Assert participation response `scoreAwards` contains both items.
  - Assert score entries are written for both score types.

- End-to-end submission activity:
  - Create submission-required activity and task with deadline.
  - Student submits within deadline.
  - Manager grades.
  - Assert `SUBMISSION_GRADED` score entries and award breakdown.

---

## Migration / API Notes

- No DB migration required for phase 1 if using existing `ActivityTask.deadline`.
- No new trigger required if using `PARTICIPATION_COMPLETED`.
- API response shape changes additively: `ActivityParticipationResponse` should include `scoreAwards`.
- `ActivityParticipationResponse.pointsEarned` may remain as an aggregate/backward-compatible field, but the new-engine display contract is `scoreAwards`.
- Backend contract uses the new engine only:
  - Activity attendance score: `PARTICIPATION_COMPLETED`
  - Submission score: `SUBMISSION_GRADED`
  - Score history source: score entries written by `ScoreEntryService`
- FE follow-up:
  - Stop expecting an `ATTENDED` score trigger from backend.
  - Stop using `pointsEarned` as the primary QR success display.
  - Render each item from `scoreAwards`, e.g. `+5 diem ren luyen`, `+1 buoi chuyen de`.
  - Update preset/rule UI to send new-engine rules, not old-engine assumptions.
- If product later needs independent submission windows, add fields in a separate migration:
  - `ActivityTask.submissionStartAt`
  - `ActivityTask.submissionDeadline`
  - or activity-level equivalents if one window applies to all tasks.

---

## Open Decisions

1. ~~Should submission be allowed before attendance, or only after QR/check-in confirms attendance?~~
   - Resolved: allow submission before attendance. Score is awarded only when both attendance and grading are complete.

2. Should students be allowed to edit/update a submission after deadline if the first submission was before deadline?
   - Recommended: block student updates after deadline unless an admin reopens the task.

3. ~~Should backend introduce `ScoreRuleTrigger.ATTENDED`?~~
   - Resolved for this scope: no. Backend uses the new engine with `PARTICIPATION_COMPLETED`; FE will align later.
