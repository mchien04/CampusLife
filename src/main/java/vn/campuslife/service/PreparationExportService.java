package vn.campuslife.service;

public interface PreparationExportService {
    ExportFile exportFinancial(Long activityId, String format);

    ExportFile exportOperational(Long activityId, String format);

    ExportFile exportAudit(Long activityId, String format);

    record ExportFile(String filename, String contentType, byte[] bytes) {
    }
}
