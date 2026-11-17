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
@Table(name = "promotion_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromotionCategory {

    @EmbeddedId
    private PromotionCategoryId id = new PromotionCategoryId();

    @MapsId("promotionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotionId", nullable = false)
    private Promotion promotion;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;

    public PromotionCategory(Promotion promotion, Category category) {
        setPromotion(promotion);
        setCategory(category);
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

    public void setCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Danh mục sản phẩm không được rỗng");
        }
        this.category = category;
        this.id.setCategoryId(category.getCategoryId());
    }

    @Override
    public String toString() {
        return "PromotionCategory{promotionId='" + id.getPromotionId()
                + "', categoryId='" + id.getCategoryId() + "'}";
    }
}