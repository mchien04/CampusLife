package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameQuiz;

import java.util.Optional;

@Repository
public interface MiniGameQuizRepository extends JpaRepository<MiniGameQuiz, Long> {
    
    /**
     * Tìm quiz theo miniGameId
     */
    @Query("SELECT q FROM MiniGameQuiz q WHERE q.miniGame.id = :miniGameId")
    Optional<MiniGameQuiz> findByMiniGameId(@Param("miniGameId") Long miniGameId);
}

