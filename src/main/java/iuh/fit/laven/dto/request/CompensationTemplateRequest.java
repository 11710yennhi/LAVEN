package iuh.fit.laven.dto.request;

import iuh.fit.laven.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sửa mã mẫu KM00000000: cả hai giá trị là PHẦN TRĂM của giá trị đơn bị hủy. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompensationTemplateRequest {
    private DiscountType discountType;
    private Integer discountValue;
    private Integer maxDiscountValue;
}
