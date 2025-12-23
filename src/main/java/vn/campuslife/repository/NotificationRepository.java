package vn.campuslife.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.Notification;
import vn.campuslife.enumeration.NotificationStatus;
import vn.campuslife.enumeration.NotificationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(Long userId,
            NotificationStatus status);

    List<Notification> findByUserIdAndTypeAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, NotificationType type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.status = 'UNREAD' AND n.isDeleted = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.createdAt >= :since AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Find notification by ID and user ID for validation
    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.user.id = :userId AND n.isDeleted = false")
    Optional<Notification> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
