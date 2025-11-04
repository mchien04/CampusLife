package vn.campuslife.repository;

import vn.campuslife.entity.TaskAssignment;
import vn.campuslife.enumeration.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {

    /**
     * Lấy danh sách phân công theo student ID (chỉ lấy task của activity chưa bị
     * xóa)
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.student.id = :studentId AND ta.task.activity.isDeleted = false")
    List<TaskAssignment> findByStudentIdAndTaskActivityIsDeletedFalse(@Param("studentId") Long studentId);

    /**
     * Lấy danh sách phân công theo task ID
     */
    List<TaskAssignment> findByTaskId(Long taskId);

    /**
     * Lấy danh sách phân công theo student ID và status
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.student.id = :studentId AND ta.status = :status AND ta.task.activity.isDeleted = false")
    List<TaskAssignment> findByStudentIdAndStatus(@Param("studentId") Long studentId,
            @Param("status") TaskStatus status);

    /**
     * Lấy phân công theo task ID và student ID
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.id = :taskId AND ta.student.id = :studentId")
    Optional<TaskAssignment> findByTaskIdAndStudentId(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    /**
     * Lấy danh sách phân công theo status (chỉ lấy task của activity chưa bị xóa)
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.status = :status AND ta.task.activity.isDeleted = false")
    List<TaskAssignment> findByStatusAndTaskActivityIsDeletedFalse(@Param("status") TaskStatus status);

    /**
     * Lấy danh sách phân công theo activity ID
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.activity.id = :activityId")
    List<TaskAssignment> findByActivityId(@Param("activityId") Long activityId);

    /**
     * Lấy danh sách phân công theo activity ID và student ID
     */
    @Query("SELECT ta FROM TaskAssignment ta WHERE ta.task.activity.id = :activityId AND ta.student.id = :studentId")
    List<TaskAssignment> findByActivityIdAndStudentId(@Param("activityId") Long activityId,
            @Param("studentId") Long studentId);

    /**
     * Đếm số phân công theo task ID
     */
    Long countByTaskId(Long taskId);

    /**
     * Đếm số phân công theo task ID và status
     */
    Long countByTaskIdAndStatus(Long taskId, TaskStatus status);

    /**
     * Kiểm tra xem sinh viên có được phân công hợp lệ cho task không
     */
    @Query("""
        SELECT CASE WHEN COUNT(ta) > 0 THEN true ELSE false END
        FROM TaskAssignment ta
        WHERE ta.task.id = :taskId
          AND ta.student.id = :studentId
          AND ta.status = 'PENDING'
    """)
    boolean existsActiveAssignment(@Param("taskId") Long taskId, @Param("studentId") Long studentId);
}
