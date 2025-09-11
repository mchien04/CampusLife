package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
        select s.department.id
        from Student s
        where s.user.username = :username and s.isDeleted = false
    """)
    Long findDepartmentIdByUsername(@Param("username") String username);
}
