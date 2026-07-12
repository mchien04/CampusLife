package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.*;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ManualScoreService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreEntryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManualScoreServiceImpl implements ManualScoreService {

    private static final int MAX_BULK_ENTRIES = 200;

    private final ManualScoreAdjustmentRepository manualScoreAdjustmentRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ActivityRepository activityRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScoreEntryService scoreEntryService;
    private final NotificationService notificationService;
    private final DepartmentAuthorizationService departmentAuthorizationService;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public Response createManualAdjustment(ManualScoreRequest request, User actor, DepartmentScope scope) {
        if (request.getSemesterId() == null) {
            throw new BadRequestException("semesterId is required");
        }
        if (request.getPoints() == null) {
            throw new BadRequestException("points is required");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("reason is required");
        }

        ManualScoreResponse created = createOne(
                request.getStudentId(),
                request.getSemesterId(),
                request.getScoreType(),
                request.getPoints(),
                request.getReason().trim(),
                request.getActivityId(),
                actor,
                scope);

        return Response.success("Manual score adjustment created", created);
    }

    @Override
    public Response createBulkManualAdjustments(BulkManualScoreRequest request, User actor, DepartmentScope scope) {
        if (request.getSemesterId() == null) {
            throw new BadRequestException("semesterId is required (học kỳ tích điểm)");
        }
        if (request.getScoreType() == null) {
            throw new BadRequestException("scoreType is required");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("reason is required");
        }
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException("entries must not be empty");
        }
        if (request.getEntries().size() > MAX_BULK_ENTRIES) {
            throw new BadRequestException("entries must not exceed " + MAX_BULK_ENTRIES);
        }

        semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        if (request.getActivityId() != null) {
            activityRepository.findByIdAndIsDeletedFalse(request.getActivityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        }

        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        List<BulkManualScoreResponse.BulkManualScoreItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (BulkManualScoreRequest.BulkManualScoreEntryRequest entry : request.getEntries()) {
            try {
                String reason = entry.getReason() != null && !entry.getReason().isBlank()
                        ? entry.getReason().trim()
                        : request.getReason().trim();
                ManualScoreResponse created = requiresNew.execute(status -> createOne(
                        entry.getStudentId(),
                        request.getSemesterId(),
                        request.getScoreType(),
                        entry.getPoints(),
                        reason,
                        request.getActivityId(),
                        actor,
                        scope));
                results.add(BulkManualScoreResponse.BulkManualScoreItemResult.builder()
                        .studentId(entry.getStudentId())
                        .success(true)
                        .data(created)
                        .build());
                successCount++;
            } catch (BadRequestException | ResourceNotFoundException | ForbiddenException ex) {
                results.add(BulkManualScoreResponse.BulkManualScoreItemResult.builder()
                        .studentId(entry.getStudentId())
                        .success(false)
                        .error(ex.getMessage())
                        .build());
                failureCount++;
            } catch (RuntimeException ex) {
                results.add(BulkManualScoreResponse.BulkManualScoreItemResult.builder()
                        .studentId(entry.getStudentId())
                        .success(false)
                        .error(ex.getMessage() != null ? ex.getMessage() : "Unexpected error")
                        .build());
                failureCount++;
            }
        }

        BulkManualScoreResponse body = BulkManualScoreResponse.builder()
                .semesterId(request.getSemesterId())
                .scoreType(request.getScoreType())
                .total(request.getEntries().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();

        writeAudit(actor, "SCORE_MANUAL_BULK_CREATE", "ManualScoreAdjustment", request.getSemesterId(),
                "semesterId=" + request.getSemesterId()
                        + ",scoreType=" + request.getScoreType()
                        + ",total=" + body.getTotal()
                        + ",success=" + successCount
                        + ",failure=" + failureCount);

        return Response.success("Bulk manual score adjustments processed", body);
    }

    @Override
    @Transactional
    public Response reverseManualAdjustment(Long adjustmentId, String reason, User actor, DepartmentScope scope) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("reason is required");
        }

        ManualScoreAdjustment adjustment = manualScoreAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Manual score adjustment not found"));

        if (scope != null && (scope.manager() || scope.admin())) {
            departmentAuthorizationService.requireStudentAccess(adjustment.getStudent().getId(), scope);
        }

        var activeEntries = scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(
                ScoreEntrySourceType.MANUAL_ADJUSTMENT, adjustmentId,
                vn.campuslife.enumeration.ScoreEntryStatus.ACTIVE);
        if (activeEntries.isEmpty()) {
            throw new BadRequestException("No active score entry found for this adjustment");
        }

        scoreEntryService.reverseEntries(
                ScoreEntrySourceType.MANUAL_ADJUSTMENT,
                adjustmentId,
                reason.trim(),
                actor);

        writeAudit(actor, "SCORE_MANUAL_REVERSE", "ManualScoreAdjustment", adjustmentId,
                "studentId=" + adjustment.getStudent().getId()
                        + ",reason=" + reason.trim());

        notifyScoreUpdate(adjustment.getStudent(), "Điểm điều chỉnh thủ công đã được hủy",
                "Một điều chỉnh điểm thủ công (" + adjustment.getPoints() + " điểm, "
                        + adjustment.getScoreType() + ") đã được hủy. Lý do: " + reason.trim(),
                Map.of("adjustmentId", adjustmentId));

        return Response.success("Manual score adjustment reversed", Map.of(
                "adjustmentId", adjustmentId,
                "reversedEntries", activeEntries.size()));
    }

    private ManualScoreResponse createOne(
            Long studentId,
            Long semesterId,
            vn.campuslife.enumeration.ScoreType scoreType,
            java.math.BigDecimal points,
            String reason,
            Long activityId,
            User actor,
            DepartmentScope scope) {
        if (studentId == null) {
            throw new BadRequestException("studentId is required");
        }
        if (points == null) {
            throw new BadRequestException("points is required");
        }
        if (scoreType == null) {
            throw new BadRequestException("scoreType is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("reason is required");
        }

        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        if (scope != null && (scope.manager() || scope.admin())) {
            departmentAuthorizationService.requireStudentAccess(student.getId(), scope);
        }

        Activity activity = null;
        if (activityId != null) {
            activity = activityRepository.findByIdAndIsDeletedFalse(activityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        }

        ManualScoreAdjustment adjustment = new ManualScoreAdjustment();
        adjustment.setStudent(student);
        adjustment.setSemester(semester);
        adjustment.setScoreType(scoreType);
        adjustment.setPoints(points);
        adjustment.setReason(reason);
        adjustment.setActivity(activity);
        adjustment.setCreatedBy(actor);
        adjustment = manualScoreAdjustmentRepository.save(adjustment);

        ScoreEntry entry = scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                .studentId(student.getId())
                .semesterId(semester.getId())
                .scoreType(scoreType)
                .activityId(activityId)
                .sourceType(ScoreEntrySourceType.MANUAL_ADJUSTMENT)
                .sourceId(adjustment.getId())
                .points(points)
                .reason(reason)
                .actor(actor)
                .build());

        writeAudit(actor, "SCORE_MANUAL_CREATE", "ManualScoreAdjustment", adjustment.getId(),
                "studentId=" + student.getId()
                        + ",semesterId=" + semester.getId()
                        + ",scoreType=" + scoreType
                        + ",points=" + points
                        + ",scoreEntryId=" + entry.getId());

        notifyScoreUpdate(student, "Điểm được điều chỉnh thủ công",
                "Điểm " + scoreType + " của bạn đã được cập nhật: "
                        + points + " điểm. Lý do: " + reason,
                Map.of("adjustmentId", adjustment.getId(), "scoreEntryId", entry.getId()));

        return toResponse(adjustment, entry.getId());
    }

    private ManualScoreResponse toResponse(ManualScoreAdjustment adjustment, Long scoreEntryId) {
        return ManualScoreResponse.builder()
                .adjustmentId(adjustment.getId())
                .scoreEntryId(scoreEntryId)
                .studentId(adjustment.getStudent().getId())
                .semesterId(adjustment.getSemester().getId())
                .scoreType(adjustment.getScoreType())
                .points(adjustment.getPoints())
                .reason(adjustment.getReason())
                .activityId(adjustment.getActivity() != null ? adjustment.getActivity().getId() : null)
                .createdByUserId(adjustment.getCreatedBy() != null ? adjustment.getCreatedBy().getId() : null)
                .createdAt(adjustment.getCreatedAt())
                .build();
    }

    private void notifyScoreUpdate(Student student, String title, String content, Map<String, Object> metadata) {
        if (student.getUser() == null) {
            return;
        }
        notificationService.sendNotification(
                student.getUser().getId(),
                title,
                content,
                NotificationType.SCORE_UPDATE,
                null,
                metadata);
    }

    private void writeAudit(User actor, String action, String entityType, Long entityId, String detail) {
        if (actor == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }
}
