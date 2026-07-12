package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.NotificationType;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.*;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreAppealService;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.UploadStorageService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreAppealServiceImpl implements ScoreAppealService {

    private static final int MAX_EVIDENCE_FILES = 5;
    private static final long MAX_EVIDENCE_BYTES = 5L * 1024 * 1024;

    private static final Set<ScoreAppealStatus> OPEN_FOR_MESSAGE =
            EnumSet.of(ScoreAppealStatus.PENDING, ScoreAppealStatus.IN_REVIEW);
    private static final Set<ScoreAppealStatus> DECIDABLE =
            EnumSet.of(ScoreAppealStatus.PENDING, ScoreAppealStatus.IN_REVIEW);
    private static final Set<ScoreAppealStatus> CLOSABLE =
            EnumSet.of(ScoreAppealStatus.PENDING, ScoreAppealStatus.IN_REVIEW,
                    ScoreAppealStatus.APPROVED, ScoreAppealStatus.REJECTED);

    private final ScoreAppealRepository scoreAppealRepository;
    private final ScoreAppealMessageRepository scoreAppealMessageRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final ManualScoreAdjustmentRepository manualScoreAdjustmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final ScoreEntryService scoreEntryService;
    private final NotificationService notificationService;
    private final DepartmentAuthorizationService departmentAuthorizationService;
    private final UploadStorageService uploadStorageService;
    private final UploadProperties uploadProperties;

    @Override
    @Transactional
    public Response uploadEvidence(List<MultipartFile> files, User studentUser) {
        Student student = studentRepository.findByUserIdAndIsDeletedFalse(studentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Only students can upload appeal evidence"));

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one image file is required");
        }
        if (files.size() > MAX_EVIDENCE_FILES) {
            throw new BadRequestException("Maximum " + MAX_EVIDENCE_FILES + " evidence images allowed");
        }

        String directory = uploadProperties.getPaths().getScoreAppeals() + "/" + student.getId();
        List<String> publicUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("Empty file is not allowed");
            }
            if (file.getSize() > MAX_EVIDENCE_BYTES) {
                throw new BadRequestException("Each evidence image must be <= 5MB");
            }
            try {
                String relativePath = uploadStorageService.store(file, directory, true);
                publicUrls.add(uploadStorageService.toPublicUrl(relativePath));
            } catch (IOException | IllegalArgumentException e) {
                throw new BadRequestException("Failed to upload evidence: " + e.getMessage());
            }
        }

        return Response.success("Evidence uploaded", Map.of("urls", publicUrls));
    }

    @Override
    @Transactional
    public Response createAppeal(CreateScoreAppealRequest request, User studentUser) {
        Student student = studentRepository.findByUserIdAndIsDeletedFalse(studentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Only students can create score appeals"));

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        ScoreEntry related = requireDeductionEntry(request.getRelatedScoreEntryId(), student);

        ScoreAppeal appeal = new ScoreAppeal();
        appeal.setStudent(student);
        appeal.setSemester(semester);
        appeal.setScoreType(request.getScoreType());
        appeal.setRelatedScoreEntry(related);
        appeal.setTitle(request.getTitle().trim());
        appeal.setReason(request.getReason().trim());
        appeal.setRequestedPoints(request.getRequestedPoints());
        appeal.setEvidenceUrls(toStoredEvidenceCsv(request.getEvidenceUrls()));
        appeal.setStatus(ScoreAppealStatus.PENDING);
        appeal = scoreAppealRepository.save(appeal);

        writeAudit(studentUser, "SCORE_APPEAL_CREATE", "ScoreAppeal", appeal.getId(),
                "semesterId=" + semester.getId() + ",scoreType=" + request.getScoreType()
                        + ",relatedScoreEntryId=" + related.getId()
                        + ",evidenceCount=" + toPublicEvidenceUrls(appeal.getEvidenceUrls()).size());

        return Response.success("Score appeal created", toResponse(appeal, List.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public Response listMyAppeals(User studentUser) {
        Student student = studentRepository.findByUserIdAndIsDeletedFalse(studentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Only students can list their appeals"));
        List<ScoreAppealResponse> items = scoreAppealRepository
                .findByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(a -> toResponse(a, List.of()))
                .collect(Collectors.toList());
        return Response.success("My score appeals", items);
    }

    @Override
    @Transactional(readOnly = true)
    public Response listAppeals(ScoreAppealStatus status, Long semesterId, Long studentId,
                                int page, int size, DepartmentScope scope) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<ScoreAppeal> result;
        if (scope != null && scope.manager() && !scope.admin()) {
            if (scope.departmentIds().isEmpty()) {
                throw new ForbiddenException("Manager has no assigned departments");
            }
            if (studentId != null) {
                departmentAuthorizationService.requireStudentAccess(studentId, scope);
            }
            result = scoreAppealRepository.findFilteredScoped(
                    status, semesterId, studentId, scope.departmentIds(), pageable);
        } else if (scope != null && scope.student() && !scope.admin() && !scope.manager()) {
            throw new ForbiddenException("Only admin/manager can list all appeals");
        } else {
            // Admin or unscoped staff (SecurityConfig already restricts this endpoint)
            result = scoreAppealRepository.findFiltered(status, semesterId, studentId, pageable);
        }

        Map<String, Object> body = Map.of(
                "content", result.getContent().stream()
                        .map(a -> toResponse(a, List.of()))
                        .collect(Collectors.toList()),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages());
        return Response.success("Score appeals", body);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getAppeal(Long appealId, User actor, DepartmentScope scope) {
        ScoreAppeal appeal = findAppeal(appealId);
        assertCanView(appeal, actor, scope);
        List<ScoreAppealMessage> messages =
                scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(appealId);
        return Response.success("Score appeal detail", toResponse(appeal, messages));
    }

    @Override
    @Transactional
    public Response addMessage(Long appealId, ScoreAppealMessageRequest request, User actor, DepartmentScope scope) {
        ScoreAppeal appeal = findAppeal(appealId);
        assertCanMessage(appeal, actor, scope);

        if (!OPEN_FOR_MESSAGE.contains(appeal.getStatus())) {
            throw new BadRequestException("Cannot add messages when appeal status is " + appeal.getStatus());
        }

        ScoreAppealMessage message = new ScoreAppealMessage();
        message.setAppeal(appeal);
        message.setSender(actor);
        message.setContent(request.getContent().trim());
        message = scoreAppealMessageRepository.save(message);

        boolean staff = actor.getRole() == Role.ADMIN || actor.getRole() == Role.MANAGER;
        if (staff && appeal.getStatus() == ScoreAppealStatus.PENDING) {
            appeal.setStatus(ScoreAppealStatus.IN_REVIEW);
            scoreAppealRepository.save(appeal);
        }

        writeAudit(actor, "SCORE_APPEAL_MESSAGE", "ScoreAppeal", appealId,
                "messageId=" + message.getId());

        List<ScoreAppealMessage> messages =
                scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(appealId);
        return Response.success("Message added", toResponse(appeal, messages));
    }

    @Override
    @Transactional(readOnly = true)
    public Response previewDecision(Long appealId, ScoreAppealDecisionRequest request, User actor, DepartmentScope scope) {
        ScoreAppeal appeal = findAppeal(appealId);
        assertStaffAccess(appeal, scope);

        if (!DECIDABLE.contains(appeal.getStatus())) {
            throw new BadRequestException("Appeal cannot be previewed from status " + appeal.getStatus());
        }

        ScoreAppealStatus decision = request.getDecision();
        if (decision != ScoreAppealStatus.APPROVED && decision != ScoreAppealStatus.REJECTED) {
            throw new BadRequestException("decision must be APPROVED or REJECTED");
        }

        ScoreEntry related = appeal.getRelatedScoreEntry();
        if (decision == ScoreAppealStatus.APPROVED) {
            assertRelatedDeductionReversible(related);
        }

        ScoreType bonusScoreType = request.getScoreType() != null ? request.getScoreType() : appeal.getScoreType();
        Long bonusSemesterId = request.getSemesterId() != null
                ? request.getSemesterId()
                : appeal.getSemester().getId();

        ScoreType relatedType = related != null ? related.getScoreType() : bonusScoreType;
        Long relatedSemesterId = related != null ? related.getSemester().getId() : bonusSemesterId;

        BigDecimal relatedCurrent = studentScoreRepository
                .findByStudentIdAndSemesterIdAndScoreType(
                        appeal.getStudent().getId(), relatedSemesterId, relatedType)
                .map(StudentScore::getScore)
                .orElse(BigDecimal.ZERO);

        BigDecimal adjustedPoints = request.getAdjustedPoints();
        boolean willReverse = decision == ScoreAppealStatus.APPROVED;
        boolean willCreateBonus = decision == ScoreAppealStatus.APPROVED && adjustedPoints != null;

        // Reverse removes related.points from ACTIVE sum → projected = current - related.points
        BigDecimal projectedAfterReverse = willReverse && related != null
                ? relatedCurrent.subtract(related.getPoints() != null ? related.getPoints() : BigDecimal.ZERO)
                : relatedCurrent;

        BigDecimal bonusCurrent = relatedType.equals(bonusScoreType) && relatedSemesterId.equals(bonusSemesterId)
                ? projectedAfterReverse
                : studentScoreRepository
                        .findByStudentIdAndSemesterIdAndScoreType(
                                appeal.getStudent().getId(), bonusSemesterId, bonusScoreType)
                        .map(StudentScore::getScore)
                        .orElse(BigDecimal.ZERO);

        BigDecimal projectedBonus = willCreateBonus
                ? bonusCurrent.add(adjustedPoints)
                : bonusCurrent;

        // Primary projectedScore: bonus type if creating bonus, else related type after reverse
        BigDecimal projectedScore = willCreateBonus ? projectedBonus : projectedAfterReverse;
        BigDecimal currentScore = willCreateBonus && !(relatedType.equals(bonusScoreType) && relatedSemesterId.equals(bonusSemesterId))
                ? bonusCurrent
                : relatedCurrent;

        String note;
        if (decision == ScoreAppealStatus.REJECTED) {
            note = "Từ chối khiếu nại — điểm không thay đổi.";
        } else if (willCreateBonus) {
            note = "Chấp nhận: gỡ điểm trừ trên entry #" + related.getId()
                    + " (" + relatedType + "), rồi cộng " + adjustedPoints + " vào " + bonusScoreType + ".";
        } else {
            note = "Chấp nhận: chỉ gỡ điểm trừ trên entry #" + related.getId()
                    + " (" + relatedType + ") — không cộng điểm thêm.";
        }

        ScoreAppealDecisionPreviewResponse preview = ScoreAppealDecisionPreviewResponse.builder()
                .appealId(appeal.getId())
                .studentId(appeal.getStudent().getId())
                .studentCode(appeal.getStudent().getStudentCode())
                .studentFullName(appeal.getStudent().getFullName())
                .semesterId(bonusSemesterId)
                .scoreType(willCreateBonus ? bonusScoreType : relatedType)
                .decision(decision)
                .currentScore(currentScore)
                .adjustedPoints(adjustedPoints)
                .projectedScore(projectedScore)
                .willCreateLedgerEntry(willCreateBonus)
                .willReverseRelated(willReverse)
                .relatedScoreEntryId(related != null ? related.getId() : null)
                .relatedEntryPoints(related != null ? related.getPoints() : null)
                .relatedScoreType(relatedType)
                .projectedRelatedScore(projectedAfterReverse)
                .note(note)
                .build();

        return Response.success("Score appeal decision preview", preview);
    }

    @Override
    @Transactional
    public Response decide(Long appealId, ScoreAppealDecisionRequest request, User actor, DepartmentScope scope) {
        ScoreAppeal appeal = findAppeal(appealId);
        assertStaffAccess(appeal, scope);

        if (!DECIDABLE.contains(appeal.getStatus())) {
            throw new BadRequestException("Appeal cannot be decided from status " + appeal.getStatus());
        }

        ScoreAppealStatus decision = request.getDecision();
        if (decision != ScoreAppealStatus.APPROVED && decision != ScoreAppealStatus.REJECTED) {
            throw new BadRequestException("decision must be APPROVED or REJECTED");
        }

        ScoreEntry resultingEntry = null;
        if (decision == ScoreAppealStatus.APPROVED) {
            ScoreEntry related = appeal.getRelatedScoreEntry();
            assertRelatedDeductionReversible(related);

            String reverseReason = "Appeal #" + appeal.getId() + " approved — reversed deduction"
                    + (request.getDecisionNotes() != null && !request.getDecisionNotes().isBlank()
                    ? ": " + request.getDecisionNotes().trim()
                    : "");
            scoreEntryService.reverseEntry(related.getId(), reverseReason, actor);
            writeAudit(actor, "SCORE_APPEAL_REVERSE", "ScoreEntry", related.getId(),
                    "fromAppealId=" + appealId);

            if (request.getAdjustedPoints() != null) {
                resultingEntry = applyAdjustment(appeal, request, actor);
                appeal.setResultingScoreEntry(resultingEntry);
            }
        }

        appeal.setStatus(decision);
        appeal.setDecisionNotes(request.getDecisionNotes());
        appeal.setDecidedAt(LocalDateTime.now());
        appeal.setDecidedBy(actor);
        scoreAppealRepository.save(appeal);

        writeAudit(actor, "SCORE_APPEAL_DECIDE", "ScoreAppeal", appealId,
                "decision=" + decision
                        + ",reversedRelated=" + (decision == ScoreAppealStatus.APPROVED)
                        + ",adjustedPoints=" + request.getAdjustedPoints()
                        + (resultingEntry != null ? ",scoreEntryId=" + resultingEntry.getId() : ""));

        String title = decision == ScoreAppealStatus.APPROVED
                ? "Khiếu nại điểm được chấp nhận"
                : "Khiếu nại điểm bị từ chối";
        String content = "Khiếu nại \"" + appeal.getTitle() + "\" đã được " + decision
                + (request.getDecisionNotes() != null ? ". Ghi chú: " + request.getDecisionNotes() : "");
        notifyStudent(appeal.getStudent(), title, content, Map.of(
                "appealId", appealId,
                "status", decision.name()));

        List<ScoreAppealMessage> messages =
                scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(appealId);
        return Response.success("Appeal decided", toResponse(appeal, messages));
    }

    @Override
    @Transactional
    public Response close(Long appealId, User actor, DepartmentScope scope) {
        ScoreAppeal appeal = findAppeal(appealId);
        assertStaffAccess(appeal, scope);

        if (!CLOSABLE.contains(appeal.getStatus())) {
            throw new BadRequestException("Appeal cannot be closed from status " + appeal.getStatus());
        }
        if (appeal.getStatus() == ScoreAppealStatus.CLOSED) {
            throw new BadRequestException("Appeal is already closed");
        }

        appeal.setStatus(ScoreAppealStatus.CLOSED);
        scoreAppealRepository.save(appeal);

        writeAudit(actor, "SCORE_APPEAL_CLOSE", "ScoreAppeal", appealId, "closedBy=" + actor.getId());

        List<ScoreAppealMessage> messages =
                scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(appealId);
        return Response.success("Appeal closed", toResponse(appeal, messages));
    }

    @Override
    @Transactional
    public Response withdraw(Long appealId, User studentUser) {
        ScoreAppeal appeal = findAppeal(appealId);
        Student student = studentRepository.findByUserIdAndIsDeletedFalse(studentUser.getId())
                .orElseThrow(() -> new ForbiddenException("Only students can withdraw appeals"));

        if (!appeal.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You can only withdraw your own appeals");
        }
        if (appeal.getStatus() != ScoreAppealStatus.PENDING) {
            throw new BadRequestException("Only PENDING appeals can be withdrawn");
        }

        appeal.setStatus(ScoreAppealStatus.CLOSED);
        scoreAppealRepository.save(appeal);

        writeAudit(studentUser, "SCORE_APPEAL_CLOSE", "ScoreAppeal", appealId, "withdrawnByStudent=true");

        return Response.success("Appeal withdrawn", toResponse(appeal, List.of()));
    }

    private ScoreEntry applyAdjustment(ScoreAppeal appeal, ScoreAppealDecisionRequest request, User actor) {
        BigDecimal points = request.getAdjustedPoints();
        ScoreType scoreType = request.getScoreType() != null ? request.getScoreType() : appeal.getScoreType();
        Long semesterId = request.getSemesterId() != null
                ? request.getSemesterId()
                : appeal.getSemester().getId();

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

        String reason = "Appeal #" + appeal.getId() + " approved"
                + (request.getDecisionNotes() != null && !request.getDecisionNotes().isBlank()
                ? ": " + request.getDecisionNotes().trim()
                : "");

        ManualScoreAdjustment adjustment = new ManualScoreAdjustment();
        adjustment.setStudent(appeal.getStudent());
        adjustment.setSemester(semester);
        adjustment.setScoreType(scoreType);
        adjustment.setPoints(points);
        adjustment.setReason(reason);
        adjustment.setCreatedBy(actor);
        adjustment = manualScoreAdjustmentRepository.save(adjustment);

        ScoreEntry entry = scoreEntryService.upsertEntry(ScoreEntryCommand.builder()
                .studentId(appeal.getStudent().getId())
                .semesterId(semester.getId())
                .scoreType(scoreType)
                .sourceType(ScoreEntrySourceType.MANUAL_ADJUSTMENT)
                .sourceId(adjustment.getId())
                .points(points)
                .reason(reason)
                .actor(actor)
                .build());

        writeAudit(actor, "SCORE_MANUAL_CREATE", "ManualScoreAdjustment", adjustment.getId(),
                "fromAppealId=" + appeal.getId() + ",scoreEntryId=" + entry.getId());

        notifyStudent(appeal.getStudent(), "Điểm được điều chỉnh sau khiếu nại",
                "Điểm " + scoreType + " đã được điều chỉnh " + points + " điểm theo khiếu nại #" + appeal.getId(),
                Map.of("appealId", appeal.getId(), "adjustmentId", adjustment.getId(),
                        "scoreEntryId", entry.getId()));

        return entry;
    }

    private ScoreAppeal findAppeal(Long appealId) {
        return scoreAppealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Score appeal not found"));
    }

    /** Entry bị trừ bắt buộc khi tạo khiếu nại: thuộc SV, ACTIVE, points &lt; 0. */
    private ScoreEntry requireDeductionEntry(Long relatedScoreEntryId, Student student) {
        if (relatedScoreEntryId == null) {
            throw new BadRequestException("relatedScoreEntryId is required (must attach the deducted score entry)");
        }
        ScoreEntry related = scoreEntryRepository.findById(relatedScoreEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Related score entry not found"));
        if (!related.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Related score entry does not belong to you");
        }
        if (related.getStatus() != ScoreEntryStatus.ACTIVE) {
            throw new BadRequestException("Related score entry must be ACTIVE");
        }
        if (related.getPoints() == null || related.getPoints().compareTo(BigDecimal.ZERO) >= 0) {
            throw new BadRequestException("Related score entry must be a deduction (points < 0)");
        }
        return related;
    }

    private void assertRelatedDeductionReversible(ScoreEntry related) {
        if (related == null) {
            throw new BadRequestException("Appeal has no related deduction entry to reverse");
        }
        // Reload to avoid stale status
        ScoreEntry fresh = scoreEntryRepository.findById(related.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Related score entry not found"));
        if (fresh.getStatus() != ScoreEntryStatus.ACTIVE) {
            throw new BadRequestException("Related score entry is not ACTIVE and cannot be reversed");
        }
        if (fresh.getPoints() == null || fresh.getPoints().compareTo(BigDecimal.ZERO) >= 0) {
            throw new BadRequestException("Related score entry must be a deduction (points < 0)");
        }
    }

    private void assertCanView(ScoreAppeal appeal, User actor, DepartmentScope scope) {
        if (actor.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserIdAndIsDeletedFalse(actor.getId())
                    .orElseThrow(() -> new ForbiddenException("Access denied"));
            if (!appeal.getStudent().getId().equals(student.getId())) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }
        assertStaffAccess(appeal, scope);
    }

    private void assertCanMessage(ScoreAppeal appeal, User actor, DepartmentScope scope) {
        if (actor.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserIdAndIsDeletedFalse(actor.getId())
                    .orElseThrow(() -> new ForbiddenException("Access denied"));
            if (!appeal.getStudent().getId().equals(student.getId())) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }
        assertStaffAccess(appeal, scope);
    }

    private void assertStaffAccess(ScoreAppeal appeal, DepartmentScope scope) {
        if (scope != null && (scope.manager() || scope.admin())) {
            departmentAuthorizationService.requireStudentAccess(appeal.getStudent().getId(), scope);
        }
    }

    private ScoreAppealResponse toResponse(ScoreAppeal appeal, List<ScoreAppealMessage> messages) {
        Student student = appeal.getStudent();
        return ScoreAppealResponse.builder()
                .id(appeal.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentFullName(student.getFullName())
                .semesterId(appeal.getSemester().getId())
                .scoreType(appeal.getScoreType())
                .relatedScoreEntryId(appeal.getRelatedScoreEntry() != null
                        ? appeal.getRelatedScoreEntry().getId() : null)
                .title(appeal.getTitle())
                .reason(appeal.getReason())
                .evidenceUrls(toPublicEvidenceUrls(appeal.getEvidenceUrls()))
                .requestedPoints(appeal.getRequestedPoints())
                .status(appeal.getStatus())
                .decisionNotes(appeal.getDecisionNotes())
                .decidedAt(appeal.getDecidedAt())
                .decidedById(appeal.getDecidedBy() != null ? appeal.getDecidedBy().getId() : null)
                .decidedByUsername(appeal.getDecidedBy() != null ? appeal.getDecidedBy().getUsername() : null)
                .resultingScoreEntryId(appeal.getResultingScoreEntry() != null
                        ? appeal.getResultingScoreEntry().getId() : null)
                .createdAt(appeal.getCreatedAt())
                .updatedAt(appeal.getUpdatedAt())
                .messages(messages.stream().map(this::toMessageResponse).collect(Collectors.toList()))
                .build();
    }

    private String toStoredEvidenceCsv(List<String> publicOrRelativeUrls) {
        if (publicOrRelativeUrls == null || publicOrRelativeUrls.isEmpty()) {
            return null;
        }
        if (publicOrRelativeUrls.size() > MAX_EVIDENCE_FILES) {
            throw new BadRequestException("Maximum " + MAX_EVIDENCE_FILES + " evidence images allowed");
        }
        return publicOrRelativeUrls.stream()
                .filter(u -> u != null && !u.isBlank())
                .map(u -> uploadStorageService.extractRelativePath(u.trim()))
                .collect(Collectors.joining(","));
    }

    private List<String> toPublicEvidenceUrls(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(uploadStorageService::toPublicUrl)
                .collect(Collectors.toList());
    }

    private ScoreAppealMessageResponse toMessageResponse(ScoreAppealMessage message) {
        return ScoreAppealMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void notifyStudent(Student student, String title, String content, Map<String, Object> metadata) {
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
