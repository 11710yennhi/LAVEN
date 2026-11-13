package iuh.fit.laven.repository;

import iuh.fit.laven.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    void deleteByEmail(String email);

    long deleteByExpiresAtBefore(LocalDateTime time);
}
