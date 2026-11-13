package iuh.fit.laven.repository;

import iuh.fit.laven.model.PromotionBrand;
import iuh.fit.laven.model.PromotionBrandId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionBrandRepository extends JpaRepository<PromotionBrand, PromotionBrandId> {
    List<PromotionBrand> findByPromotion_PromotionId(String promotionId);

    void deleteByPromotion_PromotionId(String promotionId);
}
