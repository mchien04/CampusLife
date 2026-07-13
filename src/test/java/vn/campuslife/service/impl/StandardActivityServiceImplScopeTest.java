package vn.campuslife.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScorePresetService;
import vn.campuslife.service.mapper.StandardActivityMapper;
import vn.campuslife.service.validator.StandardActivityValidator;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandardActivityServiceImplScopeTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private DepartmentRepository departmentRepository;
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
    private StandardActivityValidator validator;
    @Mock
    private StandardActivityMapper mapper;

    @InjectMocks
    private StandardActivityServiceImpl standardActivityService;

    private Department deptA;
    private Department deptB;

    @BeforeEach
    void setUp() {
        deptA = new Department();
        deptA.setId(1L);
        deptA.setName("Dept A");

        deptB = new Department();
        deptB.setId(2L);
        deptB.setName("Dept B");
    }

    @Test
    void createActivity_ManagerWithOutOfScopeOrganizers_Rejected() {
        StandardActivityCreateRequest request = new StandardActivityCreateRequest();
        request.setName("Scoped Activity");
        request.setOrganizerIds(List.of(deptB.getId()));

        DepartmentScope scope = DepartmentScope.manager(Set.of(deptA.getId()));

        Response response = standardActivityService.createActivity(request, scope);

        assertFalse(response.isStatus());
        assertEquals("Organizer departments must be within manager scope", response.getMessage());
    }

    @Test
    void createActivity_ManagerWithCoOrganizerOutsideScope_Allowed() {
        StandardActivityCreateRequest request = new StandardActivityCreateRequest();
        request.setName("Co-organized Activity");
        request.setOrganizerIds(List.of(deptA.getId(), deptB.getId()));

        DepartmentScope scope = DepartmentScope.manager(Set.of(deptA.getId()));

        Activity entity = new Activity();
        entity.setName(request.getName());
        when(mapper.toEntity(request)).thenReturn(entity);
        when(departmentRepository.findAllById(List.of(deptA.getId(), deptB.getId())))
                .thenReturn(List.of(deptA, deptB));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(mapper.toResponse(any(Activity.class))).thenReturn(null);

        Response response = standardActivityService.createActivity(request, scope);

        assertTrue(response.isStatus());
        assertEquals("Activity created successfully", response.getMessage());
    }

    @Test
    void createActivity_ManagerMultiDepartmentWithoutOrganizers_Rejected() {
        StandardActivityCreateRequest request = new StandardActivityCreateRequest();
        request.setName("Scoped Activity");

        DepartmentScope scope = DepartmentScope.manager(Set.of(deptA.getId(), deptB.getId()));

        Response response = standardActivityService.createActivity(request, scope);

        assertFalse(response.isStatus());
        assertEquals("Manager quản lý nhiều Khoa phải chọn organizerIds trong scope", response.getMessage());
    }
}
