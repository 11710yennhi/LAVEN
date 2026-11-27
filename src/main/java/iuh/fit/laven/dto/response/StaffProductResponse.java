package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.Product;
import lombok.Getter;

/** Staff thấy thêm dữ liệu nội bộ để quản lý sản phẩm. Giá nhập nằm ở từng biến thể (StaffVariantResponse). */
@Getter
public class StaffProductResponse extends ProductResponse {
    private final Integer packagingCost;
    private final boolean status;
    private final String stopSellingReason;
    private final String coverImageId;

    public StaffProductResponse(Product product, SalePriceRange priceRange) {
        this(product, priceRange, null);
    }

    public StaffProductResponse(Product product, SalePriceRange priceRange, String imageUrl) {
        super(product, priceRange, imageUrl);
        this.packagingCost = product.getCategory().getPackagingCost();
        this.status = product.isStatus();
        this.stopSellingReason = product.getStopSellingReason();
        this.coverImageId = product.getCoverImageId();
    }
}
