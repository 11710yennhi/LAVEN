package iuh.fit.laven.scheduler;

import iuh.fit.laven.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * UC36: mỗi ngày tự từ chối phiếu trả hàng đang APPROVED mà quá 7 ngày kể từ approvedAt
 * khách chưa gửi hàng (trừ NOT_RECEIVED). rejectionReason = "Quá 7 ngày không gửi hàng", chốt sổ đơn.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnDeadlineJob {

    private final ReturnService returnService;

    // Mặc định 02:00 mỗi ngày; đổi bằng app.jobs.cron trong application.properties
    @Scheduled(cron = "${app.jobs.cron:0 0 2 * * *}")
    public void run() {
        int rejected = returnService.rejectExpiredApprovedReturns();
        if (rejected > 0) {
            log.info("ReturnDeadlineJob: tự từ chối {} phiếu trả hàng quá hạn gửi hàng", rejected);
        }
    }
}
