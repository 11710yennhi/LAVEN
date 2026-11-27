package iuh.fit.laven.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Scheduled cho toàn ứng dụng (TokenCleanupJob và các job của thành viên khác). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
