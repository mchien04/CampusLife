package vn.campuslife.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import vn.campuslife.entity.Expense;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.repository.ExpenseRepository;
import vn.campuslife.repository.PreparationTaskMemberRepository;
import vn.campuslife.repository.PreparationTaskRepository;
import vn.campuslife.service.StudentService;

@Component("preparationFinanceSecurity")
@RequiredArgsConstructor
public class PreparationFinanceSecurity {
    private final StudentService studentService;
    private final PreparationTaskRepository preparationTaskRepository;
    private final PreparationTaskMemberRepository preparationTaskMemberRepository;
    private final ExpenseRepository expenseRepository;

    public boolean isTaskLeader(Long taskId, Authentication authentication) {
        Long studentId = getStudentId(authentication);
        if (studentId == null) {
            return false;
        }
        return preparationTaskRepository.findById(taskId)
                .map(PreparationTask::getOwner)
                .map(o -> o.getId().equals(studentId))
                .orElse(false);
    }

    public boolean isTaskMember(Long taskId, Authentication authentication) {
        Long studentId = getStudentId(authentication);
        if (studentId == null) {
            return false;
        }
        boolean isLeader = preparationTaskRepository.findById(taskId)
                .map(PreparationTask::getOwner)
                .map(o -> o.getId().equals(studentId))
                .orElse(false);
        if (isLeader) {
            return true;
        }
        return preparationTaskMemberRepository.existsByTaskIdAndStudentId(taskId, studentId);
    }

    public boolean canLeaderDecideExpense(Long expenseId, Authentication authentication) {
        Long studentId = getStudentId(authentication);
        if (studentId == null) {
            return false;
        }
        return expenseRepository.findById(expenseId)
                .map(Expense::getTask)
                .map(PreparationTask::getOwner)
                .map(o -> o.getId().equals(studentId))
                .orElse(false);
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

