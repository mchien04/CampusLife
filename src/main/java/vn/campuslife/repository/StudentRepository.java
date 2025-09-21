package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Student;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    @Query("SELECT s.user.id FROM Student s WHERE s.department.id = :departmentId AND s.isDeleted = false")
    List<Long> findUserIdsByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT s.user.id FROM Student s WHERE s.studentClass.id = :classId AND s.isDeleted = false")
    List<Long> findUserIdsByClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Student s WHERE s.studentClass.id = :classId AND s.isDeleted = false")
    List<Student> findByClassId(@Param("classId") Long classId);

    /**
     * Lấy danh sách sinh viên có phân trang
     */
    Page<Student> findByIsDeletedFalse(Pageable pageable);

    /**
     * Tìm kiếm sinh viên theo tên
     */
    Page<Student> findByFullNameContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);

    /**
     * Lấy sinh viên chưa có lớp
     */
    Page<Student> findByStudentClassIsNullAndIsDeletedFalse(Pageable pageable);

    /**
     * Lấy sinh viên theo khoa
     */
    Page<Student> findByStudentClassDepartmentIdAndIsDeletedFalse(Long departmentId, Pageable pageable);

    /**
     * Lấy sinh viên theo lớp có phân trang
     */
    Page<Student> findByStudentClassIdAndIsDeletedFalse(Long classId, Pageable pageable);
}