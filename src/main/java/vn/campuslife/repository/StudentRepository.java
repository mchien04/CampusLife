package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Student;

import java.util.Collection;
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
     * Tìm kiếm sinh viên theo tên hoặc mã sinh viên
     */
    @Query("SELECT s FROM Student s WHERE s.isDeleted = false " +
           "AND (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Student> searchByFullNameOrStudentCode(@Param("keyword") String keyword, Pageable pageable);

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

    //tìm danh sách tất cả sinh viên cùng 1 khoa
    List<Student> findByDepartment_IdIn(Collection<Long> departmentIds);

    /**
     * Đếm số sinh viên theo khoa
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.department.id = :departmentId AND s.isDeleted = false")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * Đếm số sinh viên theo lớp
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.studentClass.id = :classId AND s.isDeleted = false")
    Long countByClassId(@Param("classId") Long classId);

    /**
     * Đếm tổng số sinh viên
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.isDeleted = false")
    Long countAllActive();

    /**
     * Tìm sinh viên chưa tham gia activity nào
     */
    @Query("SELECT s FROM Student s WHERE s.isDeleted = false " +
            "AND NOT EXISTS (SELECT 1 FROM ActivityParticipation ap WHERE ap.registration.student.id = s.id)")
    List<Student> findInactiveStudents();

}

