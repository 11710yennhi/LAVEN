package iuh.fit.laven.scheduler;

import iuh.fit.laven.service.ai.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mỗi ngày xóa số liệu đếm lượt hỏi chatbot cũ hơn 7 ngày. */
@Component
@RequiredArgsConstructor
public class ChatUsageCleanupJob {

    private final ChatbotService chatbotService;

    // Mặc định 02:00 mỗi ngày; đổi bằng app.jobs.cron trong application.properties
    @Scheduled(cron = "${app.jobs.cron:0 0 2 * * *}")
    public void run() {
        chatbotService.cleanupOldUsages();
    }
}
