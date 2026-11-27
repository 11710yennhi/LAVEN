package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.Variant;
import lombok.Getter;

/** Biến thể khách được xem: có giá bán, không có giá nhập. */
@Getter
public class VariantResponse {
    private final String variantId;
    private final String productId;
    private final String size;
    private final Double weight;
    private final Double capacity;
    private final Double length;
    private final Double width;
    private final Double height;
    private final Integer salePrice;
    private final Integer finalPrice;

    /** variant.product phải đã được nạp (VariantRepository có @EntityGraph). */
    public VariantResponse(Variant variant) {
        this(variant, variant.getSalePrice());
    }

    public VariantResponse(Variant variant, Integer finalPrice) {
        this.finalPrice = finalPrice;
        this.variantId = variant.getVariantId();
        this.productId = variant.getProduct().getProductId();
        this.size = variant.getSize();
        this.weight = variant.getWeight();
        this.capacity = variant.getCapacity();
        this.length = variant.getLength();
        this.width = variant.getWidth();
        this.height = variant.getHeight();
        this.salePrice = variant.getSalePrice();
    }
}
