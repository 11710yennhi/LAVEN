package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.Variant;
import lombok.Getter;

/** Staff thấy thêm giá nhập của biến thể. */
@Getter
public class StaffVariantResponse extends VariantResponse {
    private final Integer originalPrice;

    public StaffVariantResponse(Variant variant) {
        super(variant);
        this.originalPrice = variant.getOriginalPrice();
    }
}
