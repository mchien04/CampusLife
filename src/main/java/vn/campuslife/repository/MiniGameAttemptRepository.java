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

    /**
     * Đếm tổng số attempts
     */
    @Query("SELECT COUNT(mga) FROM MiniGameAttempt mga")
    Long countAll();

    /**
     * Đếm số attempts theo status
     */
    @Query("SELECT COUNT(mga) FROM MiniGameAttempt mga WHERE mga.status = :status")
    Long countByStatus(@Param("status") AttemptStatus status);

    /**
     * Đếm số attempts theo minigame
     */
    @Query("SELECT COUNT(mga) FROM MiniGameAttempt mga WHERE mga.miniGame.id = :miniGameId")
    Long countByMiniGameId(@Param("miniGameId") Long miniGameId);

    /**
     * Đếm số attempts theo minigame và status
     */
    @Query("SELECT COUNT(mga) FROM MiniGameAttempt mga " +
            "WHERE mga.miniGame.id = :miniGameId AND mga.status = :status")
    Long countByMiniGameIdAndStatus(@Param("miniGameId") Long miniGameId, @Param("status") AttemptStatus status);

    /**
     * Tính điểm trung bình theo minigame
     * Lấy điểm từ ActivityParticipation (vì điểm được lưu ở đó, không phải trong MiniGameAttempt)
     */
    @Query("SELECT AVG(ap.pointsEarned) FROM MiniGameAttempt mga " +
            "JOIN mga.miniGame mg " +
            "JOIN mg.activity a " +
            "JOIN ActivityRegistration ar ON ar.activity.id = a.id AND ar.student.id = mga.student.id " +
            "JOIN ActivityParticipation ap ON ap.registration.id = ar.id " +
            "WHERE mga.miniGame.id = :miniGameId " +
            "AND mga.status = 'PASSED' " +
            "AND ap.participationType = 'COMPLETED' " +
            "AND ap.pointsEarned IS NOT NULL")
    java.math.BigDecimal calculateAverageScoreByMiniGameId(@Param("miniGameId") Long miniGameId);

    /**
     * Tính số câu đúng trung bình theo minigame
     */
    @Query("SELECT AVG(mga.correctCount) FROM MiniGameAttempt mga " +
            "WHERE mga.miniGame.id = :miniGameId")
    Double calculateAverageCorrectAnswersByMiniGameId(@Param("miniGameId") Long miniGameId);

    /**
     * Top minigames có nhiều attempts nhất
     */
    @Query("SELECT mga.miniGame.id, COUNT(mga) as attemptCount FROM MiniGameAttempt mga " +
            "GROUP BY mga.miniGame.id " +
            "ORDER BY attemptCount DESC")
    List<Object[]> findTopMiniGamesByAttempts(org.springframework.data.domain.Pageable pageable);

    /**
     * Đếm số sinh viên unique đã làm minigame
     */
    @Query("SELECT COUNT(DISTINCT mga.student.id) FROM MiniGameAttempt mga " +
            "WHERE mga.miniGame.id = :miniGameId")
    Long countUniqueStudentsByMiniGameId(@Param("miniGameId") Long miniGameId);
}

