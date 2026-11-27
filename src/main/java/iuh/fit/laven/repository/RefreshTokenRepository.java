package iuh.fit.laven.repository;

import iuh.fit.laven.model.RefreshToken;
import iuh.fit.laven.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    void deleteByUser(User user);

    long deleteByExpiresAtBefore(LocalDateTime time);
}
