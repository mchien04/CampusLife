package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameAnswer;

import java.util.List;

@Repository
public interface MiniGameAnswerRepository extends JpaRepository<MiniGameAnswer, Long> {

    /**
     * Lấy tất cả answers của một attempt
     */
    @Query("SELECT mga FROM MiniGameAnswer mga WHERE mga.attempt.id = :attemptId")
    List<MiniGameAnswer> findByAttemptId(@Param("attemptId") Long attemptId);
}

