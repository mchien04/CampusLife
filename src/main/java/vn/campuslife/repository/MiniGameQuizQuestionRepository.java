package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameQuizQuestion;

@Repository
public interface MiniGameQuizQuestionRepository extends JpaRepository<MiniGameQuizQuestion, Long> {
}

