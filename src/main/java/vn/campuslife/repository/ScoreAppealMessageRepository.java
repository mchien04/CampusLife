package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ScoreAppealMessage;

import java.util.List;

@Repository
public interface ScoreAppealMessageRepository extends JpaRepository<ScoreAppealMessage, Long> {

    List<ScoreAppealMessage> findByAppealIdOrderByCreatedAtAsc(Long appealId);
}
