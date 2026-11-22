package iuh.fit.laven.scheduler;

import iuh.fit.laven.service.ai.ProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mỗi đêm: upsert sản phẩm đang bán, xóa sản phẩm ngừng bán khỏi chỉ mục vector. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexJob {

    private final ProductIndexService productIndexService;

    // Dùng chung lịch với các job khác của nhóm (mặc định 02:00 mỗi ngày)
    @Scheduled(cron = "${app.jobs.cron:0 0 2 * * *}")
    public void run() {
        if (!productIndexService.isConfigured()) {
            log.warn("Bỏ qua đánh chỉ mục: chưa có GEMINI_API_KEY");
            return;
        }
        try {
            ProductIndexService.ReindexResult result = productIndexService.reindexAll();
            log.info("Job đánh chỉ mục: {} sản phẩm, xóa {}", result.indexed(), result.removed());
        } catch (Exception e) {
            // Job lỗi không được làm sập app; đêm sau sẽ chạy lại
            log.error("Job đánh chỉ mục thất bại", e);
        }
    }
}