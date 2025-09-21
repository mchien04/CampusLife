package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.StudentClass;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {

    List<StudentClass> findByDepartmentIdAndIsDeletedFalse(Long departmentId);

    List<StudentClass> findByIsDeletedFalse();

    Optional<StudentClass> findByClassNameAndIsDeletedFalse(String className);

    @Query("SELECT sc FROM StudentClass sc WHERE sc.department.id = :departmentId AND sc.isDeleted = false ORDER BY sc.className")
    List<StudentClass> findActiveClassesByDepartment(@Param("departmentId") Long departmentId);

    Optional<StudentClass> findByIdAndIsDeletedFalse(Long id);
}
