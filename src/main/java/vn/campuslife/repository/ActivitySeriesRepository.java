package vn.campuslife.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivitySeries;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, Long>, JpaSpecificationExecutor<ActivitySeries> {

    /**
     * Pessimistic lock on series row (SELECT ... FOR UPDATE) to serialize capacity checks.
     * Must be called inside an active transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ActivitySeries s WHERE s.id = :id AND s.isDeleted = false")
    Optional<ActivitySeries> findByIdAndIsDeletedFalseForUpdate(@Param("id") Long id);

    /**
     * Đếm tổng số series
     */
    @Query("SELECT COUNT(s) FROM ActivitySeries s WHERE s.isDeleted = false")
    Long countAllActive();

    /**
     * Đếm số sinh viên đăng ký series
     */
    @Query("SELECT COUNT(DISTINCT ar.student.id) FROM ActivityRegistration ar " +
            "WHERE ar.activity.seriesId = :seriesId " +
            "AND ar.student.isDeleted = false")
    Long countStudentsBySeriesId(@Param("seriesId") Long seriesId);

    List<ActivitySeries> findByIsDeletedFalse();

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM ActivitySeries s
            LEFT JOIN s.targetDepartments td
            LEFT JOIN s.organizers so
            WHERE s.id = :seriesId
              AND s.isDeleted = false
              AND (
                  td.id IN :deptIds
                  OR so.id IN :deptIds
                  OR EXISTS (
                      SELECT 1 FROM Activity a
                      JOIN a.organizers o
                      WHERE a.seriesId = s.id
                        AND a.isDeleted = false
                        AND o.id IN :deptIds
                  )
              )
            """)
    boolean existsActiveByIdAndDepartmentScope(@Param("seriesId") Long seriesId, @Param("deptIds") Set<Long> deptIds);

}

