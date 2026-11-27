package iuh.fit.laven.service;

import iuh.fit.laven.dto.response.DashboardSalesResponse;
import iuh.fit.laven.dto.response.DashboardSalesResponse.BestSeller;
import iuh.fit.laven.dto.response.DashboardSalesResponse.CategoryRevenue;
import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.ReturnRecord;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class DashboardSalesService {
    public enum Period { DAYS_7, DAYS_30, MONTH }

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<OrderStatus> REVENUE_STATUSES = List.of(OrderStatus.COMPLETED, OrderStatus.RETURNED);

    private final EntityManager entityManager;
    private final ProductImageService images;

    @Transactional(readOnly = true)
    public DashboardSalesResponse getSales(Period period) {
        LocalDate today = LocalDate.now(VIETNAM);
        LocalDate from = switch (period) {
            case DAYS_7 -> today.minusDays(6);
            case DAYS_30 -> today.minusDays(29);
            case MONTH -> today.withDayOfMonth(1);
        };
        LocalDate to = period == Period.MONTH ? today.withDayOfMonth(today.lengthOfMonth()) : today;
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        var orders = entityManager.createQuery("""
                SELECT o FROM Order o LEFT JOIN FETCH o.returnRecord
                WHERE o.finalizedAt >= :start AND o.finalizedAt < :end AND o.orderStatus IN :statuses
                """, Order.class)
                .setParameter("start", start).setParameter("end", end)
                .setParameter("statuses", REVENUE_STATUSES).getResultList();
        var rows = entityManager.createQuery("""
                SELECT d.order.orderId, p.productId, p.productName, c.categoryId, c.categoryName,
                       d.quantity, d.unitPrice, ri.quantity
                FROM OrderDetail d JOIN d.variant.product p JOIN p.category c
                LEFT JOIN ReturnItem ri ON ri.order = d.order AND ri.variant = d.variant AND ri.color = d.color
                WHERE d.order.finalizedAt >= :start AND d.order.finalizedAt < :end AND d.order.orderStatus IN :statuses
                """, Object[].class)
                .setParameter("start", start).setParameter("end", end)
                .setParameter("statuses", REVENUE_STATUSES).getResultList();
        Map<String, List<Object[]>> byOrder = rows.stream().collect(java.util.stream.Collectors.groupingBy(row -> (String) row[0]));
        Map<String, BestSeller> products = new HashMap<>();
        Map<String, CategoryRevenue> categories = new HashMap<>();
        long revenue = 0;
        for (Order order : orders) {
            ReturnRecord returned = countedReturn(order.getReturnRecord());
            long refund = returned == null ? 0 : returned.getRefundAmount();
            revenue += order.getTotalAmount() - refund;
            Map<String, CategoryRevenue> retained = new TreeMap<>();
            for (Object[] row : byOrder.getOrDefault(order.getOrderId(), List.of())) {
                int bought = (Integer) row[5];
                int returnedQuantity = returned == null ? 0 : returned.getReturnReason() == ReturnReason.NOT_RECEIVED
                        ? bought : row[7] == null ? 0 : (Integer) row[7];
                int kept = Math.max(0, bought - returnedQuantity);
                if (kept == 0) continue;
                var product = new BestSeller((String) row[1], (String) row[2], (String) row[4], null, kept);
                products.merge(product.productId(), product, (a, b) -> new BestSeller(
                        a.productId(), a.productName(), a.categoryName(), null, a.netQuantity() + b.netQuantity()));
                addCategory(retained, new CategoryRevenue((String) row[3], (String) row[4], (long) kept * (Integer) row[6]));
            }
            long retainedGross = retained.values().stream().mapToLong(CategoryRevenue::netRevenue).sum();
            if (retainedGross == 0) continue;
            // Phí ship chỉ được hoàn khi trả hết. Đơn còn hàng giữ lại chỉ trừ phần hoàn tiền hàng.
            long netMerchandise = Math.max(0L, (long) order.getProductTotal() - order.getOrderDiscount() - refund);
            long cumulative = 0;
            long allocated = 0;
            for (CategoryRevenue category : retained.values()) {
                cumulative += category.netRevenue();
                long throughCategory = netMerchandise * cumulative / retainedGross;
                addCategory(categories, new CategoryRevenue(category.categoryId(), category.categoryName(), throughCategory - allocated));
                allocated = throughCategory;
            }
        }
        var vietnameseNames = Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        vietnameseNames.setStrength(Collator.SECONDARY);
        var top = products.values().stream().sorted(Comparator.comparingLong(BestSeller::netQuantity).reversed()
                .thenComparing(BestSeller::productName, vietnameseNames)
                .thenComparing(BestSeller::productId)).limit(5).toList();
        var covers = images.findCoverUrls(top.stream().map(BestSeller::productId).toList());
        var bestSellers = top.stream().map(p -> new BestSeller(p.productId(), p.productName(), p.categoryName(),
                covers.get(p.productId()), p.netQuantity())).toList();
        var categoryTotals = categories.values().stream().filter(c -> c.netRevenue() > 0)
                .sorted(Comparator.comparingLong(CategoryRevenue::netRevenue).reversed().thenComparing(CategoryRevenue::categoryId))
                .toList();
        Long average = orders.isEmpty() ? null : Math.round((double) revenue / orders.size());
        return new DashboardSalesResponse(from, to, orders.size(), revenue, average, bestSellers, categoryTotals);
    }

    private static void addCategory(Map<String, CategoryRevenue> totals, CategoryRevenue row) {
        totals.merge(row.categoryId(), row, (a, b) -> new CategoryRevenue(
                a.categoryId(), a.categoryName(), a.netRevenue() + b.netRevenue()));
    }

    private static ReturnRecord countedReturn(ReturnRecord record) {
        if (record == null || record.getReturnType() != ReturnType.CUSTOMER_RETURN) return null;
        return record.getStatus() == ReturnStatus.RECEIVED
                || (record.getReturnReason() == ReturnReason.NOT_RECEIVED && record.getStatus() == ReturnStatus.APPROVED) ? record : null;
    }
}
