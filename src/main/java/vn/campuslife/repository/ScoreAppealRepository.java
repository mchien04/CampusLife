package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ScoreAppeal;
import vn.campuslife.enumeration.ScoreAppealStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface ScoreAppealRepository extends JpaRepository<ScoreAppeal, Long> {

    List<ScoreAppeal> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT a FROM ScoreAppeal a " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND (:semesterId IS NULL OR a.semester.id = :semesterId) " +
           "AND (:studentId IS NULL OR a.student.id = :studentId) " +
           "ORDER BY a.createdAt DESC")
    Page<ScoreAppeal> findFiltered(
            @Param("status") ScoreAppealStatus status,
            @Param("semesterId") Long semesterId,
            @Param("studentId") Long studentId,
            Pageable pageable);

    @Query("SELECT a FROM ScoreAppeal a " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND (:semesterId IS NULL OR a.semester.id = :semesterId) " +
           "AND (:studentId IS NULL OR a.student.id = :studentId) " +
           "AND a.student.department.id IN :departmentIds " +
           "ORDER BY a.createdAt DESC")
    Page<ScoreAppeal> findFilteredScoped(
            @Param("status") ScoreAppealStatus status,
            @Param("semesterId") Long semesterId,
            @Param("studentId") Long studentId,
            @Param("departmentIds") Collection<Long> departmentIds,
            Pageable pageable);
}
