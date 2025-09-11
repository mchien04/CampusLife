package vn.campuslife.repository;

import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findById(Long id);

    List<Department> findAll();
}