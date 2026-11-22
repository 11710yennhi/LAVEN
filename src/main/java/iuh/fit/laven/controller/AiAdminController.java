package iuh.fit.laven.controller;

import iuh.fit.laven.dto.response.MessageResponse;
import iuh.fit.laven.service.ai.ProductIndexService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API quản trị AI. /api/admin/** chỉ ADMIN gọi được (SecurityConfig đã cấu hình). */
@RestController
@RequestMapping("/api/admin/ai")
public class AiAdminController {

    private final ProductIndexService productIndexService;

    public AiAdminController(ProductIndexService productIndexService) {
        this.productIndexService = productIndexService;
    }

    /** Đánh lại chỉ mục ngay (dùng khi demo, không cần chờ job ban đêm). */
    @PostMapping("/reindex")
    public MessageResponse reindex() {
        ProductIndexService.ReindexResult result = productIndexService.reindexAll();
        return new MessageResponse("Đã đánh chỉ mục " + result.indexed()
                + " sản phẩm đang bán, xóa " + result.removed() + " sản phẩm ngừng bán khỏi chỉ mục");
    }
}