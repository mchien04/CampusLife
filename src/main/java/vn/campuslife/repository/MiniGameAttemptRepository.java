package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameAttempt;
import vn.campuslife.enumeration.AttemptStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiniGameAttemptRepository extends JpaRepository<MiniGameAttempt, Long> {

    /**
     * Tìm attempt theo student và minigame
     */
    @Query("SELECT mga FROM MiniGameAttempt mga " +
            "WHERE mga.student.id = :studentId AND mga.miniGame.id = :miniGameId " +
            "ORDER BY mga.startedAt DESC")
    List<MiniGameAttempt> findByStudentIdAndMiniGameId(
            @Param("studentId") Long studentId,
            @Param("miniGameId") Long miniGameId);

    /**
     * Tìm attempt đang làm (IN_PROGRESS) của student
     */
    @Query("SELECT mga FROM MiniGameAttempt mga " +
            "WHERE mga.student.id = :studentId AND mga.miniGame.id = :miniGameId AND mga.status = :status " +
            "ORDER BY mga.startedAt DESC")
    Optional<MiniGameAttempt> findInProgressAttempt(
            @Param("studentId") Long studentId,
            @Param("miniGameId") Long miniGameId,
            @Param("status") AttemptStatus status);
}

