package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGame;

import java.util.Optional;

@Repository
public interface MiniGameRepository extends JpaRepository<MiniGame, Long> {

    /**
     * Tìm minigame theo activity ID
     */
    @Query("SELECT mg FROM MiniGame mg WHERE mg.activity.id = :activityId")
    Optional<MiniGame> findByActivityId(@Param("activityId") Long activityId);
}

