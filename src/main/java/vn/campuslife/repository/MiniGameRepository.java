package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.MiniGame;

import java.util.Optional;
import java.util.Set;

@Repository
public interface MiniGameRepository extends JpaRepository<MiniGame, Long>, JpaSpecificationExecutor<MiniGame> {

    @Query("SELECT mg FROM MiniGame mg WHERE mg.activity.id = :activityId")
    Optional<MiniGame> findByActivityId(@Param("activityId") Long activityId);

    @Query("""
            SELECT CASE WHEN COUNT(mg) > 0 THEN true ELSE false END
            FROM MiniGame mg
            JOIN mg.activity a
            JOIN a.organizers o
            WHERE mg.id = :miniGameId
              AND a.isDeleted = false
              AND o.id IN :deptIds
            """)
    boolean existsByIdAndActivityOrganizerDepartmentIds(@Param("miniGameId") Long miniGameId, @Param("deptIds") Set<Long> deptIds);
}
