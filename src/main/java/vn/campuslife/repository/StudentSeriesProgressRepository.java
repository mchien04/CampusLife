package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.StudentSeriesProgress;

import java.util.Optional;

@Repository
public interface StudentSeriesProgressRepository extends JpaRepository<StudentSeriesProgress, Long> {

    /**
     * Tìm progress theo student và series
     */
    @Query("SELECT ssp FROM StudentSeriesProgress ssp " +
            "WHERE ssp.student.id = :studentId AND ssp.series.id = :seriesId")
    Optional<StudentSeriesProgress> findByStudentIdAndSeriesId(
            @Param("studentId") Long studentId,
            @Param("seriesId") Long seriesId);

    /**
     * Đếm số sinh viên đã hoàn thành series (completedCount = total activities)
     * Sinh viên được coi là hoàn thành khi completedCount >= tổng số activities trong series
     */
    @Query("SELECT COUNT(ssp) FROM StudentSeriesProgress ssp " +
            "WHERE ssp.series.id = :seriesId " +
            "AND ssp.student.isDeleted = false " +
            "AND ssp.completedCount >= (SELECT COUNT(a) FROM Activity a WHERE a.seriesId = :seriesId AND a.isDeleted = false)")
    Long countCompletedStudentsBySeriesId(@Param("seriesId") Long seriesId);

    /**
     * Lấy tất cả progress của series với pagination
     */
    @Query("SELECT ssp FROM StudentSeriesProgress ssp " +
            "WHERE ssp.series.id = :seriesId " +
            "AND ssp.student.isDeleted = false")
    Page<StudentSeriesProgress> findBySeriesId(@Param("seriesId") Long seriesId, Pageable pageable);

    /**
     * Tìm progress theo series và student name/code (cho search)
     */
    @Query("SELECT ssp FROM StudentSeriesProgress ssp " +
            "WHERE ssp.series.id = :seriesId " +
            "AND ssp.student.isDeleted = false " +
            "AND (LOWER(ssp.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(ssp.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<StudentSeriesProgress> findBySeriesIdAndStudentNameOrCode(
            @Param("seriesId") Long seriesId,
            @Param("keyword") String keyword,
            Pageable pageable);
}

