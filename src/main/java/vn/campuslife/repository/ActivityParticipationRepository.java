package vn.campuslife.repository;

import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.enumeration.ParticipationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityParticipationRepository extends JpaRepository<ActivityParticipation, Long> {

    /**
     * Lấy danh sách tham gia theo student ID (chỉ lấy activity chưa bị xóa)
     */
    @Query("SELECT ap FROM ActivityParticipation ap WHERE ap.student.id = :studentId AND ap.activity.isDeleted = false")
    List<ActivityParticipation> findByStudentIdAndActivityIsDeletedFalse(@Param("studentId") Long studentId);

    /**
     * Lấy danh sách tham gia theo activity ID
     */
    @Query("SELECT ap FROM ActivityParticipation ap WHERE ap.activity.id = :activityId AND ap.activity.isDeleted = false")
    List<ActivityParticipation> findByActivityIdAndActivityIsDeletedFalse(@Param("activityId") Long activityId);

    /**
     * Lấy danh sách tham gia theo student ID và participation type
     */
    @Query("SELECT ap FROM ActivityParticipation ap WHERE ap.student.id = :studentId AND ap.participationType = :type AND ap.activity.isDeleted = false")
    List<ActivityParticipation> findByStudentIdAndParticipationType(@Param("studentId") Long studentId,
            @Param("type") ParticipationType type);

    /**
     * Lấy danh sách tham gia theo activity ID và participation type
     */
    @Query("SELECT ap FROM ActivityParticipation ap WHERE ap.activity.id = :activityId AND ap.participationType = :type")
    List<ActivityParticipation> findByActivityIdAndParticipationType(@Param("activityId") Long activityId,
            @Param("type") ParticipationType type);

    /**
     * Đếm số tham gia theo activity ID
     */
    @Query("SELECT COUNT(ap) FROM ActivityParticipation ap WHERE ap.activity.id = :activityId")
    Long countByActivityId(@Param("activityId") Long activityId);

    /**
     * Đếm số tham gia theo activity ID và participation type
     */
    @Query("SELECT COUNT(ap) FROM ActivityParticipation ap WHERE ap.activity.id = :activityId AND ap.participationType = :type")
    Long countByActivityIdAndParticipationType(@Param("activityId") Long activityId,
            @Param("type") ParticipationType type);

    /**
     * Lấy danh sách tham gia trong khoảng thời gian
     */
    @Query("SELECT ap FROM ActivityParticipation ap WHERE ap.date BETWEEN :startDate AND :endDate AND ap.activity.isDeleted = false")
    List<ActivityParticipation> findByDateBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
