package vn.campuslife.service;

import vn.campuslife.security.department.DepartmentScope;

public interface PreparationExportService {
    ExportFile exportFinancial(Long activityId, String format);

    ExportFile exportFinancial(Long activityId, String format, DepartmentScope scope);

    ExportFile exportOperational(Long activityId, String format);

    ExportFile exportOperational(Long activityId, String format, DepartmentScope scope);

    ExportFile exportAudit(Long activityId, String format);

    ExportFile exportAudit(Long activityId, String format, DepartmentScope scope);

    record ExportFile(String filename, String contentType, byte[] bytes) {
    }
}
