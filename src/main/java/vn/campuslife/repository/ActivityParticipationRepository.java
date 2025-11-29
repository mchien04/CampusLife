package vn.campuslife.repository;

import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.ScoreType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityParticipationRepository extends JpaRepository<ActivityParticipation, Long> {

        // Kiểm tra đã tồn tại participation cho registration
        boolean existsByRegistration(ActivityRegistration registration);

        // Lấy participation theo registration
        Optional<ActivityParticipation> findByRegistration(ActivityRegistration registration);

        // Lấy tất cả participation theo activityId và trạng thái
        @Query("SELECT ap FROM ActivityParticipation ap " +
                        "WHERE ap.registration.activity.id = :activityId " +
                        "AND ap.participationType = :type")
        List<ActivityParticipation> findByActivityIdAndParticipationType(
                        @Param("activityId") Long activityId,
                        @Param("type") ParticipationType type);

        // Lấy tất cả participation theo studentId, semesterId và scoreType
        @Query("SELECT ap FROM ActivityParticipation ap " +
                        "WHERE ap.registration.student.id = :studentId " +
                        "AND ap.registration.activity.scoreType = :scoreType")
        List<ActivityParticipation> findByStudentIdAndScoreType(
                        @Param("studentId") Long studentId,
                        @Param("scoreType") ScoreType scoreType);

        // Lấy tất cả participation theo activityId
        @Query("SELECT ap FROM ActivityParticipation ap " +
                        "WHERE ap.registration.activity.id = :activityId " +
                        "AND ap.registration.activity.isDeleted = false")
        List<ActivityParticipation> findByActivityId(@Param("activityId") Long activityId);

        /**
         * Đếm số participation theo activity
         */
        @Query("SELECT COUNT(ap) FROM ActivityParticipation ap " +
                "WHERE ap.registration.activity.id = :activityId " +
                "AND ap.registration.activity.isDeleted = false")
        Long countByActivityId(@Param("activityId") Long activityId);

        /**
         * Đếm số participation theo activity và participation type
         */
        @Query("SELECT COUNT(ap) FROM ActivityParticipation ap " +
                "WHERE ap.registration.activity.id = :activityId " +
                "AND ap.participationType = :type " +
                "AND ap.registration.activity.isDeleted = false")
        Long countByActivityIdAndParticipationType(@Param("activityId") Long activityId, @Param("type") ParticipationType type);

        /**
         * Đếm số participation theo student
         */
        @Query("SELECT COUNT(ap) FROM ActivityParticipation ap " +
                "WHERE ap.registration.student.id = :studentId " +
                "AND ap.registration.activity.isDeleted = false")
        Long countByStudentId(@Param("studentId") Long studentId);

        /**
         * Đếm tổng số participation trong khoảng thời gian
         */
        @Query("SELECT COUNT(ap) FROM ActivityParticipation ap " +
                "WHERE ap.date >= :startDate AND ap.date <= :endDate " +
                "AND ap.registration.activity.isDeleted = false")
        Long countByDateRange(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

        /**
         * Top students có nhiều participation nhất
         */
        @Query("SELECT ap.registration.student.id, COUNT(ap) as partCount FROM ActivityParticipation ap " +
                "WHERE ap.registration.activity.isDeleted = false " +
                "AND ap.registration.student.isDeleted = false " +
                "GROUP BY ap.registration.student.id " +
                "ORDER BY partCount DESC")
        List<Object[]> findTopStudentsByParticipations(org.springframework.data.domain.Pageable pageable);

}
