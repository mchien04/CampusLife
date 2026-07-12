package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.Role;
import vn.campuslife.enumeration.ScoreAppealStatus;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ForbiddenException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.*;
import vn.campuslife.config.UploadProperties;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreEntryService;
import vn.campuslife.service.UploadStorageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreAppealServiceImplTest {

    @Mock private ScoreAppealRepository scoreAppealRepository;
    @Mock private ScoreAppealMessageRepository scoreAppealMessageRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private ScoreEntryRepository scoreEntryRepository;
    @Mock private StudentScoreRepository studentScoreRepository;
    @Mock private ManualScoreAdjustmentRepository manualScoreAdjustmentRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ScoreEntryService scoreEntryService;
    @Mock private NotificationService notificationService;
    @Mock private DepartmentAuthorizationService departmentAuthorizationService;
    @Mock private UploadStorageService uploadStorageService;
    @Mock private UploadProperties uploadProperties;

    @InjectMocks
    private ScoreAppealServiceImpl scoreAppealService;

    private User studentUser;
    private User managerUser;
    private Student student;
    private Semester semester;
    private ScoreAppeal appeal;
    private ScoreEntry deductionEntry;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(20L);
        studentUser.setUsername("sv001");
        studentUser.setRole(Role.STUDENT);

        managerUser = new User();
        managerUser.setId(1L);
        managerUser.setUsername("manager1");
        managerUser.setRole(Role.MANAGER);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);
        student.setStudentCode("SV001");
        student.setFullName("Nguyen Van A");

        semester = new Semester();
        semester.setId(200L);

        deductionEntry = new ScoreEntry();
        deductionEntry.setId(501L);
        deductionEntry.setStudent(student);
        deductionEntry.setSemester(semester);
        deductionEntry.setScoreType(ScoreType.REN_LUYEN);
        deductionEntry.setPoints(BigDecimal.valueOf(-2));
        deductionEntry.setStatus(vn.campuslife.enumeration.ScoreEntryStatus.ACTIVE);

        appeal = new ScoreAppeal();
        appeal.setId(50L);
        appeal.setStudent(student);
        appeal.setSemester(semester);
        appeal.setScoreType(ScoreType.REN_LUYEN);
        appeal.setRelatedScoreEntry(deductionEntry);
        appeal.setTitle("Sai điểm check-in");
        appeal.setReason("Tôi đã điểm danh");
        appeal.setStatus(ScoreAppealStatus.PENDING);
    }

    @Test
    void createAppeal_success() {
        CreateScoreAppealRequest request = new CreateScoreAppealRequest();
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.REN_LUYEN);
        request.setRelatedScoreEntryId(501L);
        request.setTitle("Sai điểm check-in");
        request.setReason("Tôi đã điểm danh");
        request.setRequestedPoints(BigDecimal.TEN);

        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));
        when(scoreAppealRepository.save(any(ScoreAppeal.class))).thenAnswer(inv -> {
            ScoreAppeal a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });

        Response resp = scoreAppealService.createAppeal(request, studentUser);

        assertTrue(resp.isStatus());
        ScoreAppealResponse body = (ScoreAppealResponse) resp.getBody();
        assertEquals(50L, body.getId());
        assertEquals(ScoreAppealStatus.PENDING, body.getStatus());
        assertEquals(501L, body.getRelatedScoreEntryId());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createAppeal_missingRelatedEntry_throws() {
        CreateScoreAppealRequest request = new CreateScoreAppealRequest();
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.REN_LUYEN);
        request.setTitle("Appeal");
        request.setReason("Reason");

        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));

        assertThrows(BadRequestException.class,
                () -> scoreAppealService.createAppeal(request, studentUser));
    }

    @Test
    void createAppeal_positiveRelatedEntry_throws() {
        CreateScoreAppealRequest request = new CreateScoreAppealRequest();
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.REN_LUYEN);
        request.setRelatedScoreEntryId(501L);
        request.setTitle("Appeal");
        request.setReason("Reason");

        deductionEntry.setPoints(BigDecimal.ONE);
        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));

        assertThrows(BadRequestException.class,
                () -> scoreAppealService.createAppeal(request, studentUser));
    }

    @Test
    void createAppeal_relatedEntryOtherStudent_forbidden() {
        CreateScoreAppealRequest request = new CreateScoreAppealRequest();
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.REN_LUYEN);
        request.setTitle("Appeal");
        request.setReason("Reason");
        request.setRelatedScoreEntryId(999L);

        Student other = new Student();
        other.setId(99L);
        ScoreEntry entry = new ScoreEntry();
        entry.setId(999L);
        entry.setStudent(other);
        entry.setPoints(BigDecimal.valueOf(-1));
        entry.setStatus(vn.campuslife.enumeration.ScoreEntryStatus.ACTIVE);

        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(scoreEntryRepository.findById(999L)).thenReturn(Optional.of(entry));

        assertThrows(ForbiddenException.class,
                () -> scoreAppealService.createAppeal(request, studentUser));
    }

    @Test
    void addMessage_staffMovesPendingToInReview() {
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(scoreAppealMessageRepository.save(any(ScoreAppealMessage.class))).thenAnswer(inv -> {
            ScoreAppealMessage m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        ScoreAppealMessageRequest msg = new ScoreAppealMessageRequest("Đang xem xét");
        DepartmentScope scope = DepartmentScope.adminScope();

        Response resp = scoreAppealService.addMessage(50L, msg, managerUser, scope);

        assertTrue(resp.isStatus());
        assertEquals(ScoreAppealStatus.IN_REVIEW, appeal.getStatus());
        verify(scoreAppealRepository).save(appeal);
    }

    @Test
    void decide_approveWithAdjustedPoints_reversesThenCreatesLedger() {
        appeal.setStatus(ScoreAppealStatus.IN_REVIEW);
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(manualScoreAdjustmentRepository.save(any(ManualScoreAdjustment.class))).thenAnswer(inv -> {
            ManualScoreAdjustment a = inv.getArgument(0);
            a.setId(77L);
            return a;
        });

        ScoreEntry entry = new ScoreEntry();
        entry.setId(600L);
        when(scoreEntryService.upsertEntry(any(ScoreEntryCommand.class))).thenReturn(entry);
        when(scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        ScoreAppealDecisionRequest decision = new ScoreAppealDecisionRequest();
        decision.setDecision(ScoreAppealStatus.APPROVED);
        decision.setDecisionNotes("Bổ sung 5 điểm");
        decision.setAdjustedPoints(BigDecimal.valueOf(5));

        Response resp = scoreAppealService.decide(50L, decision, managerUser, DepartmentScope.adminScope());

        assertTrue(resp.isStatus());
        assertEquals(ScoreAppealStatus.APPROVED, appeal.getStatus());
        assertEquals(600L, appeal.getResultingScoreEntry().getId());

        verify(scoreEntryService).reverseEntry(eq(501L), contains("Appeal #50"), eq(managerUser));

        ArgumentCaptor<ScoreEntryCommand> cmdCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(cmdCaptor.capture());
        assertEquals(ScoreEntrySourceType.MANUAL_ADJUSTMENT, cmdCaptor.getValue().getSourceType());
        assertEquals(77L, cmdCaptor.getValue().getSourceId());
        verify(notificationService, atLeastOnce())
                .sendNotification(eq(20L), anyString(), anyString(), any(), isNull(), anyMap());
    }

    @Test
    void decide_approveWithoutAdjustedPoints_onlyReverses() {
        appeal.setStatus(ScoreAppealStatus.IN_REVIEW);
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));
        when(scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        ScoreAppealDecisionRequest decision = new ScoreAppealDecisionRequest();
        decision.setDecision(ScoreAppealStatus.APPROVED);

        Response resp = scoreAppealService.decide(50L, decision, managerUser, DepartmentScope.adminScope());

        assertTrue(resp.isStatus());
        assertEquals(ScoreAppealStatus.APPROVED, appeal.getStatus());
        assertNull(appeal.getResultingScoreEntry());
        verify(scoreEntryService).reverseEntry(eq(501L), contains("Appeal #50"), eq(managerUser));
        verify(scoreEntryService, never()).upsertEntry(any());
        verify(manualScoreAdjustmentRepository, never()).save(any());
    }

    @Test
    void previewDecision_showsProjectedScoreWithoutWriting() {
        appeal.setStatus(ScoreAppealStatus.IN_REVIEW);
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));

        StudentScore current = new StudentScore();
        current.setScore(BigDecimal.valueOf(80));
        when(studentScoreRepository.findByStudentIdAndSemesterIdAndScoreType(
                10L, 200L, ScoreType.REN_LUYEN)).thenReturn(Optional.of(current));

        ScoreAppealDecisionRequest decision = new ScoreAppealDecisionRequest();
        decision.setDecision(ScoreAppealStatus.APPROVED);
        decision.setAdjustedPoints(BigDecimal.valueOf(5));

        Response resp = scoreAppealService.previewDecision(50L, decision, managerUser, DepartmentScope.adminScope());

        assertTrue(resp.isStatus());
        ScoreAppealDecisionPreviewResponse preview = (ScoreAppealDecisionPreviewResponse) resp.getBody();
        // reverse -2 from 80 → 82, then +5 → 87
        assertEquals(BigDecimal.valueOf(80), preview.getCurrentScore());
        assertEquals(BigDecimal.valueOf(5), preview.getAdjustedPoints());
        assertEquals(BigDecimal.valueOf(87), preview.getProjectedScore());
        assertEquals(BigDecimal.valueOf(82), preview.getProjectedRelatedScore());
        assertTrue(preview.isWillCreateLedgerEntry());
        assertTrue(preview.isWillReverseRelated());
        verify(scoreEntryService, never()).upsertEntry(any());
        verify(scoreEntryService, never()).reverseEntry(anyLong(), anyString(), any());
        verify(manualScoreAdjustmentRepository, never()).save(any());
    }

    @Test
    void createAppeal_storesEvidenceUrls() {
        CreateScoreAppealRequest request = new CreateScoreAppealRequest();
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.REN_LUYEN);
        request.setRelatedScoreEntryId(501L);
        request.setTitle("Sai điểm");
        request.setReason("Có ảnh minh chứng");
        request.setEvidenceUrls(List.of("http://localhost:8080/uploads/score-appeals/10/a.jpg"));

        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(scoreEntryRepository.findById(501L)).thenReturn(Optional.of(deductionEntry));
        when(uploadStorageService.extractRelativePath(anyString())).thenReturn("score-appeals/10/a.jpg");
        when(uploadStorageService.toPublicUrl("score-appeals/10/a.jpg"))
                .thenReturn("http://localhost:8080/uploads/score-appeals/10/a.jpg");
        when(scoreAppealRepository.save(any(ScoreAppeal.class))).thenAnswer(inv -> {
            ScoreAppeal a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });

        Response resp = scoreAppealService.createAppeal(request, studentUser);
        assertTrue(resp.isStatus());
        ScoreAppealResponse body = (ScoreAppealResponse) resp.getBody();
        assertEquals(1, body.getEvidenceUrls().size());
        assertEquals("http://localhost:8080/uploads/score-appeals/10/a.jpg", body.getEvidenceUrls().get(0));
    }

    @Test
    void decide_reject_noLedger() {
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(scoreAppealMessageRepository.findByAppealIdOrderByCreatedAtAsc(50L)).thenReturn(List.of());

        ScoreAppealDecisionRequest decision = new ScoreAppealDecisionRequest();
        decision.setDecision(ScoreAppealStatus.REJECTED);
        decision.setDecisionNotes("Không đủ căn cứ");

        Response resp = scoreAppealService.decide(50L, decision, managerUser, DepartmentScope.adminScope());

        assertTrue(resp.isStatus());
        assertEquals(ScoreAppealStatus.REJECTED, appeal.getStatus());
        verify(scoreEntryService, never()).upsertEntry(any());
        verify(manualScoreAdjustmentRepository, never()).save(any());
    }

    @Test
    void decide_invalidStatus_throws() {
        appeal.setStatus(ScoreAppealStatus.CLOSED);
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));

        ScoreAppealDecisionRequest decision = new ScoreAppealDecisionRequest();
        decision.setDecision(ScoreAppealStatus.APPROVED);

        assertThrows(BadRequestException.class,
                () -> scoreAppealService.decide(50L, decision, managerUser, DepartmentScope.adminScope()));
    }

    @Test
    void withdraw_pending_closes() {
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));

        Response resp = scoreAppealService.withdraw(50L, studentUser);

        assertTrue(resp.isStatus());
        assertEquals(ScoreAppealStatus.CLOSED, appeal.getStatus());
    }

    @Test
    void withdraw_inReview_throws() {
        appeal.setStatus(ScoreAppealStatus.IN_REVIEW);
        when(scoreAppealRepository.findById(50L)).thenReturn(Optional.of(appeal));
        when(studentRepository.findByUserIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(student));

        assertThrows(BadRequestException.class,
                () -> scoreAppealService.withdraw(50L, studentUser));
    }
}
