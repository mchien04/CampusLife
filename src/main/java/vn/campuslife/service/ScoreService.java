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
     * 
     * @param semesterId   ID học kỳ (required)
     * @param scoreType    Loại điểm (optional - null = tổng điểm tất cả loại)
     * @param departmentId ID khoa (optional - null = tất cả khoa)
     * @param classId      ID lớp (optional - null = tất cả lớp)
     * @param sortOrder    Thứ tự sắp xếp: "ASC" hoặc "DESC" (mặc định DESC)
     * @return Danh sách xếp hạng với rank, student info và score
     */
    Response getStudentRanking(Long semesterId, ScoreType scoreType, Long departmentId, Long classId, String sortOrder);

    /**
     * Rà soát và tính lại điểm cho một student
     * Bao gồm: điểm từ ActivityParticipation (minigame, activity thường) và
     * milestone points từ series
     * 
     * @param studentId  ID sinh viên
     * @param semesterId ID học kỳ (null = học kỳ hiện tại)
     * @return Kết quả rà soát và cập nhật
     */
    Response recalculateStudentScore(Long studentId, Long semesterId);

    /**
     * Rà soát và tính lại điểm cho tất cả students
     * 
     * @param semesterId ID học kỳ (null = học kỳ hiện tại)
     * @return Kết quả rà soát và cập nhật
     */
    Response recalculateAllStudentScores(Long semesterId);

    /**
     * Xem lịch sử điểm của student
     * Bao gồm: ScoreHistory (tổng hợp) và ActivityParticipation (chi tiết)
     * 
     * @param studentId  ID sinh viên
     * @param semesterId ID học kỳ (required)
     * @param scoreType  Loại điểm (optional - null = tất cả loại)
     * @param page       Số trang (default 0)
     * @param size       Số bản ghi mỗi trang (default 20)
     * @param requestingStudentId ID của student đang request (null nếu là Admin/Manager)
     * @return Lịch sử điểm với thông tin nguồn (activity/series)
     */
    Response getScoreHistory(Long studentId, Long semesterId, ScoreType scoreType, Integer page, Integer size, Long requestingStudentId);
}
