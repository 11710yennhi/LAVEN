package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private String promotionId;
    private String name;
    private String description;
    private PromotionApplyType applyType;
    private String code;
    private DiscountType discountType;
    private Integer discountValue;
    private Integer minOrderValue;
    private Integer maxDiscountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean status;
    private String customerId;
    private Integer maxOrderUsage;
    private List<String> brands;
    private List<String> categoryIds;
    private List<String> productIds;
}
