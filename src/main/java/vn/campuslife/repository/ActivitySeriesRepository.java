package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivitySeries;

import java.util.List;
import java.util.Set;

@Repository
public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, Long>, JpaSpecificationExecutor<ActivitySeries> {

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
            WHERE s.id = :seriesId
              AND s.isDeleted = false
              AND (
                  td.id IN :deptIds
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

