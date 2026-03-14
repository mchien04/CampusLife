package vn.campuslife.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vn.campuslife.repository.ActivityOrganizerRepository;
import vn.campuslife.repository.PreparationTaskRepository;
import vn.campuslife.service.StudentService;

@Component("preparationSecurity")
@RequiredArgsConstructor
public class PreparationSecurity {
    private final StudentService studentService;
    private final ActivityOrganizerRepository activityOrganizerRepository;
    private final PreparationTaskRepository preparationTaskRepository;

    public boolean isOrganizer(Long activityId, Authentication authentication) {
        Long studentId = getStudentId(authentication);
        if (studentId == null) {
            return false;
        }
        return activityOrganizerRepository.existsByActivityIdAndStudentId(activityId, studentId);
    }

    public boolean isAssignee(Long taskId, Authentication authentication) {
        Long studentId = getStudentId(authentication);
        if (studentId == null) {
            return false;
        }
        return preparationTaskRepository.findByIdAndAssigneeId(taskId, studentId).isPresent();
    }

    private Long getStudentId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        try {
            return studentService.getStudentIdByUsername(authentication.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
