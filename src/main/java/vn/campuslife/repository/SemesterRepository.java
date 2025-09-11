package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Semester;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
}

