package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.MiniGameAnswer;

import java.util.List;

public interface MiniGameAnswerRepository extends JpaRepository<MiniGameAnswer, Long> {
    List<MiniGameAnswer> findAllByAttemptId(Long attemptId);
}