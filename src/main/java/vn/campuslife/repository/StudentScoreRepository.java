package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreType;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {

        @Query("SELECT ss FROM StudentScore ss WHERE ss.student.id = :studentId AND ss.semester.id = :semesterId")
        List<StudentScore> findByStudentAndSemester(@Param("studentId") Long studentId,
                        @Param("semesterId") Long semesterId);

        Optional<StudentScore> findByStudentIdAndSemesterIdAndScoreType(Long studentId, Long semesterId,
                        ScoreType scoreType);

        @Query("SELECT ss FROM StudentScore ss WHERE ss.student.id = :studentId AND ss.scoreType = :scoreType")
        List<StudentScore> findByStudentIdAndScoreType(@Param("studentId") Long studentId,
                        @Param("scoreType") ScoreType scoreType);
}
