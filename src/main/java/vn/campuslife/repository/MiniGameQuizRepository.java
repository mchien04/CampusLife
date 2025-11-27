package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameQuiz;

@Repository
public interface MiniGameQuizRepository extends JpaRepository<MiniGameQuiz, Long> {
}

