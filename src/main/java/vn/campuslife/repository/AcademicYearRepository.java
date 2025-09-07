package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.AcademicYear;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
}

