package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.PreparationTaskMember;
import vn.campuslife.enumeration.PreparationTaskMemberRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreparationTaskMemberRepository extends JpaRepository<PreparationTaskMember, Long> {
    boolean existsByTaskIdAndStudentId(Long taskId, Long studentId);

    boolean existsByTaskIdAndStudentIdAndRole(Long taskId, Long studentId, PreparationTaskMemberRole role);

    long countByTaskIdAndRole(Long taskId, PreparationTaskMemberRole role);

    Optional<PreparationTaskMember> findByTaskIdAndStudentId(Long taskId, Long studentId);

    List<PreparationTaskMember> findByTaskIdOrderByRoleAscCreatedAtAsc(Long taskId);

    @Query("""
            select m
            from PreparationTaskMember m
            join fetch m.task t
            where t.activity.id = :activityId
              and m.student.id = :studentId
            order by (case when t.deadline is null then 1 else 0 end), t.deadline asc, t.id asc
            """)
    List<PreparationTaskMember> findByStudentIdAndActivityIdOrderByTaskDeadlineAscIdAsc(
            @Param("studentId") Long studentId,
            @Param("activityId") Long activityId);

    @Query("""
            select m.student.id as studentId, count(distinct m.task.id) as taskCount
            from PreparationTaskMember m
            where m.task.activity.id = :activityId
            group by m.student.id
            """)
    List<StudentTaskCountView> countTasksByStudentInActivity(@Param("activityId") Long activityId);

    interface StudentTaskCountView {
        Long getStudentId();
        Long getTaskCount();
    }
}
