package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.UserDepartment;
import vn.campuslife.entity.UserDepartmentId;

import java.util.Set;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, UserDepartmentId> {

    @Query("""
            SELECT ud.department.id
            FROM UserDepartment ud
            WHERE ud.user.id = :userId
              AND ud.department.isDeleted = false
            """)
    Set<Long> findActiveDepartmentIdsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT ud.department.id
            FROM UserDepartment ud
            WHERE ud.user.username = :username
              AND ud.user.isDeleted = false
              AND ud.department.isDeleted = false
            """)
    Set<Long> findActiveDepartmentIdsByUsername(@Param("username") String username);

    void deleteByUser_Id(Long userId);

    java.util.List<UserDepartment> findByUser_Id(Long userId);
}
