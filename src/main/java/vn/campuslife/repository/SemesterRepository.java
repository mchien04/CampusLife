package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Semester;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    /**
     * Tìm semester mà một ngày cụ thể nằm trong khoảng startDate và endDate
     * Ưu tiên semester có isOpen = true nếu có nhiều semester trùng
     */
    @Query("""
        SELECT s FROM Semester s 
        WHERE s.startDate <= :date 
        AND s.endDate >= :date 
        ORDER BY s.isOpen DESC, s.startDate DESC
        """)
    Optional<Semester> findByDate(@Param("date") LocalDate date);

    /**
     * Tìm semester mà một LocalDateTime nằm trong khoảng startDate và endDate
     */
    default Optional<Semester> findByDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return Optional.empty();
        }
        return findByDate(dateTime.toLocalDate());
    }
}



