package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.MiniGameQuiz;
import vn.campuslife.entity.MiniGameQuizQuestion;

import java.util.List;

public interface MiniGameQuizQuestionRepository extends JpaRepository<MiniGameQuizQuestion, Long> {
    List<MiniGameQuizQuestion> findByMiniGameQuiz(MiniGameQuiz quiz);

}