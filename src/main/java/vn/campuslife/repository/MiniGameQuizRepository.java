package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGame;
import vn.campuslife.entity.MiniGameQuiz;

import java.util.Optional;

@Repository
public interface MiniGameQuizRepository extends JpaRepository<MiniGameQuiz, Long> {

    Optional<MiniGameQuiz> findByMiniGameId(Long miniGameId);
    Optional<MiniGameQuiz> findByMiniGame(MiniGame miniGame);

}

