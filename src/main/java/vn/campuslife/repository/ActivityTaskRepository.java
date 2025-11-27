package vn.campuslife.repository;

import vn.campuslife.entity.ActivityTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityTaskRepository extends JpaRepository<ActivityTask, Long> {

    /**
     * Lấy danh sách nhiệm vụ theo activity ID (chỉ lấy activity chưa bị xóa)
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.activity.id = :activityId AND at.activity.isDeleted = false")
    List<ActivityTask> findByActivityIdAndActivityIsDeletedFalse(@Param("activityId") Long activityId);

    /**
     * Lấy danh sách nhiệm vụ theo activity ID, sắp xếp theo ngày tạo
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.activity.id = :activityId AND at.activity.isDeleted = false ORDER BY at.createdAt ASC")
    List<ActivityTask> findByActivityIdOrderByCreatedAtAsc(@Param("activityId") Long activityId);

    /**
     * Lấy nhiệm vụ theo ID (chỉ lấy nếu activity chưa bị xóa)
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.id = :id AND at.activity.isDeleted = false")
    Optional<ActivityTask> findByIdAndActivityIsDeletedFalse(@Param("id") Long id);

    /**
     * Lấy danh sách nhiệm vụ có deadline trong khoảng thời gian
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.deadline BETWEEN :start AND :end AND at.activity.isDeleted = false")
    List<ActivityTask> findByDeadlineBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Lấy danh sách nhiệm vụ có deadline trước thời điểm chỉ định
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.deadline < :date AND at.activity.isDeleted = false")
    List<ActivityTask> findByDeadlineBeforeAndActivityIsDeletedFalse(@Param("date") LocalDateTime date);

    /**
     * Lấy danh sách nhiệm vụ có deadline sắp tới (trong 7 ngày tới)
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.deadline BETWEEN :today AND :nextWeek AND at.activity.isDeleted = false")
    List<ActivityTask> findUpcomingDeadlines(@Param("today") LocalDateTime today,
            @Param("nextWeek") LocalDateTime nextWeek);

    /**
     * Đếm số nhiệm vụ của một activity
     */
    @Query("SELECT COUNT(at) FROM ActivityTask at WHERE at.activity.id = :activityId AND at.activity.isDeleted = false")
    Long countByActivityId(@Param("activityId") Long activityId);

    /**
     * Lấy nhiệm vụ theo ID và activity ID (chỉ lấy nếu activity chưa bị xóa)
     */
    @Query("SELECT at FROM ActivityTask at WHERE at.id = :taskId AND at.activity.id = :activityId AND at.activity.isDeleted = false")
    Optional<ActivityTask> findByIdAndActivityIdAndActivityIsDeletedFalse(@Param("taskId") Long taskId,
            @Param("activityId") Long activityId);
}
