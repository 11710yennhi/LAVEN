package iuh.fit.laven.repository;

import iuh.fit.laven.model.PromotionProduct;
import iuh.fit.laven.model.PromotionProductId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionProductRepository extends JpaRepository<PromotionProduct, PromotionProductId> {
    List<PromotionProduct> findByPromotion_PromotionId(String promotionId);

    void deleteByPromotion_PromotionId(String promotionId);
}
