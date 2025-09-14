package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Activity;
import vn.campuslife.enumeration.ActivityType;
import vn.campuslife.enumeration.ScoreType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByIsDeletedFalse();
    Optional<Activity> findByIdAndIsDeletedFalse(Long id);
    List<Activity> findByScoreTypeAndIsDeletedFalseOrderByStartDateAsc(ScoreType scoreType);
    List<Activity> findByIsDeletedFalseOrderByStartDateAsc();

    @Query("""
       select a from Activity a
       where a.isDeleted = false
         and a.startDate >= :start
         and a.startDate <  :end
       order by a.startDate desc
       """)
    List<Activity> findInMonth(@Param("start") LocalDate start,
                               @Param("end")   LocalDate end);

    @Query("""
        select distinct a
        from Activity a
        join a.organizers d
        where a.isDeleted = false
          and d.id = :deptId
        order by a.startDate asc
        """)
    List<Activity> findForDepartment(@Param("deptId") Long deptId);
}
