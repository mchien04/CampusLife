package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivitySeries;

@Repository
public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, Long> {

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
}

