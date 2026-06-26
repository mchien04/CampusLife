package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.RecalculationJob;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecalculationJobRepository extends JpaRepository<RecalculationJob, Long> {

    @Query("SELECT COUNT(j) FROM RecalculationJob j WHERE j.semesterId = :semesterId AND j.status IN :statuses")
    long countBySemesterIdAndStatusIn(@Param("semesterId") Long semesterId, @Param("statuses") List<String> statuses);

    List<RecalculationJob> findByStatus(String status);

    Optional<RecalculationJob> findBySemesterIdAndStatusIn(Long semesterId, List<String> statuses);
}
