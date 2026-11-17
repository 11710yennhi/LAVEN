package iuh.fit.laven.dto.request;

import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {
    private String name;
    private String description;
    private PromotionApplyType applyType;
    private String code;              // ORDER/SHIPPING: để trống khi tạo thì hệ thống tự sinh
    private DiscountType discountType;
    private Integer discountValue;
    private Integer minOrderValue;    // null = 0
    private Integer maxDiscountValue; // null = 0 (không giới hạn)
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean status;           // null: tạo mới = true, cập nhật = giữ nguyên
    private Integer maxOrderUsage;    // null = không giới hạn; chỉ dùng cho ORDER/SHIPPING
    // Phạm vi cho khuyến mãi PRODUCT
    private List<String> brands;
    private List<String> categoryIds;
    private List<String> productIds;
}
