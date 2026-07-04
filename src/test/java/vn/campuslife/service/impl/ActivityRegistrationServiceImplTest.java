package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.ActivityParticipationRequest;
import vn.campuslife.repository.*;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.config.UploadProperties;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityRegistrationServiceImplTest {

    @Mock
    private ActivityRegistrationRepository registrationRepository;

    @Mock
    private ActivityParticipationRepository participationRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReminderScheduleService reminderScheduleService;

    @Mock
    private ActivitySeriesService activitySeriesService;

    @Mock
    private ActivitySeriesRepository activitySeriesRepository;

    @Mock
    private SemesterHelperService semesterHelperService;

    @Mock
    private ScoreRuleEngine scoreRuleEngine;

    @Mock
    private TaskSubmissionRepository taskSubmissionRepository;

    @Mock
    private UploadProperties uploadProperties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PreparationTaskMemberRepository preparationTaskMemberRepository;

    @Mock
    private ActivityOrganizerRepository activityOrganizerRepository;

    @InjectMocks
    private ActivityRegistrationServiceImpl activityRegistrationService;

    private Student student;
    private User studentUser;
    private Activity activity;
    private ActivityRegistration registration;
    private ActivityParticipation participation;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(33L);
        studentUser.setRole(Role.MANAGER);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);

        activity = new Activity();
        activity.setId(100L);
        activity.setName("Reg Activity");
        activity.setDraft(false);

        registration = new ActivityRegistration();
        registration.setId(50L);
        registration.setActivity(activity);
        registration.setStudent(student);
        registration.setStatus(RegistrationStatus.APPROVED);

        participation = new ActivityParticipation();
        participation.setId(600L);
        participation.setRegistration(registration);
        participation.setParticipationType(ParticipationType.ATTENDED);
    }

    @Test
    void gradeCompletion_NoSubmissionRequired_GradedCompleted_Successful() {
        activity.setRequiresSubmission(false);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed successfully");

        assertTrue(response.isStatus());
        assertEquals("Đã chấm điểm completion", response.getMessage());
        assertEquals(true, participation.getIsCompleted());
        assertEquals(ParticipationType.COMPLETED, participation.getParticipationType());

        verify(participationRepository).save(participation);
        verify(scoreRuleEngine).applyActivityCompleted(participation, studentUser);
        verifyNoInteractions(activitySeriesService);
    }

    @Test
    void gradeCompletion_SubmissionRequired_NoGradedSubmission_ReturnsError() {
        activity.setRequiresSubmission(true);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.existsByActivityAndStudentAndStatus(100L, 10L, SubmissionStatus.GRADED))
                .thenReturn(false); // No graded submission

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertFalse(response.isStatus());
        assertEquals("Sinh viên chưa nộp bài hoặc chưa được chấm điểm", response.getMessage());
        verify(participationRepository, never()).save(any());
        verifyNoInteractions(scoreRuleEngine);
    }

    @Test
    void gradeCompletion_SubmissionRequired_HasGradedSubmission_Successful() {
        activity.setRequiresSubmission(true);

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.existsByActivityAndStudentAndStatus(100L, 10L, SubmissionStatus.GRADED))
                .thenReturn(true); // Has graded submission

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertTrue(response.isStatus());
        assertEquals("Đã chấm điểm completion (điểm đã được tính từ bài nộp)", response.getMessage());
        assertEquals(true, participation.getIsCompleted());

        verify(participationRepository).save(participation);
        verifyNoInteractions(scoreRuleEngine); // Scoring is already handled via the submission grading
    }

    @Test
    void gradeCompletion_InSeries_TriggersSeriesProgress() {
        activity.setRequiresSubmission(false);
        activity.setSeriesId(888L); // Part of series

        when(participationRepository.findById(600L)).thenReturn(Optional.of(participation));

        Response response = activityRegistrationService.gradeCompletion(600L, true, "Completed");

        assertTrue(response.isStatus());
        verify(activitySeriesService).updateStudentProgress(10L, 100L);
    }

    @Test
    void checkIn_TicketFlow_FirstScan_MovesToCheckedIn() {
        participation.setParticipationType(ParticipationType.REGISTERED);
        registration.setTicketCode("TK001");

        when(registrationRepository.findByTicketCode("TK001")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findByUsernameAndIsDeletedFalse("manager_user")).thenReturn(Optional.of(studentUser));

        Response response = activityRegistrationService
                .checkIn(new ActivityParticipationRequest("TK001", null, null, null), "manager_user");

        assertTrue(response.isStatus());
        assertEquals(ParticipationType.CHECKED_IN, participation.getParticipationType());
        assertNotNull(participation.getCheckInTime());
        verify(scoreRuleEngine, never()).applyActivityCompleted(any(), any());
    }

    @Test
    void checkIn_TicketFlow_SecondScan_SubmissionActivity_OnlyAttendedWhenNotYetGraded() {
        activity.setRequiresSubmission(true);
        participation.setParticipationType(ParticipationType.CHECKED_IN);
        registration.setTicketCode("TK002");

        when(registrationRepository.findByTicketCode("TK002")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.findByActivityAndStudentAndStatusOrderByLatest(100L, 10L,
                SubmissionStatus.GRADED))
                .thenReturn(List.of());
        when(userRepository.findByUsernameAndIsDeletedFalse("manager_user")).thenReturn(Optional.of(studentUser));

        Response response = activityRegistrationService
                .checkIn(new ActivityParticipationRequest("TK002", null, null, null), "manager_user");

        assertTrue(response.isStatus());
        assertEquals(ParticipationType.ATTENDED, participation.getParticipationType());
        assertEquals(RegistrationStatus.ATTENDED, registration.getStatus());
        assertNull(participation.getIsCompleted());
        verify(scoreRuleEngine, never()).applySubmissionGraded(any(), any());
    }

    @Test
    void checkIn_TicketFlow_SecondScan_EventBasic_CompletesAndAppliesActivityScore() {
        activity.setRequiresSubmission(false);
        participation.setParticipationType(ParticipationType.CHECKED_IN);
        registration.setTicketCode("TK003");

        when(registrationRepository.findByTicketCode("TK003")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findByUsernameAndIsDeletedFalse("manager_user")).thenReturn(Optional.of(studentUser));

        Response response = activityRegistrationService
                .checkIn(new ActivityParticipationRequest("TK003", null, null, null), "manager_user");

        assertTrue(response.isStatus());
        assertEquals(ParticipationType.COMPLETED, participation.getParticipationType());
        assertEquals(Boolean.TRUE, participation.getIsCompleted());
        assertEquals(RegistrationStatus.ATTENDED, registration.getStatus());
        assertNotNull(participation.getCheckOutTime());
        verify(scoreRuleEngine).applyActivityCompleted(participation, studentUser);
    }

    @Test
    void checkInByQrCode_EventBasicStandalone_CompletesAndAppliesActivityScore() {
        activity.setRequiresSubmission(false);
        activity.setCheckInCode("ACTQR-BASIC");
        participation.setParticipationType(ParticipationType.REGISTERED);

        when(activityRepository.findByCheckInCode("ACTQR-BASIC")).thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));

        Response response = activityRegistrationService.checkInByQrCode("ACTQR-BASIC", 10L);

        assertTrue(response.isStatus());
        assertEquals(ParticipationType.COMPLETED, participation.getParticipationType());
        assertEquals(Boolean.TRUE, participation.getIsCompleted());
        assertEquals(RegistrationStatus.ATTENDED, registration.getStatus());
        assertNotNull(participation.getCheckInTime());
        assertNull(participation.getCheckOutTime());
        verify(scoreRuleEngine).applyActivityCompleted(participation, studentUser);
    }

    @Test
    void checkIn_StudentWithScannerAndOrganizer_Allowed() {
        User studentUserOnly = new User();
        studentUserOnly.setId(44L);
        studentUserOnly.setRole(Role.STUDENT);

        Student studentOnly = new Student();
        studentOnly.setId(11L);
        studentOnly.setUser(studentUserOnly);

        participation.setParticipationType(ParticipationType.REGISTERED);
        registration.setTicketCode("TK_ST_1");

        when(registrationRepository.findByTicketCode("TK_ST_1")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findByUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentUserOnly));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentOnly));
        when(preparationTaskMemberRepository.existsScannerTaskForStudentAndActivity(11L, 100L)).thenReturn(true);
        when(activityOrganizerRepository.existsByActivityIdAndStudentId(100L, 11L)).thenReturn(true);

        Response response = activityRegistrationService.checkIn(new ActivityParticipationRequest("TK_ST_1", null, null, null), "student_scanner");

        assertTrue(response.isStatus());
    }

    @Test
    void checkIn_StudentWithScannerButNotOrganizer_Blocked() {
        User studentUserOnly = new User();
        studentUserOnly.setId(44L);
        studentUserOnly.setRole(Role.STUDENT);

        Student studentOnly = new Student();
        studentOnly.setId(11L);
        studentOnly.setUser(studentUserOnly);

        when(registrationRepository.findByTicketCode("TK_ST_2")).thenReturn(Optional.of(registration));
        when(userRepository.findByUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentUserOnly));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentOnly));
        when(preparationTaskMemberRepository.existsScannerTaskForStudentAndActivity(11L, 100L)).thenReturn(true);
        when(activityOrganizerRepository.existsByActivityIdAndStudentId(100L, 11L)).thenReturn(false);

        ActivityParticipationRequest request = new ActivityParticipationRequest("TK_ST_2", null, null, null);
        assertThrows(vn.campuslife.exception.ForbiddenException.class, () -> 
            activityRegistrationService.checkIn(request, "student_scanner")
        );
    }

    @Test
    void checkIn_StudentWithoutScanner_Blocked() {
        User studentUserOnly = new User();
        studentUserOnly.setId(44L);
        studentUserOnly.setRole(Role.STUDENT);

        Student studentOnly = new Student();
        studentOnly.setId(11L);
        studentOnly.setUser(studentUserOnly);

        when(registrationRepository.findByTicketCode("TK_ST_3")).thenReturn(Optional.of(registration));
        when(userRepository.findByUsernameAndIsDeletedFalse("student_no_scan")).thenReturn(Optional.of(studentUserOnly));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student_no_scan")).thenReturn(Optional.of(studentOnly));
        when(preparationTaskMemberRepository.existsScannerTaskForStudentAndActivity(11L, 100L)).thenReturn(false);

        ActivityParticipationRequest request = new ActivityParticipationRequest("TK_ST_3", null, null, null);
        assertThrows(vn.campuslife.exception.ForbiddenException.class, () -> 
            activityRegistrationService.checkIn(request, "student_no_scan")
        );
    }

    @Test
    void checkInByQrCode_SubmissionAlreadyGraded_CompletesAndAppliesSubmissionScore() {
        activity.setRequiresSubmission(true);
        activity.setCheckInCode("ACTQR");

        participation.setParticipationType(ParticipationType.REGISTERED);

        TaskSubmission gradedSubmission = new TaskSubmission();
        gradedSubmission.setId(700L);
        gradedSubmission.setIsCompleted(false);

        when(activityRepository.findByCheckInCode("ACTQR")).thenReturn(Optional.of(activity));
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(taskSubmissionRepository.findByActivityAndStudentAndStatusOrderByLatest(100L, 10L,
                SubmissionStatus.GRADED))
                .thenReturn(List.of(gradedSubmission));

        Response response = activityRegistrationService.checkInByQrCode("ACTQR", 10L);

        assertTrue(response.isStatus());
        assertEquals(ParticipationType.COMPLETED, participation.getParticipationType());
        assertEquals(Boolean.FALSE, participation.getIsCompleted());
        assertNotNull(participation.getCheckInTime());
        assertNull(participation.getCheckOutTime());
        verify(scoreRuleEngine).applySubmissionGraded(gradedSubmission, studentUser);
    }

    @Test
    void cancelRegistration_StatusIsAttended_ReturnsError() {
        registration.setStatus(RegistrationStatus.ATTENDED);
        when(registrationRepository.findByActivityIdAndStudentId(100L, 10L)).thenReturn(Optional.of(registration));

        Response response = activityRegistrationService.cancelRegistration(100L, 10L);

        assertFalse(response.isStatus());
        assertEquals("Không thể huỷ đăng ký đã điểm danh tham gia (ATTENDED).", response.getMessage());
        verify(registrationRepository, never()).save(any());
    }
}
