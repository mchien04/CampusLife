package vn.campuslife.service;

import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.security.department.DepartmentScope;

/**
 * Xuất file danh sách điểm sinh viên theo học kỳ, lọc khoa / lớp.
 */
public interface ScoreExportService {

    ExportFile exportSemesterScores(Long semesterId, Long departmentId, Long classId, ScoreType scoreType);

    ExportFile exportSemesterScores(Long semesterId, Long departmentId, Long classId, ScoreType scoreType,
                                    DepartmentScope scope);

    record ExportFile(String filename, String contentType, byte[] bytes) {
    }
}
