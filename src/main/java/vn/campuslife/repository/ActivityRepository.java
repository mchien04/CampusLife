package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
public interface ActivityRepository extends JpaRepository<Activity, Long>,
        JpaSpecificationExecutor<Activity> {

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
      @Param("end") LocalDate end);

  @Query("""
      select distinct a
      from Activity a
      join a.organizers d
      where a.isDeleted = false
        and d.id = :deptId
      order by a.startDate asc
      """)
  List<Activity> findForDepartment(@Param("deptId") Long deptId);

  /**
   * Lấy danh sách activities trong series
   */
  List<Activity> findBySeriesIdAndIsDeletedFalse(Long seriesId);

  /**
   * Đếm số activities trong một series cụ thể
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.seriesId = :seriesId AND a.isDeleted = false")
  Long countBySeriesId(@Param("seriesId") Long seriesId);

  /**
   * Tìm activity theo check-in code
   */
  Optional<Activity> findByCheckInCode(String checkInCode);

  /**
   * Đếm số activities theo type
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.type = :type AND a.isDeleted = false")
  Long countByType(@Param("type") ActivityType type);

  /**
   * Đếm số activities theo scoreType
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.scoreType = :scoreType AND a.isDeleted = false")
  Long countByScoreType(@Param("scoreType") ScoreType scoreType);

  /**
   * Đếm số activities theo trạng thái draft
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.isDraft = :isDraft AND a.isDeleted = false")
  Long countByIsDraft(@Param("isDraft") boolean isDraft);

  /**
   * Đếm số activities trong series
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.seriesId IS NOT NULL AND a.isDeleted = false")
  Long countActivitiesInSeries();

  /**
   * Đếm số activities đơn lẻ (không thuộc series)
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.seriesId IS NULL AND a.isDeleted = false")
  Long countStandaloneActivities();

  /**
   * Đếm số activities theo khoa tổ chức
   */
  @Query("SELECT COUNT(DISTINCT a) FROM Activity a JOIN a.organizers d WHERE d.id = :departmentId AND a.isDeleted = false")
  Long countByDepartmentId(@Param("departmentId") Long departmentId);

  /**
   * Đếm số activities trong khoảng thời gian
   */
  @Query("SELECT COUNT(a) FROM Activity a WHERE a.startDate >= :startDate AND a.startDate <= :endDate AND a.isDeleted = false")
  Long countByDateRange(@Param("startDate") java.time.LocalDateTime startDate,
      @Param("endDate") java.time.LocalDateTime endDate);
  //Tìm sự kiện trong tháng
  @Query("""
      select a from Activity a
      where a.isDeleted = false
        and a.startDate >= :start
        and a.startDate <  :end
      order by a.startDate desc
      """)
  List<Activity> findInMonth(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

}
