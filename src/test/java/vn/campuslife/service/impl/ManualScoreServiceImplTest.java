package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.exception.BadRequestException;
import vn.campuslife.exception.ResourceNotFoundException;
import vn.campuslife.model.Response;
import vn.campuslife.model.score.BulkManualScoreRequest;
import vn.campuslife.model.score.BulkManualScoreResponse;
import vn.campuslife.model.score.ManualScoreRequest;
import vn.campuslife.model.score.ManualScoreResponse;
import vn.campuslife.model.score.ScoreEntryCommand;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreEntryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualScoreServiceImplTest {

    @Mock private ManualScoreAdjustmentRepository manualScoreAdjustmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ScoreEntryRepository scoreEntryRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ScoreEntryService scoreEntryService;
    @Mock private NotificationService notificationService;
    @Mock private DepartmentAuthorizationService departmentAuthorizationService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionStatus transactionStatus;

    @InjectMocks
    private ManualScoreServiceImpl manualScoreService;

    private User actor;
    private Student student;
    private Semester semester;
    private ManualScoreRequest request;

    @BeforeEach
    void setUp() {
        actor = new User();
        actor.setId(1L);
        actor.setUsername("manager1");

        User studentUser = new User();
        studentUser.setId(20L);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);

        semester = new Semester();
        semester.setId(200L);

        request = new ManualScoreRequest();
        request.setStudentId(10L);
        request.setSemesterId(200L);
        request.setScoreType(ScoreType.CONG_TAC_XA_HOI);
        request.setPoints(BigDecimal.valueOf(5));
        request.setReason("Hỗ trợ chuẩn bị sự kiện");

        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
    }

    @Test
    void createManualAdjustment_createsLedgerEntryAndAudit() {
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(manualScoreAdjustmentRepository.save(any(ManualScoreAdjustment.class))).thenAnswer(inv -> {
            ManualScoreAdjustment a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        ScoreEntry entry = new ScoreEntry();
        entry.setId(500L);
        when(scoreEntryService.upsertEntry(any(ScoreEntryCommand.class))).thenReturn(entry);

        DepartmentScope scope = DepartmentScope.manager(Set.of(1L));
        Response resp = manualScoreService.createManualAdjustment(request, actor, scope);

        assertTrue(resp.isStatus());
        ManualScoreResponse body = (ManualScoreResponse) resp.getBody();
        assertEquals(99L, body.getAdjustmentId());
        assertEquals(500L, body.getScoreEntryId());
        assertEquals(200L, body.getSemesterId());

        verify(departmentAuthorizationService).requireStudentAccess(10L, scope);

        ArgumentCaptor<ScoreEntryCommand> cmdCaptor = ArgumentCaptor.forClass(ScoreEntryCommand.class);
        verify(scoreEntryService).upsertEntry(cmdCaptor.capture());
        ScoreEntryCommand cmd = cmdCaptor.getValue();
        assertEquals(ScoreEntrySourceType.MANUAL_ADJUSTMENT, cmd.getSourceType());
        assertEquals(99L, cmd.getSourceId());
        assertEquals(BigDecimal.valueOf(5), cmd.getPoints());
        assertEquals(200L, cmd.getSemesterId());

        verify(auditLogRepository).save(any(AuditLog.class));
        verify(notificationService).sendNotification(eq(20L), anyString(), anyString(), any(), isNull(), anyMap());
    }

    @Test
    void createManualAdjustment_studentNotFound_throws() {
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> manualScoreService.createManualAdjustment(request, actor, DepartmentScope.adminScope()));
    }

    @Test
    void createBulk_requiresSemesterAndProcessesEntries() {
        Student student2 = new Student();
        student2.setId(11L);
        User u2 = new User();
        u2.setId(21L);
        student2.setUser(u2);

        when(semesterRepository.findById(200L)).thenReturn(Optional.of(semester));
        when(studentRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(student));
        when(studentRepository.findByIdAndIsDeletedFalse(11L)).thenReturn(Optional.of(student2));
        when(manualScoreAdjustmentRepository.save(any(ManualScoreAdjustment.class))).thenAnswer(inv -> {
            ManualScoreAdjustment a = inv.getArgument(0);
            a.setId(a.getStudent().getId() + 100);
            return a;
        });
        when(scoreEntryService.upsertEntry(any(ScoreEntryCommand.class))).thenAnswer(inv -> {
            ScoreEntry e = new ScoreEntry();
            e.setId(inv.getArgument(0, ScoreEntryCommand.class).getStudentId() + 500);
            return e;
        });

        BulkManualScoreRequest bulk = new BulkManualScoreRequest();
        bulk.setSemesterId(200L);
        bulk.setScoreType(ScoreType.CONG_TAC_XA_HOI);
        bulk.setReason("Hỗ trợ sự kiện");
        bulk.setEntries(List.of(
                new BulkManualScoreRequest.BulkManualScoreEntryRequest(10L, BigDecimal.valueOf(5), null),
                new BulkManualScoreRequest.BulkManualScoreEntryRequest(11L, BigDecimal.valueOf(3), null)
        ));

        Response resp = manualScoreService.createBulkManualAdjustments(bulk, actor, DepartmentScope.adminScope());
        assertTrue(resp.isStatus());
        BulkManualScoreResponse body = (BulkManualScoreResponse) resp.getBody();
        assertEquals(200L, body.getSemesterId());
        assertEquals(2, body.getTotal());
        assertEquals(2, body.getSuccessCount());
        assertEquals(0, body.getFailureCount());
    }

    @Test
    void reverseManualAdjustment_reversesActiveEntries() {
        ManualScoreAdjustment adjustment = new ManualScoreAdjustment();
        adjustment.setId(99L);
        adjustment.setStudent(student);
        adjustment.setSemester(semester);
        adjustment.setPoints(BigDecimal.valueOf(5));
        adjustment.setScoreType(ScoreType.CONG_TAC_XA_HOI);

        ScoreEntry active = new ScoreEntry();
        active.setId(500L);

        when(manualScoreAdjustmentRepository.findById(99L)).thenReturn(Optional.of(adjustment));
        when(scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(
                ScoreEntrySourceType.MANUAL_ADJUSTMENT, 99L, ScoreEntryStatus.ACTIVE))
                .thenReturn(List.of(active));

        DepartmentScope scope = DepartmentScope.adminScope();
        Response resp = manualScoreService.reverseManualAdjustment(99L, "Sai điểm", actor, scope);

        assertTrue(resp.isStatus());
        verify(scoreEntryService).reverseEntries(
                ScoreEntrySourceType.MANUAL_ADJUSTMENT, 99L, "Sai điểm", actor);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void reverseManualAdjustment_noActiveEntry_throws() {
        ManualScoreAdjustment adjustment = new ManualScoreAdjustment();
        adjustment.setId(99L);
        adjustment.setStudent(student);

        when(manualScoreAdjustmentRepository.findById(99L)).thenReturn(Optional.of(adjustment));
        when(scoreEntryRepository.findBySourceTypeAndSourceIdAndStatus(
                ScoreEntrySourceType.MANUAL_ADJUSTMENT, 99L, ScoreEntryStatus.ACTIVE))
                .thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> manualScoreService.reverseManualAdjustment(99L, "reason", actor, DepartmentScope.adminScope()));
    }
}
