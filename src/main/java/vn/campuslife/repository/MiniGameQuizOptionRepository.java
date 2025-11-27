package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGameQuizOption;

@Repository
public interface MiniGameQuizOptionRepository extends JpaRepository<MiniGameQuizOption, Long> {
}

