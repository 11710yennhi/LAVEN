package iuh.fit.laven.scheduler;

import iuh.fit.laven.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** UC38: mỗi ngày xóa PendingRegistration, PasswordResetToken, RefreshToken hết hạn. */
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final AuthService authService;

    // Mặc định 02:00 mỗi ngày; đổi bằng app.jobs.cron trong application.properties
    @Scheduled(cron = "${app.jobs.cron:0 0 2 * * *}")
    public void run() {
        authService.cleanupExpiredTokens();
    }
}
