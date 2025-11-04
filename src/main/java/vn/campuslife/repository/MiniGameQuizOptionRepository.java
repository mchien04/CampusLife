package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.campuslife.entity.MiniGameQuizOption;
import vn.campuslife.entity.MiniGameQuizQuestion;

import java.util.List;

public interface MiniGameQuizOptionRepository extends JpaRepository<MiniGameQuizOption, Long> {
    List<MiniGameQuizOption> findByQuestion(MiniGameQuizQuestion question);

}