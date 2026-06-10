package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivityOrganizer;

import java.util.List;

@Repository
public interface ActivityOrganizerRepository extends JpaRepository<ActivityOrganizer, Long> {
    boolean existsByActivityIdAndStudentId(Long activityId, Long studentId);

    boolean existsByActivityIdAndStudentIdAndIsPrepSupervisorTrue(Long activityId, Long studentId);

    java.util.Optional<ActivityOrganizer> findByActivityIdAndStudentId(Long activityId, Long studentId);

    List<ActivityOrganizer> findByActivityIdAndIsPrepSupervisorTrue(Long activityId);

    List<ActivityOrganizer> findByActivityId(Long activityId);

    long deleteByActivityIdAndStudentId(Long activityId, Long studentId);

    @Query("""
            select ao.activity.id
            from ActivityOrganizer ao
            where ao.student.id = :studentId
              and ao.activity.isDeleted = false
              and ao.activity.hasPreparation = true
            """)
    List<Long> findPreparationEnabledActivityIdsByStudentId(@Param("studentId") Long studentId);
}
