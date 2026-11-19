package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.Product;
import lombok.Getter;

/** Staff thấy thêm dữ liệu nội bộ để quản lý sản phẩm. */
@Getter
public class StaffProductResponse extends ProductResponse {
    private final Integer originalPrice;
    private final Integer packagingCost;
    private final boolean status;
    private final String stopSellingReason;

    public StaffProductResponse(Product product) {
        super(product);
        this.originalPrice = product.getOriginalPrice();
        this.packagingCost = product.getCategory().getPackagingCost();
        this.status = product.isStatus();
        this.stopSellingReason = product.getStopSellingReason();
    }
}
