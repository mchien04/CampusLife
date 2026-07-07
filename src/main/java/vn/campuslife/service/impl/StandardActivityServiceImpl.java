package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.model.Response;
import vn.campuslife.model.activity.StandardActivityCreateRequest;
import vn.campuslife.model.activity.StandardActivityUpdateRequest;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.DepartmentRepository;
import vn.campuslife.repository.ScoreEntryRepository;
import vn.campuslife.security.department.DepartmentAuthorizationService;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.service.ActivityRegistrationAutoService;
import vn.campuslife.service.ActivityScoreRuleService;
import vn.campuslife.service.ReminderScheduleService;
import vn.campuslife.service.ScorePresetService;
import vn.campuslife.service.StandardActivityService;
import vn.campuslife.service.mapper.StandardActivityMapper;
import vn.campuslife.service.validator.StandardActivityValidator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandardActivityServiceImpl implements StandardActivityService {

    private static final Logger logger = LoggerFactory.getLogger(StandardActivityServiceImpl.class);

    private final ActivityRepository activityRepository;
    private final DepartmentRepository departmentRepository;
    private final ScoreEntryRepository scoreEntryRepository;
    private final ScorePresetService scorePresetService;
    private final ActivityScoreRuleService activityScoreRuleService;
    private final ReminderScheduleService reminderScheduleService;
    private final ActivityRegistrationAutoService autoRegisterService;
    private final DepartmentAuthorizationService departmentAuthorizationService;

    private final StandardActivityValidator validator;
    private final StandardActivityMapper mapper;

    @Override
    @Transactional
    public Response createActivity(StandardActivityCreateRequest request) {
        return createActivityInternal(request, null);
    }

    @Override
    @Transactional
    public Response createActivity(StandardActivityCreateRequest request, DepartmentScope scope) {
        return createActivityInternal(request, scope);
    }

    private Response createActivityInternal(StandardActivityCreateRequest request, DepartmentScope scope) {
        try {
            scorePresetService.applyActivityPreset(request);
            validator.validate(request);

            Set<Department> organizers = resolveOrganizersForScope(request.getOrganizerIds(), scope);

            Activity entity = mapper.toEntity(request);
            entity.setOrganizers(organizers);

            Activity saved = activityRepository.save(entity);

            if (saved.getCheckInCode() == null || saved.getCheckInCode().isBlank()) {
                String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase().replace("-", "");
                String checkInCode = String.format("ACT-%06d-%s", saved.getId(), random);
                saved.setCheckInCode(checkInCode);
                saved = activityRepository.save(saved);
            }

            if (request.getScoreRules() != null && !request.getScoreRules().isEmpty()) {
                activityScoreRuleService.replaceRules(saved.getId(), request.getScoreRules());
            }

            autoRegisterService.autoRegisterStudents(saved);
            reminderScheduleService.syncEventRemindersForActivity(saved);

            return Response.success("Activity created successfully", mapper.toResponse(saved));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid create activity request: {}", e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create standard activity: {}", e.getMessage(), e);
            return Response.error("Failed to create activity due to server error");
        }
    }

    @Override
    @Transactional
    public Response updateActivity(Long id, StandardActivityUpdateRequest request) {
        return updateActivityInternal(id, request, null);
    }

    @Override
    @Transactional
    public Response updateActivity(Long id, StandardActivityUpdateRequest request, DepartmentScope scope) {
        return updateActivityInternal(id, request, scope);
    }

    private Response updateActivityInternal(Long id, StandardActivityUpdateRequest request, DepartmentScope scope) {
        try {
            if (scope != null && scope.manager() && !scope.admin()) {
                departmentAuthorizationService.requireActivityAccess(id, scope);
            }
            Optional<Activity> opt = activityRepository.findByIdAndIsDeletedFalse(id);
            if (opt.isEmpty()) {
                return Response.error("Activity not found");
            }
            Activity existing = opt.get();

            if (request.getPresetCode() != null && existing.getPresetCode() != null
                    && request.getPresetCode() != existing.getPresetCode()) {
                return Response.error("Cannot change preset code from " + existing.getPresetCode()
                        + " to " + request.getPresetCode() + " on update. "
                        + "You can only customize score rules within the current preset.");
            }

            ActivityType effectiveType = request.getType() != null ? request.getType() : existing.getType();

            if (request.getType() != null && request.getType() != existing.getType()) {
                long activeEntries = scoreEntryRepository.countByActivityIdAndStatus(id, ScoreEntryStatus.ACTIVE);
                if (activeEntries > 0 && !existing.isDraft()) {
                    return Response.error(
                            "Cannot change type when activity has " + activeEntries
                            + " active score entries and is not draft. "
                            + "Unpublish the activity first.");
                }
            }

            scorePresetService.applyActivityPreset(request, effectiveType);

            mapper.applyUpdate(existing, request);

            if (request.getOrganizerIds() != null && !request.getOrganizerIds().isEmpty()) {
                existing.setOrganizers(resolveOrganizersForScope(request.getOrganizerIds(), scope));
            }

            Activity saved = activityRepository.save(existing);

            if (request.getScoreRules() != null) {
                activityScoreRuleService.replaceRules(saved.getId(), request.getScoreRules());
            }

            autoRegisterService.autoRegisterStudents(saved);

            return Response.success("Activity updated successfully", mapper.toResponse(saved));
        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Cannot modify score rules for activity {}: {}", id, e.getMessage());
            return Response.error(e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to update standard activity: {}", e.getMessage(), e);
            return Response.error("Failed to update activity");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivity(Long id) {
        return getActivityInternal(id, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Response getActivity(Long id, DepartmentScope scope) {
        return getActivityInternal(id, scope);
    }

    private Response getActivityInternal(Long id, DepartmentScope scope) {
        if (scope != null && scope.manager() && !scope.admin()) {
            departmentAuthorizationService.requireActivityAccess(id, scope);
        }
        Optional<Activity> opt = activityRepository.findByIdAndIsDeletedFalse(id);
        if (opt.isEmpty()) {
            return Response.error("Activity not found");
        }
        return Response.success("Activity retrieved successfully", mapper.toResponse(opt.get()));
    }

    private Set<Department> resolveOrganizers(List<Long> organizerIds) {
        if (organizerIds == null || organizerIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        var deps = departmentRepository.findAllById(organizerIds);
        var found = deps.stream().map(Department::getId).collect(Collectors.toSet());
        var missing = organizerIds.stream().filter(id -> !found.contains(id)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Department ids not found: " + missing);
        }
        return new LinkedHashSet<>(deps);
    }

    private Set<Department> resolveOrganizersForScope(List<Long> organizerIds, DepartmentScope scope) {
        if (scope == null || !scope.manager() || scope.admin()) {
            return resolveOrganizers(organizerIds);
        }
        Set<Long> managerDepartmentIds = scope.departmentIds();
        if (organizerIds == null || organizerIds.isEmpty()) {
            if (managerDepartmentIds.size() == 1) {
                return resolveOrganizers(List.copyOf(managerDepartmentIds));
            }
            throw new IllegalArgumentException("Manager quản lý nhiều Khoa phải chọn organizerIds trong scope");
        }
        if (!managerDepartmentIds.containsAll(new LinkedHashSet<>(organizerIds))) {
            throw new IllegalArgumentException("Organizer departments must be within manager scope");
        }
        return resolveOrganizers(organizerIds);
    }
}
