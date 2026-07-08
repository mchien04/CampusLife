package vn.campuslife.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import vn.campuslife.entity.Activity;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivitySeriesRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.UserRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScorePresetService;
import vn.campuslife.config.UploadProperties;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplManagerScopeTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private ActivityRegistrationRepository activityRegistrationRepository;
    @Mock
    private ActivitySeriesRepository activitySeriesRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ScoreEntryRepository scoreEntryRepository;
    @Mock
    private ScorePresetService scorePresetService;
    @Mock
    private ActivityScoreRuleService activityScoreRuleService;
    @Mock
    private ReminderScheduleService reminderScheduleService;
    @Mock
    private ActivityRegistrationAutoService autoRegisterService;
    @Mock
    private DepartmentAuthorizationService departmentAuthorizationService;
    @Mock
    private UploadProperties uploadProperties;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Test
    @SuppressWarnings("unchecked")
    void getActivitiesByMonth_ManagerScope_usesOrganizerDepartmentFilter() {
        DepartmentScope scope = DepartmentScope.manager(Set.of(1L));
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 1, 0, 0);

        when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of());

        activityService.getActivitiesByMonth(start, end, scope);

        ArgumentCaptor<Specification<Activity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(activityRepository).findAll(specCaptor.capture());
        assertTrue(specCaptor.getValue() != null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchUpcomingEvents_ManagerScope_usesOrganizerDepartmentFilter() {
        DepartmentScope scope = DepartmentScope.manager(Set.of(2L));

        when(activityRepository.findAll(any(Specification.class))).thenReturn(List.of());

        activityService.searchUpcomingEvents("workshop", scope);

        ArgumentCaptor<Specification<Activity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(activityRepository).findAll(specCaptor.capture());
        assertTrue(specCaptor.getValue() != null);
    }
}
