package iuh.fit.laven.repository;

import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentStatus;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    /**
     * Danh sách đơn (nhân viên / khách). cancellation, returnRecord là @OneToOne(mappedBy) nên Hibernate
     * không lazy được, mỗi đơn sẽ tốn thêm 2 SELECT; JOIN luôn ở đây (quan hệ 1-1 nên phân trang vẫn đúng).
     */
    @Override
    @EntityGraph(attributePaths = {"customer", "cancellation", "returnRecord"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

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


    interface Totals {
        long getOrders();
        long getAmount();
    }

    interface StatusCount {
        OrderStatus getStatus();
        long getOrders();
    }

    @Query("""
        SELECT COUNT(o) AS orders, COALESCE(SUM(o.totalAmount), 0) AS amount FROM Order o
        WHERE o.createdAt >= :start AND o.createdAt < :end AND o.orderStatus <> :excluded
        """)
    Totals summarizeCreatedOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                  @Param("excluded") OrderStatus excluded);

    @Query("SELECT o.orderStatus AS status, COUNT(o) AS orders FROM Order o GROUP BY o.orderStatus")
    List<StatusCount> countGroupedByOrderStatus();

    long countByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus = :status AND o.finalizedAt IS NULL")
    long sumUnfinalizedOrders(@Param("status") OrderStatus status);

    List<Order> findTop8ByOrderByCreatedAtDescOrderIdDesc();

    @Query("""
        SELECT COALESCE(SUM(o.totalAmount - CASE
            WHEN r.returnType = CUSTOMER_RETURN AND
                (r.status = RECEIVED OR (r.returnReason = NOT_RECEIVED AND r.status = APPROVED))
            THEN COALESCE(r.refundAmount, 0) ELSE 0 END), 0) AS amount, COUNT(o) AS orders
        FROM Order o LEFT JOIN o.returnRecord r
        WHERE o.finalizedAt >= :start AND o.finalizedAt < :end
          AND o.orderStatus IN (COMPLETED, RETURNED)
        """)
    Totals summarizeFinalizedOrders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
