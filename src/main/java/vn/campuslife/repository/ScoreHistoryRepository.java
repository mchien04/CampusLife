package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ScoreHistory;
import vn.campuslife.enumeration.ScoreType;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long> {
    
    @Query("SELECT sh FROM ScoreHistory sh " +
           "WHERE sh.score.student.id = :studentId " +
           "AND sh.score.semester.id = :semesterId " +
           "AND sh.score.scoreType = :scoreType " +
           "ORDER BY sh.changeDate DESC")
    Page<ScoreHistory> findByScore_StudentIdAndScore_SemesterIdAndScore_ScoreType(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            @Param("scoreType") ScoreType scoreType,
            Pageable pageable);
    
    @Query("SELECT sh FROM ScoreHistory sh " +
           "WHERE sh.score.student.id = :studentId " +
           "AND sh.score.semester.id = :semesterId " +
           "ORDER BY sh.changeDate DESC")
    Page<ScoreHistory> findByScore_StudentIdAndScore_SemesterId(
            @Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId,
            Pageable pageable);
}
