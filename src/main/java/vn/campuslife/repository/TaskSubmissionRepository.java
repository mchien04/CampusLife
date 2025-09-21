package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.TaskSubmission;
import vn.campuslife.enumeration.SubmissionStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {

    List<TaskSubmission> findByTaskIdAndIsDeletedFalseOrderBySubmittedAtDesc(Long taskId);

    List<TaskSubmission> findByStudentIdAndIsDeletedFalseOrderBySubmittedAtDesc(Long studentId);

    Optional<TaskSubmission> findByTaskIdAndStudentIdAndIsDeletedFalse(Long taskId, Long studentId);

    List<TaskSubmission> findByStatusAndIsDeletedFalseOrderBySubmittedAtDesc(SubmissionStatus status);

    @Query("SELECT ts FROM TaskSubmission ts WHERE ts.task.id = :taskId AND ts.student.id = :studentId AND ts.isDeleted = false")
    Optional<TaskSubmission> findLatestByTaskAndStudent(@Param("taskId") Long taskId,
            @Param("studentId") Long studentId);

    @Query("SELECT ts FROM TaskSubmission ts WHERE ts.task.id = :taskId AND ts.isDeleted = false ORDER BY ts.submittedAt DESC")
    List<TaskSubmission> findAllByTaskIdOrderBySubmittedAtDesc(@Param("taskId") Long taskId);

    @Query("SELECT ts FROM TaskSubmission ts WHERE ts.student.id = :studentId AND ts.isDeleted = false ORDER BY ts.submittedAt DESC")
    List<TaskSubmission> findAllByStudentIdOrderBySubmittedAtDesc(@Param("studentId") Long studentId);
}
