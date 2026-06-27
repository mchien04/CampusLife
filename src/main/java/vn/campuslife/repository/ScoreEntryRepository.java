package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ScoreEntry;
import vn.campuslife.enumeration.ScoreEntrySourceType;
import vn.campuslife.enumeration.ScoreEntryStatus;
import vn.campuslife.enumeration.ScoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreEntryRepository extends JpaRepository<ScoreEntry, Long> {
    
    Optional<ScoreEntry> findByStudentIdAndSourceTypeAndSourceIdAndRuleIdAndStatus(
            Long studentId, ScoreEntrySourceType sourceType, Long sourceId, Long ruleId, ScoreEntryStatus status);

    List<ScoreEntry> findBySourceTypeAndSourceIdAndStatus(
            ScoreEntrySourceType sourceType, Long sourceId, ScoreEntryStatus status);

    @Query("SELECT SUM(e.points) FROM ScoreEntry e WHERE e.student.id = :studentId AND e.semester.id = :semesterId AND e.scoreType = :scoreType AND e.status = :status")
    BigDecimal sumPointsByStudentAndSemesterAndScoreTypeAndStatus(
            @Param("studentId") Long studentId, 
            @Param("semesterId") Long semesterId, 
            @Param("scoreType") ScoreType scoreType, 
            @Param("status") ScoreEntryStatus status);

    List<ScoreEntry> findByStudentIdAndSemesterIdAndStatusOrderByCreatedAtAsc(
            Long studentId, Long semesterId, ScoreEntryStatus status);

    List<ScoreEntry> findByStudentIdAndSemesterIdAndScoreTypeAndStatusOrderByCreatedAtAsc(
            Long studentId, Long semesterId, ScoreType scoreType, ScoreEntryStatus status);

    // Paginated queries for score history (DESC order for newest first)
    @Query("SELECT se FROM ScoreEntry se LEFT JOIN FETCH se.activity " +
           "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.status = :status ORDER BY se.createdAt DESC, se.id DESC")
    org.springframework.data.domain.Page<ScoreEntry> findWithActivityByStudentAndSemester(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT se FROM ScoreEntry se LEFT JOIN FETCH se.activity " +
           "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.scoreType = :scoreType AND se.status = :status ORDER BY se.createdAt DESC, se.id DESC")
    org.springframework.data.domain.Page<ScoreEntry> findWithActivityByStudentAndSemesterAndScoreType(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("scoreType") ScoreType scoreType,
            @Param("status") ScoreEntryStatus status,
            org.springframework.data.domain.Pageable pageable);

    // Aggregate sum for running total offset computation (Option A)
    @Query("SELECT COALESCE(SUM(se.points), 0) FROM ScoreEntry se " +
           "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.status = :status " +
           "AND (se.createdAt < :cutoff OR (se.createdAt = :cutoff AND se.id < :cutoffId))")
    BigDecimal sumPointsBeforeCutoff(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("cutoffId") Long cutoffId);

    @Query("SELECT COALESCE(SUM(se.points), 0) FROM ScoreEntry se " +
           "WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.scoreType = :scoreType AND se.status = :status " +
           "AND (se.createdAt < :cutoff OR (se.createdAt = :cutoff AND se.id < :cutoffId))")
    BigDecimal sumPointsBeforeCutoffWithScoreType(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("scoreType") ScoreType scoreType,
            @Param("status") ScoreEntryStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("cutoffId") Long cutoffId);

    // Filtering queries for score management (Task 7a)
    @Query("SELECT se FROM ScoreEntry se WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.status = :status AND se.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY se.createdAt DESC")
    List<ScoreEntry> findByStudentAndSemesterAndDateRange(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT se FROM ScoreEntry se WHERE se.student.id = :studentId AND se.semester.id = :semesterId " +
           "AND se.status = :status AND LOWER(se.reason) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY se.createdAt DESC")
    List<ScoreEntry> findByStudentAndSemesterAndReasonKeyword(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status,
            @Param("keyword") String keyword);

    // Statistics source-type breakdown queries
    @Query("SELECT se.sourceType, SUM(se.points) FROM ScoreEntry se " +
           "WHERE se.semester.id = :semesterId AND se.status = :status " +
           "GROUP BY se.sourceType")
    List<Object[]> sumPointsBySourceType(
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status);

    @Query("SELECT se.sourceType, SUM(se.points) FROM ScoreEntry se " +
           "WHERE se.student.id = :studentId AND se.semester.id = :semesterId AND se.status = :status " +
           "GROUP BY se.sourceType")
    List<Object[]> sumPointsBySourceTypeForStudent(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("status") ScoreEntryStatus status);
}
