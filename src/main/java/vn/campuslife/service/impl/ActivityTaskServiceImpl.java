package vn.campuslife.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.ActivityTask;
import vn.campuslife.entity.Student;
import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.enumeration.TaskStatus;
import vn.campuslife.model.*;
import vn.campuslife.repository.ActivityRegistrationRepository;
import vn.campuslife.repository.ActivityRepository;
import vn.campuslife.repository.ActivityTaskRepository;
import vn.campuslife.repository.StudentRepository;
import vn.campuslife.repository.TaskAssignmentRepository;
import vn.campuslife.repository.TaskSubmissionRepository;
import vn.campuslife.service.ActivityTaskService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityTaskServiceImpl implements ActivityTaskService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityTaskServiceImpl.class);

    private final ActivityTaskRepository activityTaskRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRegistrationRepository activityRegistrationRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final StudentRepository studentRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;

    @Override
    @Transactional
    public Response createTask(CreateActivityTaskRequest request) {
        try {
            // Validate activity exists and not deleted
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            Activity activity = activityOpt.get();

            // Validate deadline if provided
            if (request.getDeadline() != null && activity.getEndDate() != null
                    && request.getDeadline().isBefore(activity.getEndDate())) {
                return new Response(false, "Task deadline must be after activity end date", null);
            }

            // Create new task
            ActivityTask task = new ActivityTask();
            task.setActivity(activity);
            task.setName(request.getName());
            task.setDescription(request.getDescription());
            task.setDeadline(request.getDeadline());

            ActivityTask savedTask = activityTaskRepository.save(task);
            ActivityTaskResponse response = toTaskResponse(savedTask);

            return new Response(true, "Task created successfully", response);
        } catch (Exception e) {
            logger.error("Failed to create task: {}", e.getMessage(), e);
            return new Response(false, "Failed to create task due to server error", null);
        }
    }

    @Override
    public Response getTasksByActivity(Long activityId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            List<ActivityTask> tasks = activityTaskRepository.findByActivityIdAndActivityIsDeletedFalse(activityId);
            List<ActivityTaskResponse> taskResponses = tasks.stream()
                    .map(this::toTaskResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Tasks retrieved successfully", taskResponses);
        } catch (Exception e) {
            logger.error("Failed to retrieve tasks for activity {}: {}", activityId, e.getMessage(), e);
            return new Response(false, "Failed to retrieve tasks due to server error", null);
        }
    }

    @Override
    public Response getTaskById(Long taskId) {
        try {
            Optional<ActivityTask> taskOpt = activityTaskRepository.findByIdAndActivityIsDeletedFalse(taskId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
            }

            ActivityTaskResponse response = toTaskResponse(taskOpt.get());
            return new Response(true, "Task retrieved successfully", response);
        } catch (Exception e) {
            logger.error("Failed to retrieve task {}: {}", taskId, e.getMessage(), e);
            return new Response(false, "Failed to retrieve task due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateTask(Long taskId, CreateActivityTaskRequest request) {
        try {
            Optional<ActivityTask> taskOpt = activityTaskRepository.findByIdAndActivityIsDeletedFalse(taskId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
            }

            ActivityTask task = taskOpt.get();

            // Validate activity if changed
            if (!task.getActivity().getId().equals(request.getActivityId())) {
                Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(request.getActivityId());
                if (activityOpt.isEmpty()) {
                    return new Response(false, "Activity not found", null);
                }
                task.setActivity(activityOpt.get());
            }

            // Validate deadline if provided
            if (request.getDeadline() != null && task.getActivity().getEndDate() != null
                    && request.getDeadline().isBefore(task.getActivity().getEndDate())) {
                return new Response(false, "Task deadline must be after activity end date", null);
            }

            // Update task
            task.setName(request.getName());
            task.setDescription(request.getDescription());
            task.setDeadline(request.getDeadline());

            ActivityTask savedTask = activityTaskRepository.save(task);
            ActivityTaskResponse response = toTaskResponse(savedTask);

            return new Response(true, "Task updated successfully", response);
        } catch (Exception e) {
            logger.error("Failed to update task {}: {}", taskId, e.getMessage(), e);
            return new Response(false, "Failed to update task due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response deleteTask(Long taskId) {
        try {
            Optional<ActivityTask> taskOpt = activityTaskRepository.findByIdAndActivityIsDeletedFalse(taskId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
            }

            // Check if task has assignments
            List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);
            if (!assignments.isEmpty()) {
                return new Response(false, "Cannot delete task with existing assignments", null);
            }

            activityTaskRepository.deleteById(taskId);
            return new Response(true, "Task deleted successfully", null);
        } catch (Exception e) {
            logger.error("Failed to delete task {}: {}", taskId, e.getMessage(), e);
            return new Response(false, "Failed to delete task due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response assignTask(TaskAssignmentRequest request) {
        try {
            // Validate task exists
            Optional<ActivityTask> taskOpt = activityTaskRepository
                    .findByIdAndActivityIsDeletedFalse(request.getTaskId());
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found", null);
            }

            ActivityTask task = taskOpt.get();

            // Validate students exist
            List<Student> students = studentRepository.findAllById(request.getStudentIds());
            if (students.size() != request.getStudentIds().size()) {
                return new Response(false, "Some students not found", null);
            }

            // Create assignments - luôn set status PENDING khi phân công
            List<TaskAssignment> assignments = students.stream()
                    .filter(student -> !taskAssignmentRepository.existsByTaskIdAndStudentId(request.getTaskId(),
                            student.getId()))
                    .map(student -> {
                        TaskAssignment assignment = new TaskAssignment();
                        assignment.setTask(task);
                        assignment.setStudent(student);
                        assignment.setStatus(TaskStatus.PENDING); // Luôn PENDING khi phân công
                        return assignment;
                    })
                    .collect(Collectors.toList());

            List<TaskAssignment> savedAssignments = taskAssignmentRepository.saveAll(assignments);
            List<TaskAssignmentResponse> responses = savedAssignments.stream()
                    .map(this::toAssignmentResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Tasks assigned successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to assign tasks: {}", e.getMessage(), e);
            return new Response(false, "Failed to assign tasks due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response updateTaskStatus(Long assignmentId, String status) {
        try {
            Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return new Response(false, "Task assignment not found", null);
            }

            TaskAssignment assignment = assignmentOpt.get();
            TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
            assignment.setStatus(taskStatus);

            TaskAssignment savedAssignment = taskAssignmentRepository.save(assignment);
            TaskAssignmentResponse response = toAssignmentResponse(savedAssignment);

            return new Response(true, "Task status updated successfully", response);
        } catch (IllegalArgumentException e) {
            return new Response(false, "Invalid status: " + status, null);
        } catch (Exception e) {
            logger.error("Failed to update task status for assignment {}: {}", assignmentId, e.getMessage(), e);
            return new Response(false, "Failed to update task status due to server error", null);
        }
    }

    @Override
    public Response getStudentTasks(Long studentId) {
        try {
            List<TaskAssignment> assignments = taskAssignmentRepository
                    .findByStudentIdAndTaskActivityIsDeletedFalse(studentId);
            List<TaskAssignmentResponse> responses = assignments.stream()
                    .map(this::toAssignmentResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Student tasks retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve tasks for student {}: {}", studentId, e.getMessage(), e);
            return new Response(false, "Failed to retrieve student tasks due to server error", null);
        }
    }

    @Override
    public Response getTaskAssignments(Long taskId) {
        try {
            List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(taskId);
            List<TaskAssignmentResponse> responses = assignments.stream()
                    .map(this::toAssignmentResponse)
                    .collect(Collectors.toList());

            return new Response(true, "Task assignments retrieved successfully", responses);
        } catch (Exception e) {
            logger.error("Failed to retrieve assignments for task {}: {}", taskId, e.getMessage(), e);
            return new Response(false, "Failed to retrieve task assignments due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response removeTaskAssignment(Long assignmentId) {
        try {
            Optional<TaskAssignment> assignmentOpt = taskAssignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return new Response(false, "Task assignment not found", null);
            }

            taskAssignmentRepository.deleteById(assignmentId);
            return new Response(true, "Task assignment removed successfully", null);
        } catch (Exception e) {
            logger.error("Failed to remove task assignment {}: {}", assignmentId, e.getMessage(), e);
            return new Response(false, "Failed to remove task assignment due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response autoAssignMandatoryTasks(Long activityId) {
        try {
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            Activity activity = activityOpt.get();
            if (!activity.isMandatoryForFacultyStudents()) {
                return new Response(false, "Activity is not mandatory for faculty students", null);
            }

            // Get all tasks for this activity
            List<ActivityTask> tasks = activityTaskRepository.findByActivityIdAndActivityIsDeletedFalse(activityId);
            if (tasks.isEmpty()) {
                return new Response(false, "No tasks found for this activity", null);
            }

            // Get students from organizing departments
            List<Long> departmentIds = activity.getOrganizers().stream()
                    .map(department -> department.getId())
                    .collect(Collectors.toList());

            List<Student> students = studentRepository.findByDepartmentIdInAndIsDeletedFalse(departmentIds);

            // Assign all tasks to all students
            int totalAssignments = 0;
            for (ActivityTask task : tasks) {
                for (Student student : students) {
                    if (!taskAssignmentRepository.existsByTaskIdAndStudentId(task.getId(), student.getId())) {
                        TaskAssignment assignment = new TaskAssignment();
                        assignment.setTask(task);
                        assignment.setStudent(student);
                        assignment.setStatus(TaskStatus.PENDING);
                        taskAssignmentRepository.save(assignment);
                        totalAssignments++;
                    }
                }
            }

            return new Response(true,
                    String.format("Auto-assigned %d tasks to %d students", totalAssignments, students.size()),
                    null);
        } catch (Exception e) {
            logger.error("Failed to auto-assign mandatory tasks for activity {}: {}", activityId, e.getMessage(), e);
            return new Response(false, "Failed to auto-assign mandatory tasks due to server error", null);
        }
    }

    private ActivityTaskResponse toTaskResponse(ActivityTask task) {
        ActivityTaskResponse response = new ActivityTaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setDeadline(task.getDeadline());
        response.setActivityId(task.getActivity().getId());
        response.setActivityName(task.getActivity().getName());
        response.setCreatedAt(task.getCreatedAt());

        // Get assignments for this task
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTaskId(task.getId());
        response.setAssignments(assignments.stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList()));

        // Calculate statistics
        response.setTotalAssignments((long) assignments.size());
        response.setCompletedAssignments(assignments.stream()
                .mapToLong(a -> a.getStatus() == TaskStatus.COMPLETED ? 1 : 0)
                .sum());
        response.setPendingAssignments(assignments.stream()
                .mapToLong(a -> a.getStatus() == TaskStatus.PENDING ? 1 : 0)
                .sum());

        return response;
    }

    private TaskAssignmentResponse toAssignmentResponse(TaskAssignment assignment) {
        TaskAssignmentResponse response = new TaskAssignmentResponse();
        response.setId(assignment.getId());
        response.setTaskId(assignment.getTask().getId());
        response.setTaskName(assignment.getTask().getName());
        response.setStudentId(assignment.getStudent().getId());
        response.setStudentName(assignment.getStudent().getFullName());
        response.setStudentCode(assignment.getStudent().getStudentCode());
        response.setStatus(assignment.getStatus());
        response.setUpdatedAt(assignment.getUpdatedAt());
        // Note: TaskAssignment doesn't have createdAt field, using updatedAt as
        // fallback
        response.setCreatedAt(assignment.getUpdatedAt());
        return response;
    }

    @Override
    public Response getRegisteredStudentsForActivity(Long activityId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            // Get registered students for this activity
            List<ActivityRegistration> registrations = activityRegistrationRepository
                    .findByActivityIdAndActivityIsDeletedFalse(activityId);

            List<Map<String, Object>> students = registrations.stream()
                    .map(registration -> {
                        Map<String, Object> studentInfo = new HashMap<>();
                        studentInfo.put("id", registration.getStudent().getId());
                        studentInfo.put("studentCode", registration.getStudent().getStudentCode());
                        studentInfo.put("fullName", registration.getStudent().getFullName());
                        studentInfo.put("email", registration.getStudent().getUser().getEmail());
                        studentInfo.put("phone", registration.getStudent().getPhone());
                        studentInfo.put("departmentName", registration.getStudent().getDepartment() != null
                                ? registration.getStudent().getDepartment().getName()
                                : null);
                        studentInfo.put("className", registration.getStudent().getStudentClass() != null
                                ? registration.getStudent().getStudentClass().getClassName()
                                : null);
                        studentInfo.put("registrationStatus", registration.getStatus());
                        studentInfo.put("registeredDate", registration.getRegisteredDate());
                        return studentInfo;
                    })
                    .collect(Collectors.toList());

            return new Response(true, "Registered students retrieved successfully", students);
        } catch (Exception e) {
            logger.error("Failed to get registered students for activity {}: {}", activityId, e.getMessage(), e);
            return new Response(false, "Failed to get registered students due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response assignTaskToRegisteredStudents(Long activityId, Long taskId) {
        try {
            // Validate activity exists
            Optional<Activity> activityOpt = activityRepository.findByIdAndIsDeletedFalse(activityId);
            if (activityOpt.isEmpty()) {
                return new Response(false, "Activity not found", null);
            }

            // Validate task exists and belongs to this activity
            Optional<ActivityTask> taskOpt = activityTaskRepository
                    .findByIdAndActivityIdAndActivityIsDeletedFalse(taskId, activityId);
            if (taskOpt.isEmpty()) {
                return new Response(false, "Task not found or doesn't belong to this activity", null);
            }

            ActivityTask task = taskOpt.get();

            // Get registered students for this activity
            List<ActivityRegistration> registrations = activityRegistrationRepository
                    .findByActivityIdAndActivityIsDeletedFalse(activityId);

            if (registrations.isEmpty()) {
                return new Response(false, "No registered students found for this activity", null);
            }

            // Create assignments for registered students
            List<TaskAssignment> assignments = new ArrayList<>();
            int assignedCount = 0;

            for (ActivityRegistration registration : registrations) {
                Student student = registration.getStudent();

                // Check if already assigned
                if (!taskAssignmentRepository.existsByTaskIdAndStudentId(taskId, student.getId())) {
                    TaskAssignment assignment = new TaskAssignment();
                    assignment.setTask(task);
                    assignment.setStudent(student);
                    assignment.setStatus(TaskStatus.PENDING);
                    assignments.add(assignment);
                    assignedCount++;
                }
            }

            if (assignments.isEmpty()) {
                return new Response(false, "All registered students already have this task assigned", null);
            }

            // Save assignments
            List<TaskAssignment> savedAssignments = taskAssignmentRepository.saveAll(assignments);
            List<TaskAssignmentResponse> responses = savedAssignments.stream()
                    .map(this::toAssignmentResponse)
                    .collect(Collectors.toList());

            return new Response(true,
                    String.format("Task assigned to %d registered students", assignedCount),
                    responses);
        } catch (Exception e) {
            logger.error("Failed to assign task to registered students: {}", e.getMessage(), e);
            return new Response(false, "Failed to assign task due to server error", null);
        }
    }

    @Override
    @Transactional
    public Response checkAndUpdateOverdueAssignments() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = 0;

            // Lấy tất cả assignments có status PENDING hoặc ASSIGNED
            List<TaskAssignment> assignments = taskAssignmentRepository.findAll().stream()
                    .filter(assignment -> {
                        TaskStatus status = assignment.getStatus();
                        return (status == TaskStatus.PENDING || status == TaskStatus.ASSIGNED)
                                && assignment.getTask().getDeadline() != null
                                && assignment.getTask().getDeadline().isBefore(now);
                    })
                    .collect(Collectors.toList());

            // Kiểm tra xem đã có submission chưa
            for (TaskAssignment assignment : assignments) {
                // Chỉ set OVERDUE nếu:
                // 1. Đã quá hạn (deadline < now)
                // 2. Chưa có submission (chưa nộp bài)
                // 3. Status chưa phải COMPLETED
                boolean hasSubmission = taskSubmissionRepository
                        .findByTaskIdAndStudentIdAndIsDeletedFalse(
                                assignment.getTask().getId(),
                                assignment.getStudent().getId())
                        .isPresent();

                if (!hasSubmission
                        && assignment.getTask().getDeadline() != null
                        && assignment.getTask().getDeadline().isBefore(now)
                        && assignment.getStatus() != TaskStatus.COMPLETED) {
                    assignment.setStatus(TaskStatus.OVERDUE);
                    taskAssignmentRepository.save(assignment);
                    updatedCount++;
                }
            }

            logger.info("Updated {} assignments to OVERDUE status", updatedCount);
            return new Response(true,
                    String.format("Updated %d assignments to OVERDUE status", updatedCount),
                    Map.of("updatedCount", updatedCount));
        } catch (Exception e) {
            logger.error("Failed to check and update overdue assignments: {}", e.getMessage(), e);
            return new Response(false, "Failed to check overdue assignments: " + e.getMessage(), null);
        }
    }
}
