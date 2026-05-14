package vn.campuslife.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import vn.campuslife.enumeration.ChatbotMessageRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatbotMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatbotConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatbotMessageRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean isDeleted = false;

    @CreatedDate
    private LocalDateTime createdAt;
}
