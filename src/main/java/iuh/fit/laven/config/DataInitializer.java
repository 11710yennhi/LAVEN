package iuh.fit.laven.config;

import iuh.fit.laven.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Khi khởi động: tạo mã mẫu KM00000000 nếu chưa có (tài liệu mục c.14 bước 1). */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PromotionService promotionService;

    @Override
    public void run(ApplicationArguments args) {
        promotionService.ensureCompensationTemplate();
    }
}
