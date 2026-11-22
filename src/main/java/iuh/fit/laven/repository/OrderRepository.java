package iuh.fit.laven.repository;

import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    boolean existsByManualPaymentReference(String manualPaymentReference);

    Order findOrderByOrderIdIsAndCustomer_UserId(String orderId, String customerUserId);

    Order findOrderByOrderIdIs(String orderId);

    boolean existsByCustomer_UserIdAndOrderPromotion_PromotionId(String customerUserId, String orderPromotionPromotionId);

    boolean existsByCustomer_UserIdAndShippingPromotion_PromotionId(String customerUserId, String shippingPromotionPromotionId);

    long countByOrderPromotion_PromotionId(String promotionId);

    long countByShippingPromotion_PromotionId(String promotionId);

    long countByCustomer_UserIdAndOrderPromotion_PromotionId(String customerUserId, String orderPromotionPromotionId);

    long countByCustomer_UserIdAndShippingPromotion_PromotionId(String customerUserId, String shippingPromotionPromotionId);

    long countByCustomer_UserIdAndOrderStatus(String customerUserId, OrderStatus orderStatus);

    boolean existsByCustomer_UserIdAndOrderStatusIn(String customerUserId, Collection<OrderStatus> orderStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.orderId = :orderId
          AND o.customer.userId = :customerId
        """)
    Optional<Order> findByOrderIdAndCustomer_UserIdForUpdate(
            @Param("orderId") String orderId,
            @Param("customerId") String customerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.orderId = :orderId
        """)
    Optional<Order> findByOrderIdForUpdate(
            @Param("orderId") String orderId
    );

}
