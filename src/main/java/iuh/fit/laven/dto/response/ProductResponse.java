package iuh.fit.laven.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.laven.model.Product;
import lombok.Getter;

/** Dữ liệu sản phẩm khách được xem; không chứa giá nhập hay phí đóng gói. */
@Getter
public class ProductResponse {
    private final String productId;
    private final String productName;
    private final String categoryId;
    private final String categoryName;
    private final String brand;
    private final String origin;
    private final String material;
    /** Giá bán thấp nhất / cao nhất trong các size (null nếu sản phẩm chưa có biến thể). */
    private final Integer minSalePrice;
    private final Integer maxSalePrice;
    private final Integer finalPrice;
    private final Integer discount;
    private final java.util.List<ColorOption> colors;

    public record ColorOption(String colorId, String name) {}
    private final String description;
    private final String waterResistance;
    private final String wheel;
    private final String zipper;
    private final String warranty;
    private final Integer maxQuantityPerOrder;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String imageUrl;

    public ProductResponse(Product product, SalePriceRange priceRange) {
        this(product, priceRange, null);
    }

    public ProductResponse(Product product, SalePriceRange priceRange, String imageUrl) {
        this(product, priceRange, imageUrl, priceRange == null ? null : priceRange.min(), java.util.List.of());
    }

    public ProductResponse(Product product, SalePriceRange priceRange, String imageUrl,
                           Integer finalPrice, java.util.List<ColorOption> colors) {
        this.finalPrice = finalPrice;
        this.discount = finalPrice == null ? null : priceRange.min() - finalPrice;
        this.colors = colors;
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.categoryId = product.getCategory().getCategoryId();
        this.categoryName = product.getCategory().getCategoryName();
        this.brand = product.getBrand();
        this.origin = product.getOrigin();
        this.material = product.getMaterial();
        this.minSalePrice = priceRange == null ? null : priceRange.min();
        this.maxSalePrice = priceRange == null ? null : priceRange.max();
        this.description = product.getDescription();
        this.waterResistance = product.getWaterResistance();
        this.wheel = product.getWheel();
        this.zipper = product.getZipper();
        this.warranty = product.getWarranty();
        this.maxQuantityPerOrder = product.getMaxQuantityPerOrder();
        this.imageUrl = imageUrl;
    }
}
