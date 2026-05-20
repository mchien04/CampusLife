package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ChatbotMessage;

import java.util.Optional;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    Page<ChatbotMessage> findByConversation_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    Optional<ChatbotMessage> findTop1ByConversation_IdAndIsDeletedFalseOrderByCreatedAtDesc(Long conversationId);
}
