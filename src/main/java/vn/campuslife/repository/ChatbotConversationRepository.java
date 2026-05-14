package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ChatbotConversation;

import java.util.Optional;

@Repository
public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversation, Long> {
    Optional<ChatbotConversation> findByIdAndUser_IdAndIsDeletedFalse(Long id, Long userId);

    Page<ChatbotConversation> findByUser_IdAndIsDeletedFalseOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
