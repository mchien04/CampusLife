package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.PreparationTaskMember;

@Repository
public interface PreparationTaskMemberRepository extends JpaRepository<PreparationTaskMember, Long> {
    boolean existsByTaskIdAndStudentId(Long taskId, Long studentId);
}

