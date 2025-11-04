package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGame;

import java.util.Optional;

@Repository
public interface MiniGameRepository extends JpaRepository<MiniGame, Long> {
    Optional<MiniGame> findByActivityId(Long activityId);
}
