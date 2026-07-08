package vn.campuslife.controller.preparation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.campuslife.security.department.DepartmentRequestScope;
import vn.campuslife.security.department.DepartmentScope;
import vn.campuslife.security.department.DepartmentScopeRouting;
import vn.campuslife.service.PreparationExportService;

@RestController
@RequestMapping("/api/preparation/activities/{activityId}/exports")
@RequiredArgsConstructor
public class PreparationExportController {
    private final PreparationExportService exportService;
    private final DepartmentScopeRouting departmentScopeRouting;

    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<byte[]> exportFinancial(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "xlsx") String format,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        PreparationExportService.ExportFile file = departmentScopeRouting.useManagerScopedPath(scope)
                ? exportService.exportFinancial(activityId, format, scope)
                : exportService.exportFinancial(activityId, format);
        return fileResponse(file);
    }

    @GetMapping("/operational")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<byte[]> exportOperational(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "xlsx") String format,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        PreparationExportService.ExportFile file = departmentScopeRouting.useManagerScopedPath(scope)
                ? exportService.exportOperational(activityId, format, scope)
                : exportService.exportOperational(activityId, format);
        return fileResponse(file);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or @preparationSecurity.isActivityPrepSupervisor(#activityId, authentication) or @preparationSecurity.isOrganizer(#activityId, authentication)")
    public ResponseEntity<byte[]> exportAudit(
            @PathVariable Long activityId,
            @RequestParam(defaultValue = "xlsx") String format,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DepartmentScope scope = currentScope(httpRequest);
        PreparationExportService.ExportFile file = departmentScopeRouting.useManagerScopedPath(scope)
                ? exportService.exportAudit(activityId, format, scope)
                : exportService.exportAudit(activityId, format);
        return fileResponse(file);
    }

    private ResponseEntity<byte[]> fileResponse(PreparationExportService.ExportFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"");
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        return ResponseEntity.ok().headers(headers).body(file.bytes());
    }

    private DepartmentScope currentScope(HttpServletRequest request) {
        return DepartmentRequestScope.get(request).orElse(null);
    }
}
