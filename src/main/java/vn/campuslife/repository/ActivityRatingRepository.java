package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.campuslife.entity.ActivityRating;

import java.util.List;
import java.util.Optional;


public interface ActivityRatingRepository extends JpaRepository<ActivityRating, Long> {
    List<ActivityRating> findByActivity_Id(long activityId);
    List<ActivityRating> findByStudent_Id(long studentId);
    Optional<ActivityRating> findByActivity_IdAndStudent_Id(long activityId, long studentId);
    @Query("SELECT AVG(r.rating) FROM ActivityRating r WHERE r.activity.id = :activityId")
    Double findAverageRatingByActivityId(Long activityId);
}
