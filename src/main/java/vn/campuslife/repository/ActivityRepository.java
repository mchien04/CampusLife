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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long > ,JpaSpecificationExecutor<Activity>{

    List<Activity> findByIsDeletedFalse();
    Optional<Activity> findByIdAndIsDeletedFalse(Long id);
    @Query("""
    select a from Activity a
    where a.isDeleted = false
      and a.scoreType = :scoreType
      and a.endDate >= CURRENT_DATE
    order by a.startDate asc
    """)
    List<Activity> findByScoreTypeActive(@Param("scoreType") ScoreType scoreType);

    List<Activity> findByIsDeletedFalseOrderByStartDateAsc();

    @Query("""
       select a from Activity a
       where a.isDeleted = false
         and a.startDate >= :start
         and a.startDate <  :end
       order by a.startDate desc
""")

    List<Activity> findInMonth(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);


    @Query("""
        select distinct a
        from Activity a
        join a.organizers d
        where a.isDeleted = false
          and d.id = :deptId
          and a.startDate > CURRENT_TIMESTAMP
        order by a.startDate asc
""")
    List<Activity> findForDepartment(@Param("deptId") Long deptId);



    @Query("SELECT a FROM ActivitySeries s JOIN s.activities a WHERE s.id = :seriesId AND a.isDeleted = false")
    List<Activity> findAllBySeries_IdAndIsDeletedFalse(@Param("seriesId") Long seriesId);

    @Query("SELECT a FROM Activity a WHERE a.id = :id AND a.id IN " +
            "(SELECT act.id FROM ActivitySeries s JOIN s.activities act WHERE s.id = :seriesId)")
    Optional<Activity> findByIdAndSeriesId(@Param("id") Long id, @Param("seriesId") Long seriesId);



}
