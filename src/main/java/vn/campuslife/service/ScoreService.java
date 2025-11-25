package vn.campuslife.service;

import vn.campuslife.enumeration.ScoreType;
import vn.campuslife.model.Response;

import java.util.List;

public interface ScoreService {
    Response calculateTrainingScore(Long studentId, Long semesterId, List<Long> excludedCriterionIds,
            Long enteredByUserId);

    Response viewScores(Long studentId, Long semesterId);

    Response getTotalScore(Long studentId, Long semesterId);

    /**
     * Lấy bảng xếp hạng điểm sinh viên
     * @param semesterId ID học kỳ (required)
     * @param scoreType Loại điểm (optional - null = tổng điểm tất cả loại)
     * @param departmentId ID khoa (optional - null = tất cả khoa)
     * @param classId ID lớp (optional - null = tất cả lớp)
     * @param sortOrder Thứ tự sắp xếp: "ASC" hoặc "DESC" (mặc định DESC)
     * @return Danh sách xếp hạng với rank, student info và score
     */
    Response getStudentRanking(Long semesterId, ScoreType scoreType, Long departmentId, Long classId, String sortOrder);
}
