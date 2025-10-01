package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Activity;
import vn.campuslife.entity.Student;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("""
                select s.department.id
                from Student s
                where s.user.username = :username and s.isDeleted = false
            """)
    Long findDepartmentIdByUsername(@Param("username") String username);

    /**
     * Tìm sinh viên theo danh sách department ID và chưa bị xóa
     */
    @Query("SELECT s FROM Student s WHERE s.department.id IN :departmentIds AND s.isDeleted = false")
    List<Student> findByDepartmentIdInAndIsDeletedFalse(@Param("departmentIds") List<Long> departmentIds);

    /**
     * Tìm sinh viên theo User ID
     */
    @Query("SELECT s FROM Student s WHERE s.user.id = :userId AND s.isDeleted = false")
    Optional<Student> findByUserIdAndIsDeletedFalse(@Param("userId") Long userId);

    /**
     * Tìm sinh viên theo username
     */
    @Query("SELECT s FROM Student s WHERE s.user.username = :username AND s.isDeleted = false")
    Optional<Student> findByUserUsernameAndIsDeletedFalse(@Param("username") String username);

    /**
     * Tìm sinh viên theo ID và chưa bị xóa
     */
    Optional<Student> findByIdAndIsDeletedFalse(Long id);

    //tìm danh sách tất cả sinh viên cùng 1 khoa
    List<Student> findByDepartment_IdIn(Collection<Long> departmentIds);

}
