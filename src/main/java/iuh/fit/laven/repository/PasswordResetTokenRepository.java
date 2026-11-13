package iuh.fit.laven.repository;

import iuh.fit.laven.model.PasswordResetToken;
import iuh.fit.laven.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {
    void deleteByUser(User user);

    long deleteByExpiresAtBefore(LocalDateTime time);
}
