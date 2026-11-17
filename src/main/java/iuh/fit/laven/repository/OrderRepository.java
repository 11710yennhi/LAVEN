package iuh.fit.laven.repository;

import iuh.fit.laven.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    List<Order> findOrderByCustomer_UserIdOrderByCreatedAtDesc(String customerUserId);

    Order findOrderByOrderIdIsAndCustomer_UserId(String orderId, String customerUserId);

    Order findOrderByOrderIdIs(String orderId);

    boolean existsByCustomer_UserIdAndOrderPromotion_PromotionId(String customerUserId, String orderPromotionPromotionId);

    boolean existsByCustomer_UserIdAndShippingPromotion_PromotionId(String customerUserId, String shippingPromotionPromotionId);

    long countByOrderPromotion_PromotionId(String promotionId);

    long countByShippingPromotion_PromotionId(String promotionId);

    long countByCustomer_UserIdAndOrderPromotion_PromotionId(String customerUserId, String orderPromotionPromotionId);

    long countByCustomer_UserIdAndShippingPromotion_PromotionId(String customerUserId, String shippingPromotionPromotionId);

}
