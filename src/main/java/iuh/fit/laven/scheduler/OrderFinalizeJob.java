package iuh.fit.laven.scheduler;

import iuh.fit.laven.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * UC37: mỗi ngày chốt sổ doanh thu cho đơn COMPLETED đã hết hạn trả hàng mà không có phiếu trả
 * (finalizedAt = completedAt + 7 ngày).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFinalizeJob {

    private final StatisticsService statisticsService;

    // Mặc định 02:00 mỗi ngày; đổi bằng app.jobs.cron trong application.properties
    @Scheduled(cron = "${app.jobs.cron:0 0 2 * * *}")
    public void run() {
        int finalized = statisticsService.finalizeOrdersPastReturnWindow();
        if (finalized > 0) {
            log.info("OrderFinalizeJob: chốt sổ {} đơn hàng hết hạn trả hàng", finalized);
        }
    }
}
