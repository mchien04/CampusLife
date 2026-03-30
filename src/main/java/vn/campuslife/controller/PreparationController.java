package vn.campuslife.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.campuslife.entity.PreparationTask;
import vn.campuslife.entity.Student;
import vn.campuslife.model.Response;
import vn.campuslife.model.TaskStatsRespone;
import vn.campuslife.model.preparation.*;
import vn.campuslife.service.PreparationService;
import vn.campuslife.service.StudentService;

import java.util.List;

import java.util.List;

@RestController
@RequestMapping("/api/preparation")
@RequiredArgsConstructor
public class PreparationController {

    private final PreparationService preparationService;

    private final StudentService userService;

    @PutMapping("/activities/{activityId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> togglePreparation(@PathVariable Long activityId, @RequestParam boolean enabled) {
        preparationService.togglePreparation(activityId, enabled);
        return ResponseEntity.ok(Response.success("Updated preparation flag"));
    }

    @GetMapping("/activities/{activityId}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getPreparationDashboard(@PathVariable Long activityId) {
        PreparationDashboardDto dashboard = preparationService.getPreparationDashboard(activityId);
        return ResponseEntity.ok(Response.success("OK", dashboard));
    }

    @GetMapping("/my/activity-ids")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Response> listMyPreparationActivityIds(Authentication authentication) {
        return ResponseEntity
                .ok(Response.success("OK", preparationService.listMyPreparationActivityIds(authentication.getName())));
    }

    @GetMapping("/activities/{activityId}/organizers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> listOrganizers(@PathVariable Long activityId) {
        return ResponseEntity.ok(Response.success("OK", preparationService.listOrganizers(activityId)));
    }

    @PostMapping("/activities/{activityId}/organizers/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> addOrganizer(@PathVariable Long activityId, @PathVariable Long studentId) {
        preparationService.addOrganizer(activityId, studentId);
        return ResponseEntity.ok(Response.success("Added organizer"));
    }

    @DeleteMapping("/activities/{activityId}/organizers/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> removeOrganizer(@PathVariable Long activityId, @PathVariable Long studentId) {
        preparationService.removeOrganizer(activityId, studentId);
        return ResponseEntity.ok(Response.success("Removed organizer"));
    }

    @PostMapping("/activities/{activityId}/tasks")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> assignTask(@PathVariable Long activityId,
            @RequestBody @Valid CreatePreparationTaskRequest req) {
        PreparationTaskDto dto = preparationService.assignTask(new CreatePreparationTaskRequest(
                activityId,
                req.getOwnerId(),
                req.getTitle(),
                req.getDescription(),
                req.getDeadline(),
                req.getIsFinancial()));
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/status")
    @PreAuthorize("@preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> updateMyTaskStatus(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdatePreparationTaskStatusRequest req,
            Authentication authentication) {
        PreparationTaskDto dto = preparationService.updateMyTaskStatus(taskId, req.getStatus(),
                authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/tasks/{taskId}/members")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> listTaskMembers(@PathVariable Long taskId) {
        List<PreparationTaskMemberDto> members = preparationService.listTaskMembers(taskId);
        return ResponseEntity.ok(Response.success("OK", members));
    }

    @DeleteMapping("/tasks/{taskId}/members/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> removeTaskMember(@PathVariable Long taskId, @PathVariable Long studentId) {
        preparationService.removeTaskMember(taskId, studentId);
        return ResponseEntity.ok(Response.success("OK"));
    }

    @PostMapping("/tasks/{taskId}/leaders/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> promoteLeader(@PathVariable Long taskId, @PathVariable Long studentId) {
        preparationService.promoteTaskLeader(taskId, studentId);
        return ResponseEntity.ok(Response.success("OK"));
    }

    @DeleteMapping("/tasks/{taskId}/leaders/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> demoteLeader(@PathVariable Long taskId, @PathVariable Long studentId) {
        preparationService.demoteTaskLeader(taskId, studentId);
        return ResponseEntity.ok(Response.success("OK"));
    }

    @PutMapping("/tasks/{taskId}/accept")
    @PreAuthorize("@preparationSecurity.isTaskMember(#taskId, authentication)")
    public ResponseEntity<Response> acceptTask(@PathVariable Long taskId, Authentication authentication) {
        PreparationTaskDto dto = preparationService.acceptTask(taskId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/request-complete")
    @PreAuthorize("@preparationSecurity.isAssignee(#taskId, authentication)")
    public ResponseEntity<Response> requestComplete(@PathVariable Long taskId, Authentication authentication) {
        PreparationTaskDto dto = preparationService.requestCompleteTask(taskId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @PutMapping("/tasks/{taskId}/complete-decision")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Response> completeDecision(
            @PathVariable Long taskId,
            @RequestBody @Valid ApproveTaskCompletionRequest request) {
        PreparationTaskDto dto = preparationService.adminCompleteDecision(taskId,
                Boolean.TRUE.equals(request.getApproved()));
        return ResponseEntity.ok(Response.success("OK", dto));
    }

    @GetMapping("/activities/{activityId}/workload-warnings")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<Response> getWorkloadWarnings(@PathVariable Long activityId) {
        List<WorkloadWarningDto> warnings = preparationService.getWorkloadWarnings(activityId);
        return ResponseEntity.ok(Response.success("OK", warnings));
    }

    // new
    @GetMapping("/stats/{id}")
    public ResponseEntity<TaskStatsRespone> getStats(@PathVariable Long id) {
        TaskStatsRespone stats = preparationService.getStudentStats(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Response> getTaskDetail(@PathVariable("id") Long id) {
        PreparationTaskDto taskDto = preparationService.getTaskDetail(id);
        return ResponseEntity.ok(Response.success("Lấy chi tiết thành công", taskDto));
    }

    @GetMapping("/my/activities/tasks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Response> getMyTasks(
            @RequestParam Long activityId,
            Authentication authentication) {
        List<MyPreparationTaskDto> tasks = preparationService.getPreparationTasks(activityId, authentication.getName());
        return ResponseEntity.ok(Response.success("OK", tasks));
    }
}
