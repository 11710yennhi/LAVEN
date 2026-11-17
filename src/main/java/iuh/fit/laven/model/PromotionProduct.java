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
@Table(name = "promotion_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionProduct {

    @EmbeddedId
    private PromotionProductId id = new PromotionProductId();

    @MapsId("promotionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotionId", nullable = false)
    private Promotion promotion;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productId", nullable = false)
    private Product product;

    public PromotionProduct(Promotion promotion, Product product) {
        setPromotion(promotion);
        setProduct(product);
    }

    public void setPromotion(Promotion promotion) {
        if (promotion == null) {
            throw new IllegalArgumentException("Khuyến mãi không được rỗng");
        }
        if (promotion.getApplyType() != PromotionApplyType.PRODUCT) {
            throw new IllegalArgumentException("Khuyến mãi không hợp lệ");
        }
        this.promotion = promotion;
        this.id.setPromotionId(promotion.getPromotionId());
    }

    public void setProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không được rỗng");
        }
        this.product = product;
        this.id.setProductId(product.getProductId());
    }

    @Override
    public String toString() {
        return "PromotionProduct{promotionId='" + id.getPromotionId()
                + "', productId='" + id.getProductId() + "'}";
    }
}