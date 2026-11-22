package iuh.fit.laven.service;

import iuh.fit.laven.dto.response.StatisticsResponse;
import iuh.fit.laven.exception.AppException;
import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.ReturnRecord;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC37 - Chốt sổ doanh thu và UC31 - Thống kê (quy trình 17, công thức phần g).
 * Truy vấn bằng EntityManager để không phải sửa repository của các phần khác.
 */
@Slf4j
@Service
public class StatisticsService {

    /** Khách được yêu cầu trả hàng trong 7 ngày kể từ completedAt */
    private static final int RETURN_WINDOW_DAYS = 7;
    /** Sắp hết hàng: stock ≤ 5 */
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int DEFAULT_TOP = 10;
    private static final int MAX_TOP = 50;

    /** Đơn có thể có doanh thu khi chốt sổ (CANCELLED, DELIVERY_FAILED không có doanh thu) */
    private static final List<OrderStatus> REVENUE_STATUSES = List.of(OrderStatus.COMPLETED, OrderStatus.RETURNED);

    @PersistenceContext
    private EntityManager entityManager;

    // =====================================================================
    // UC37 - Chốt sổ (gọi từ scheduler/OrderFinalizeJob mỗi ngày)
    // Đơn COMPLETED không có ReturnRecord và completedAt + 7 ngày ≤ now
    // → finalizedAt = completedAt + 7 ngày (quy trình 17 bước 1).
    // Đơn có phiếu trả hàng được chốt sổ ở ReturnService khi phiếu kết thúc.
    // =====================================================================
    @Transactional
    public int finalizeOrdersPastReturnWindow() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(RETURN_WINDOW_DAYS);
        List<Order> orders = entityManager.createQuery("""
                SELECT o FROM Order o
                WHERE o.orderStatus = :status
                  AND o.finalizedAt IS NULL
                  AND o.completedAt <= :deadline
                  AND NOT EXISTS (SELECT 1 FROM ReturnRecord r WHERE r.order = o)
                """, Order.class)
                .setParameter("status", OrderStatus.COMPLETED)
                .setParameter("deadline", deadline)
                .getResultList();

        for (Order order : orders) {
            order.setFinalizedAt(order.getCompletedAt().plusDays(RETURN_WINDOW_DAYS));
        }
        return orders.size();
    }

    // =====================================================================
    // UC31 - Thống kê tháng M
    // =====================================================================
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(Integer year, Integer month, Integer top) {
        YearMonth yearMonth = resolveMonth(year, month);
        int topLimit = top == null ? DEFAULT_TOP : top;
        if (topLimit < 1 || topLimit > MAX_TOP) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Số sản phẩm bán chạy phải từ 1 đến " + MAX_TOP);
        }
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // ---- Doanh thu đã chốt tháng M ----
        List<Order> finalizedOrders = entityManager.createQuery("""
                SELECT o FROM Order o
                LEFT JOIN FETCH o.returnRecord
                WHERE o.finalizedAt >= :start AND o.finalizedAt < :end
                  AND o.orderStatus IN :statuses
                """, Order.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("statuses", REVENUE_STATUSES)
                .getResultList();

        long finalizedRevenue = 0;
        long finalizedRefund = 0;
        for (Order order : finalizedOrders) {
            long refund = countedRefund(order.getReturnRecord());
            finalizedRevenue += order.getTotalAmount() - refund;
            finalizedRefund += refund;
        }

        // ---- Doanh thu tạm tính ----
        Object[] provisional = entityManager.createQuery("""
                SELECT COALESCE(SUM(o.totalAmount), 0), COUNT(o) FROM Order o
                WHERE o.orderStatus = :status AND o.finalizedAt IS NULL
                """, Object[].class)
                .setParameter("status", OrderStatus.COMPLETED)
                .getSingleResult();

        return StatisticsResponse.builder()
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .finalizedRevenue(finalizedRevenue)
                .finalizedOrderCount(finalizedOrders.size())
                .finalizedRefundAmount(finalizedRefund)
                .provisionalRevenue(((Number) provisional[0]).longValue())
                .provisionalOrderCount(((Number) provisional[1]).longValue())
                .bestSellers(findBestSellers(start, end, topLimit))
                .lowStocks(findLowStocks())
                .orderCountByStatus(countOrdersByStatus())
                .returnCosts(sumReturnCosts(start, end))
                .build();
    }

    /**
     * refundAmount chỉ tính khi phiếu trả hàng đã RECEIVED, hoặc NOT_RECEIVED đã APPROVED; còn lại tính 0
     * (ghi chú công thức "Doanh thu đã chốt tháng M", phần g).
     */
    private long countedRefund(ReturnRecord record) {
        return isCountedReturn(record) ? record.getRefundAmount() : 0;
    }

    private boolean isCountedReturn(ReturnRecord record) {
        if (record == null || record.getReturnType() != ReturnType.CUSTOMER_RETURN) {
            return false;
        }
        return record.getStatus() == ReturnStatus.RECEIVED
                || (record.getReturnReason() == ReturnReason.NOT_RECEIVED
                    && record.getStatus() == ReturnStatus.APPROVED);
    }

    /** Bán chạy: Σ (quantity đã mua − quantity trả) của các đơn chốt sổ trong tháng (quy trình 17 bước 4). */
    private List<StatisticsResponse.BestSeller> findBestSellers(LocalDateTime start, LocalDateTime end, int top) {
        List<Object[]> soldRows = entityManager.createQuery("""
                SELECT d.variant.product.productId, d.variant.product.productName, SUM(d.quantity)
                FROM OrderDetail d
                WHERE d.order.finalizedAt >= :start AND d.order.finalizedAt < :end
                  AND d.order.orderStatus IN :statuses
                GROUP BY d.variant.product.productId, d.variant.product.productName
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("statuses", REVENUE_STATUSES)
                .getResultList();

        List<Object[]> returnedRows = entityManager.createQuery("""
                SELECT i.variant.product.productId, SUM(i.quantity)
                FROM ReturnItem i, ReturnRecord r
                WHERE r.order = i.order
                  AND i.order.finalizedAt >= :start AND i.order.finalizedAt < :end
                  AND r.returnType = :customerReturn
                  AND (r.status = :received OR (r.returnReason = :notReceived AND r.status = :approved))
                GROUP BY i.variant.product.productId
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("customerReturn", ReturnType.CUSTOMER_RETURN)
                .setParameter("received", ReturnStatus.RECEIVED)
                .setParameter("notReceived", ReturnReason.NOT_RECEIVED)
                .setParameter("approved", ReturnStatus.APPROVED)
                .getResultList();

        Map<String, Long> returnedByProduct = new HashMap<>();
        for (Object[] row : returnedRows) {
            returnedByProduct.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<StatisticsResponse.BestSeller> result = new ArrayList<>();
        for (Object[] row : soldRows) {
            String productId = (String) row[0];
            long sold = ((Number) row[2]).longValue();
            long returned = returnedByProduct.getOrDefault(productId, 0L);
            result.add(StatisticsResponse.BestSeller.builder()
                    .productId(productId)
                    .productName((String) row[1])
                    .soldQuantity(sold)
                    .returnedQuantity(returned)
                    .netQuantity(sold - returned)
                    .build());
        }
        return result.stream()
                .filter(b -> b.getNetQuantity() > 0)
                .sorted(Comparator.comparingLong(StatisticsResponse.BestSeller::getNetQuantity).reversed())
                .limit(top)
                .toList();
    }

    /** Sắp hết hàng: stock ≤ 5, chỉ tính sản phẩm và danh mục đang kinh doanh. */
    private List<StatisticsResponse.LowStock> findLowStocks() {
        List<ColorStock> stocks = entityManager.createQuery("""
                SELECT cs FROM ColorStock cs
                WHERE cs.stock <= :threshold
                  AND cs.variant.product.status = true
                  AND cs.variant.product.category.status = true
                ORDER BY cs.stock ASC
                """, ColorStock.class)
                .setParameter("threshold", LOW_STOCK_THRESHOLD)
                .getResultList();

        return stocks.stream()
                .map(cs -> StatisticsResponse.LowStock.builder()
                        .productId(cs.getVariant().getProduct().getProductId())
                        .productName(cs.getVariant().getProduct().getProductName())
                        .variantId(cs.getVariant().getVariantId())
                        .size(cs.getVariant().getSize())
                        .colorId(cs.getColor().getColorId())
                        .colorName(cs.getColor().getName())
                        .stock(cs.getStock())
                        .build())
                .toList();
    }

    /** Số đơn theo trạng thái; trạng thái không có đơn trả về 0. */
    private Map<OrderStatus, Long> countOrdersByStatus() {
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status, 0L);
        }
        List<Object[]> rows = entityManager.createQuery(
                        "SELECT o.orderStatus, COUNT(o) FROM Order o GROUP BY o.orderStatus", Object[].class)
                .getResultList();
        for (Object[] row : rows) {
            result.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    /**
     * Chi phí ship chiều về theo returnType, tính các phiếu tạo trong tháng mà hàng thực sự được gửi về:
     * mọi phiếu FAILED_DELIVERY; phiếu CUSTOMER_RETURN đã IN_TRANSIT hoặc RECEIVED
     * (phiếu bị từ chối, quá hạn hay NOT_RECEIVED không phát sinh ship chiều về).
     */
    private List<StatisticsResponse.ReturnCost> sumReturnCosts(LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = entityManager.createQuery("""
                SELECT r.returnType, COUNT(r), COALESCE(SUM(r.returnShippingCost), 0)
                FROM ReturnRecord r
                WHERE r.createdAt >= :start AND r.createdAt < :end
                  AND (r.returnType = :failedDelivery OR r.status IN :shippedStatuses)
                GROUP BY r.returnType
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("failedDelivery", ReturnType.FAILED_DELIVERY)
                .setParameter("shippedStatuses", List.of(ReturnStatus.IN_TRANSIT, ReturnStatus.RECEIVED))
                .getResultList();

        Map<ReturnType, StatisticsResponse.ReturnCost> byType = new LinkedHashMap<>();
        for (ReturnType type : ReturnType.values()) {
            byType.put(type, new StatisticsResponse.ReturnCost(type, 0, 0));
        }
        for (Object[] row : rows) {
            ReturnType type = (ReturnType) row[0];
            byType.put(type, new StatisticsResponse.ReturnCost(
                    type, ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return new ArrayList<>(byType.values());
    }

    /** Mặc định tháng hiện tại; năm và tháng phải đi cùng nhau. */
    private YearMonth resolveMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.from(LocalDate.now());
        }
        if (year == null || month == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng nhập cả năm và tháng");
        }
        if (month < 1 || month > 12) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Tháng phải từ 1 đến 12");
        }
        if (year < 2000 || year > 9999) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Năm không hợp lệ");
        }
        return YearMonth.of(year, month);
    }
}
