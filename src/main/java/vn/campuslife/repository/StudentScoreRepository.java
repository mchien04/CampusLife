package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.StudentScore;
import vn.campuslife.enumeration.ScoreSourceType;

import java.util.List;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {

    @Query("SELECT ss FROM StudentScore ss WHERE ss.student.id = :studentId AND ss.semester.id = :semesterId")
    List<StudentScore> findByStudentAndSemester(@Param("studentId") Long studentId,
            @Param("semesterId") Long semesterId);

    boolean existsByStudentIdAndActivityIdAndScoreSourceType(Long studentId, Long activityId,
            ScoreSourceType scoreSourceType);

    boolean existsByStudentIdAndSubmissionIdAndScoreSourceType(Long studentId, Long submissionId,
            ScoreSourceType scoreSourceType);
}
