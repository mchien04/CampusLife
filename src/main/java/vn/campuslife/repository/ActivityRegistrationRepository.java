package vn.campuslife.repository;

import vn.campuslife.entity.ActivityParticipation;
import vn.campuslife.entity.ActivityRegistration;
import vn.campuslife.entity.Student;
import vn.campuslife.enumeration.ParticipationType;
import vn.campuslife.enumeration.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {

    /**
     * Lấy danh sách đăng ký theo student ID
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.student.id = :studentId AND ar.student.isDeleted = false")
    List<ActivityRegistration> findByStudentIdAndStudentIsDeletedFalse(@Param("studentId") Long studentId);

    /**
     * Lấy danh sách đăng ký theo activity ID
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.activity.id = :activityId AND ar.activity.isDeleted = false")
    List<ActivityRegistration> findByActivityIdAndActivityIsDeletedFalse(@Param("activityId") Long activityId);

    /**
     * Lấy ds đăng ký theo activity ID và student ID
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.activity.id = :activityId AND ar.student.id = :studentId")
    Optional<ActivityRegistration> findByActivityIdAndStudentId(@Param("activityId") Long activityId,
                                                                @Param("studentId") Long studentId);

    /**
     * Lấy danh sách đăng ký theo status
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.status = :status AND ar.activity.isDeleted = false")
    List<ActivityRegistration> findByStatusAndActivityIsDeletedFalse(@Param("status") RegistrationStatus status);

    /**
     * Lấy danh sách đăng ký theo activity ID và status
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.activity.id = :activityId AND ar.status = :status")
    List<ActivityRegistration> findByActivityIdAndStatus(@Param("activityId") Long activityId,
                                                         @Param("status") RegistrationStatus status);

    /**
     * Kiểm tra xem student đã đăng ký activity này chưa
     */
    @Query("SELECT COUNT(ar) > 0 FROM ActivityRegistration ar WHERE ar.activity.id = :activityId AND ar.student.id = :studentId")
    boolean existsByActivityIdAndStudentId(@Param("activityId") Long activityId,
                                           @Param("studentId") Long studentId);

    /**
     * Đếm số đăng ký theo activity ID
     */
    @Query("SELECT COUNT(ar) FROM ActivityRegistration ar WHERE ar.activity.id = :activityId")
    Long countByActivityId(@Param("activityId") Long activityId);

    /**
     * Đếm số đăng ký theo activity ID và status
     */
    @Query("SELECT COUNT(ar) FROM ActivityRegistration ar WHERE ar.activity.id = :activityId AND ar.status = :status")
    Long countByActivityIdAndStatus(@Param("activityId") Long activityId,
                                    @Param("status") RegistrationStatus status);

    /**
     * Lấy danh sách đăng ký trong khoảng thời gian
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.registeredDate BETWEEN :startDate AND :endDate AND ar.activity.isDeleted = false")
    List<ActivityRegistration> findByRegisteredDateBetween(@Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy danh sách đăng ký sắp tới (trong 7 ngày tới)
     */
    @Query("SELECT ar FROM ActivityRegistration ar WHERE ar.activity.startDate BETWEEN :today AND :nextWeek AND ar.status = 'APPROVED' AND ar.activity.isDeleted = false")
    List<ActivityRegistration> findUpcomingRegistrations(@Param("today") LocalDateTime today,
                                                         @Param("nextWeek") LocalDateTime nextWeek);

    /**
     * Kiểm tra mã vé có tồn tại chưa
     */
    boolean existsByTicketCode(String ticketCode);

    /**
     * Tìm bản đăng ký theo studentId và status
     */
    Optional<ActivityRegistration> findByStudentIdAndStatus(Long studentId, RegistrationStatus status);

    /**
     * Tìm bản đăng ký theo ticketCode
     */
    Optional<ActivityRegistration> findByTicketCode(String ticketCode);
    /**
     * Lấy danh sách đăng ký theo status của 1 sinh viên
     */
    @Query("""
       SELECT ar 
       FROM ActivityRegistration ar 
       WHERE ar.student.id = :studentId 
         AND ar.status = :status 
         AND ar.activity.isDeleted = false
       """)
    List<ActivityRegistration> findListByStudentIdAndStatus(
            @Param("studentId") Long studentId,
            @Param("status") RegistrationStatus status
    );



    /**
     * Tìm tất cả registration đã APPROVED nhưng chưa có participation
     */
    @Query("SELECT ar FROM ActivityRegistration ar " +
            "WHERE ar.status = 'APPROVED' " +
            "AND ar.activity.isDeleted = false " +
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM ActivityParticipation ap WHERE ap.registration.id = ar.id" +
            ")")
    List<ActivityRegistration> findApprovedRegistrationsWithoutParticipation();

}
