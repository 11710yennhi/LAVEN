package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.PromotionApplyType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_brands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionBrand {

    // Khóa chính kép (promotionId, brand). brand chỉ là chuỗi, không phải bảng riêng
    @EmbeddedId
    private PromotionBrandId id = new PromotionBrandId();

    @MapsId("promotionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotionId", nullable = false)
    private Promotion promotion;

    public PromotionBrand(Promotion promotion, String brand) {
        setPromotion(promotion);
        setBrand(brand);
    }

    public String getBrand() {
        return id.getBrand();
    }

    public void setPromotion(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException("Khuyến mãi không được rỗng");
        }
        // Chỉ khuyến mãi sản phẩm mới có phạm vi thương hiệu / danh mục / sản phẩm
        if (promotion.getApplyType() != PromotionApplyType.PRODUCT) {
            throw new IllegalArgumentException("Khuyến mãi không hợp lệ");
        }
        this.promotion = promotion;
        this.id.setPromotionId(promotion.getPromotionId());
    }

    public void setBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được rỗng");
        }
        String value = brand.trim();
        if (value.length() > 100) {
            throw new IllegalArgumentException("Tên thương hiệu tối đa 100 ký tự");
        }
        this.id.setBrand(value);
    }

    @Override
    public String toString() {
        return "PromotionBrand{promotionId='" + id.getPromotionId() + "', brand='" + id.getBrand() + "'}";
    }
}