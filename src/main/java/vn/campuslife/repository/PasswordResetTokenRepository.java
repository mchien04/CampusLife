package vn.campuslife.repository;

import vn.campuslife.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);
    
    Optional<PasswordResetToken> findByUserIdAndUsedFalse(Long userId);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiryDate < ?1")
    void deleteExpiredTokens(LocalDateTime now);
}

