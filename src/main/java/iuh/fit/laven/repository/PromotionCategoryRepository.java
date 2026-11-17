package iuh.fit.laven.repository;

import iuh.fit.laven.model.PromotionCategory;
import iuh.fit.laven.model.PromotionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, PromotionCategoryId> {
    List<PromotionCategory> findByPromotion_PromotionId(String promotionId);

    void deleteByPromotion_PromotionId(String promotionId);
}
