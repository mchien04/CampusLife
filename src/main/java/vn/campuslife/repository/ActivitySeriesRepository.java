package vn.campuslife.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.ActivitySeries;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, Long> {
    @Query("SELECT a FROM ActivitySeries s JOIN s.activities a WHERE s.id = :seriesId AND a.isDeleted = false")
    List<Activity> findActivitiesBySeriesId(@Param("seriesId") Long seriesId);

    @EntityGraph(attributePaths = "activities")
    List<ActivitySeries> findAllByCreatedByAndIsDeletedFalseOrderByCreatedAtDesc(String createdBy);

    @EntityGraph(attributePaths = "activities")
    Optional<ActivitySeries> findWithActivitiesById(Long id);
    ActivitySeries getSeriesById(Long id);



}
