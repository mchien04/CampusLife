package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.EmailAttachment;

import java.util.List;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {
    List<EmailAttachment> findByEmailHistoryId(Long emailHistoryId);
    
    void deleteByEmailHistoryId(Long emailHistoryId);
}

