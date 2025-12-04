package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EmailHistory;

@Repository
public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {
    Page<EmailHistory> findBySenderIdOrderBySentAtDesc(Long senderId, Pageable pageable);
    
    Page<EmailHistory> findByRecipientIdOrderBySentAtDesc(Long recipientId, Pageable pageable);
}

