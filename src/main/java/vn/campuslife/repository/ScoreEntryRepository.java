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
}
