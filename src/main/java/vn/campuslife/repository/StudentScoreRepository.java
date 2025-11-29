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

        // Lấy tất cả scores theo semester và scoreType, sắp xếp theo điểm
        @Query("SELECT ss FROM StudentScore ss " +
                        "WHERE ss.semester.id = :semesterId " +
                        "AND ss.scoreType = :scoreType " +
                        "AND ss.student.isDeleted = false " +
                        "ORDER BY ss.score DESC")
        List<StudentScore> findBySemesterIdAndScoreTypeOrderByScoreDesc(
                        @Param("semesterId") Long semesterId,
                        @Param("scoreType") ScoreType scoreType);

        /**
         * Tính điểm trung bình theo scoreType và semester
         */
        @Query("SELECT AVG(ss.score) FROM StudentScore ss " +
                "WHERE ss.semester.id = :semesterId " +
                "AND ss.scoreType = :scoreType " +
                "AND ss.student.isDeleted = false")
        java.math.BigDecimal calculateAverageBySemesterAndScoreType(
                @Param("semesterId") Long semesterId,
                @Param("scoreType") ScoreType scoreType);

        /**
         * Tính điểm trung bình theo scoreType và department
         */
        @Query("SELECT AVG(ss.score) FROM StudentScore ss " +
                "WHERE ss.scoreType = :scoreType " +
                "AND ss.student.department.id = :departmentId " +
                "AND ss.student.isDeleted = false")
        java.math.BigDecimal calculateAverageByDepartmentAndScoreType(
                @Param("departmentId") Long departmentId,
                @Param("scoreType") ScoreType scoreType);

        /**
         * Tính điểm trung bình theo scoreType và class
         */
        @Query("SELECT AVG(ss.score) FROM StudentScore ss " +
                "WHERE ss.scoreType = :scoreType " +
                "AND ss.student.studentClass.id = :classId " +
                "AND ss.student.isDeleted = false")
        java.math.BigDecimal calculateAverageByClassAndScoreType(
                @Param("classId") Long classId,
                @Param("scoreType") ScoreType scoreType);

        /**
         * Tính điểm trung bình theo scoreType và semester
         */
        @Query("SELECT AVG(ss.score) FROM StudentScore ss " +
                "WHERE ss.scoreType = :scoreType " +
                "AND ss.semester.id = :semesterId " +
                "AND ss.student.isDeleted = false")
        java.math.BigDecimal calculateAverageByScoreTypeAndSemester(
                @Param("scoreType") ScoreType scoreType,
                @Param("semesterId") Long semesterId);

        /**
         * Lấy điểm max, min theo scoreType và semester
         */
        @Query("SELECT MAX(ss.score), MIN(ss.score) FROM StudentScore ss " +
                "WHERE ss.scoreType = :scoreType " +
                "AND ss.semester.id = :semesterId " +
                "AND ss.student.isDeleted = false")
        Object[] findMaxMinByScoreTypeAndSemester(
                @Param("scoreType") ScoreType scoreType,
                @Param("semesterId") Long semesterId);

        // Lấy tất cả scores theo semester, sắp xếp theo điểm
        @Query("SELECT ss FROM StudentScore ss " +
                        "WHERE ss.semester.id = :semesterId " +
                        "AND ss.student.isDeleted = false " +
                        "ORDER BY ss.score DESC")
        List<StudentScore> findBySemesterIdOrderByScoreDesc(@Param("semesterId") Long semesterId);

        // Lấy tất cả scores theo semester, scoreType và department, sắp xếp theo điểm
        @Query("SELECT ss FROM StudentScore ss " +
                        "WHERE ss.semester.id = :semesterId " +
                        "AND ss.scoreType = :scoreType " +
                        "AND ss.student.department.id = :departmentId " +
                        "AND ss.student.isDeleted = false " +
                        "ORDER BY ss.score DESC")
        List<StudentScore> findBySemesterIdAndScoreTypeAndDepartmentIdOrderByScoreDesc(
                        @Param("semesterId") Long semesterId,
                        @Param("scoreType") ScoreType scoreType,
                        @Param("departmentId") Long departmentId);

        // Lấy tất cả scores theo semester, scoreType và class, sắp xếp theo điểm
        @Query("SELECT ss FROM StudentScore ss " +
                        "WHERE ss.semester.id = :semesterId " +
                        "AND ss.scoreType = :scoreType " +
                        "AND ss.student.studentClass.id = :classId " +
                        "AND ss.student.isDeleted = false " +
                        "ORDER BY ss.score DESC")
        List<StudentScore> findBySemesterIdAndScoreTypeAndClassIdOrderByScoreDesc(
                        @Param("semesterId") Long semesterId,
                        @Param("scoreType") ScoreType scoreType,
                        @Param("classId") Long classId);
}
