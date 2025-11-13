package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.MiniGameAttempt;

import java.util.Optional;

public interface MiniGameAttemptRepository extends JpaRepository<MiniGameAttempt, Long> {
    Optional<MiniGameAttempt> findByMiniGameIdAndStudentId(Long miniGameId, Long studentId);
}
