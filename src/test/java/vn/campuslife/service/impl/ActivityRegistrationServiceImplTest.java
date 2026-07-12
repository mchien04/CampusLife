package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.*;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.EventTimeStatus;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import vn.campuslife.enumeration.SubmissionStatus;
import vn.campuslife.enumeration.Role;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.ActivityParticipationRequest;
import vn.campuslife.model.activity.PersonalCalendarEventItem;
import vn.campuslife.model.activity.PersonalCalendarResponse;
import vn.campuslife.repository.*;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.SemesterHelperService;
import vn.campuslife.service.ActivitySeriesService;
import vn.campuslife.service.NotificationService;
import vn.campuslife.service.ScoreRuleEngine;
import vn.campuslife.config.UploadProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private DepartmentAuthorizationService departmentAuthorizationService;

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
    void checkIn_StudentWithScannerButNotOrganizer_Allowed() {
        User studentUserOnly = new User();
        studentUserOnly.setId(44L);
        studentUserOnly.setRole(Role.STUDENT);

        Student studentOnly = new Student();
        studentOnly.setId(11L);
        studentOnly.setUser(studentUserOnly);

        participation.setParticipationType(ParticipationType.REGISTERED);
        registration.setTicketCode("TK_ST_2");

        when(registrationRepository.findByTicketCode("TK_ST_2")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findByUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentUserOnly));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student_scanner")).thenReturn(Optional.of(studentOnly));
        when(preparationTaskMemberRepository.existsScannerTaskForStudentAndActivity(11L, 100L)).thenReturn(true);
        when(activityOrganizerRepository.existsByActivityIdAndStudentId(100L, 11L)).thenReturn(false);

        Response response = activityRegistrationService.checkIn(
                new ActivityParticipationRequest("TK_ST_2", null, null, null),
                "student_scanner");

        assertTrue(response.isStatus());
    }

    @Test
    void checkIn_StudentOrganizerWithoutScanner_Allowed() {
        User studentUserOnly = new User();
        studentUserOnly.setId(44L);
        studentUserOnly.setRole(Role.STUDENT);

        Student studentOnly = new Student();
        studentOnly.setId(11L);
        studentOnly.setUser(studentUserOnly);

        participation.setParticipationType(ParticipationType.REGISTERED);
        registration.setTicketCode("TK_ORG_1");

        when(registrationRepository.findByTicketCode("TK_ORG_1")).thenReturn(Optional.of(registration));
        when(participationRepository.findByRegistration(registration)).thenReturn(Optional.of(participation));
        when(userRepository.findByUsernameAndIsDeletedFalse("student_org")).thenReturn(Optional.of(studentUserOnly));
        when(studentRepository.findByUserUsernameAndIsDeletedFalse("student_org")).thenReturn(Optional.of(studentOnly));
        when(preparationTaskMemberRepository.existsScannerTaskForStudentAndActivity(11L, 100L)).thenReturn(false);
        when(activityOrganizerRepository.existsByActivityIdAndStudentId(100L, 11L)).thenReturn(true);

        Response response = activityRegistrationService.checkIn(
                new ActivityParticipationRequest("TK_ORG_1", null, null, null),
                "student_org");

        assertTrue(response.isStatus());
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
    void getActivityRegistrations_ManagerWithActivityAccess_ReturnsAllActivityRegistrations() {
        DepartmentScope scope = DepartmentScope.manager(Set.of(1L));
        when(registrationRepository.findByActivityIdAndActivityIsDeletedFalse(9L))
                .thenReturn(List.of(registration));

        Response response = activityRegistrationService.getActivityRegistrations(9L, scope);

        assertTrue(response.isStatus());
        assertEquals(1, ((List<?>) response.getBody()).size());
        verify(departmentAuthorizationService).requireActivityAccess(9L, scope);
        verify(registrationRepository).findByActivityIdAndActivityIsDeletedFalse(9L);
    }

    @Test
    void updateRegistrationStatus_ManagerWithActivityAccess_ApprovesWithoutStudentDepartmentCheck() {
        DepartmentScope scope = DepartmentScope.manager(Set.of(1L));
        registration.setStatus(RegistrationStatus.PENDING);

        when(registrationRepository.findById(50L)).thenReturn(Optional.of(registration));
        when(registrationRepository.save(any(ActivityRegistration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRepository.existsByRegistration(registration)).thenReturn(false);
        when(participationRepository.save(any(ActivityParticipation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Response response = activityRegistrationService.updateRegistrationStatus(50L, "APPROVED", scope);

        assertTrue(response.isStatus());
        verify(departmentAuthorizationService).requireActivityAccess(100L, scope);
        verify(departmentAuthorizationService, never()).requireStudentAccess(anyLong(), any());
        verify(participationRepository).save(any(ActivityParticipation.class));
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

    @Test
    void getStudentJoinedEventDates_MultiDayExpandsMarkedDatesAndFiltersByDate() {
        activity.setStartDate(LocalDateTime.of(2025, 7, 12, 8, 0));
        activity.setEndDate(LocalDateTime.of(2025, 7, 13, 17, 0));
        activity.setLocation("A1");
        activity.setType(ActivityType.SUKIEN);
        activity.setBannerUrl("/uploads/banner.png");
        activity.setShareLink("https://example.com/a/100");
        activity.setImportant(true);
        registration.setTicketCode("TKT001");
        registration.setSeriesId(null);

        Activity singleDay = new Activity();
        singleDay.setId(101L);
        singleDay.setName("Workshop");
        singleDay.setStartDate(LocalDateTime.of(2025, 7, 12, 14, 0));
        singleDay.setEndDate(LocalDateTime.of(2025, 7, 12, 16, 0));
        singleDay.setDeleted(false);

        ActivityRegistration other = new ActivityRegistration();
        other.setId(51L);
        other.setActivity(singleDay);
        other.setStudent(student);
        other.setStatus(RegistrationStatus.ATTENDED);

        Activity outsideRange = new Activity();
        outsideRange.setId(102L);
        outsideRange.setName("Old Event");
        outsideRange.setStartDate(LocalDateTime.of(2025, 6, 1, 9, 0));
        outsideRange.setEndDate(LocalDateTime.of(2025, 6, 1, 11, 0));
        outsideRange.setDeleted(false);

        ActivityRegistration oldReg = new ActivityRegistration();
        oldReg.setId(52L);
        oldReg.setActivity(outsideRange);
        oldReg.setStudent(student);
        oldReg.setStatus(RegistrationStatus.APPROVED);

        when(registrationRepository.findByStudentIdAndStudentIsDeletedFalse(10L))
                .thenReturn(List.of(registration, other, oldReg));
        when(uploadProperties.getPublicUrl()).thenReturn("https://cdn.example.com");

        LocalDate from = LocalDate.of(2025, 7, 1);
        LocalDate to = LocalDate.of(2025, 7, 31);
        LocalDate date = LocalDate.of(2025, 7, 12);

        Response response = activityRegistrationService.getStudentJoinedEventDates(10L, from, to, date);

        assertTrue(response.isStatus());
        assertInstanceOf(PersonalCalendarResponse.class, response.getBody());
        PersonalCalendarResponse calendar = (PersonalCalendarResponse) response.getBody();

        assertEquals(from, calendar.getFrom());
        assertEquals(to, calendar.getTo());
        assertEquals(2, calendar.getMarkedDates().size());
        assertEquals(LocalDate.of(2025, 7, 12), calendar.getMarkedDates().get(0).getDate());
        assertEquals(2, calendar.getMarkedDates().get(0).getEventCount());
        assertEquals(LocalDate.of(2025, 7, 13), calendar.getMarkedDates().get(1).getDate());
        assertEquals(1, calendar.getMarkedDates().get(1).getEventCount());

        assertEquals(2, calendar.getEvents().size());
        assertTrue(calendar.getEvents().stream().anyMatch(e -> e.getActivityId().equals(100L)));
        assertTrue(calendar.getEvents().stream().anyMatch(e -> e.getActivityId().equals(101L)));
        assertFalse(calendar.getEvents().stream().anyMatch(e -> e.getActivityId().equals(102L)));

        PersonalCalendarEventItem multiDay = calendar.getEvents().stream()
                .filter(e -> e.getActivityId().equals(100L))
                .findFirst()
                .orElseThrow();
        assertEquals("https://cdn.example.com/uploads/banner.png", multiDay.getBannerUrl());
        assertEquals(EventTimeStatus.PAST, multiDay.getEventTimeStatus());
        assertEquals(ActivityType.SUKIEN, multiDay.getActivityType());
        assertTrue(multiDay.isImportant());
    }

    @Test
    void getStudentJoinedEventDates_InvalidRange_ReturnsError() {
        Response response = activityRegistrationService.getStudentJoinedEventDates(
                10L,
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 7, 1),
                null);

        assertFalse(response.isStatus());
        assertEquals("from must be on or before to", response.getMessage());
        verifyNoInteractions(registrationRepository);
    }
}
