package iuh.fit.laven.service;

import iuh.fit.laven.model.ColorStock;
import iuh.fit.laven.dto.response.DashboardResponse;
import iuh.fit.laven.dto.response.DashboardResponse.*;
import iuh.fit.laven.repository.OrderRepository;
import iuh.fit.laven.repository.ColorStockRepository;
import iuh.fit.laven.repository.ReturnRecordRepository;
import iuh.fit.laven.repository.UserRepository;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.UserRole;
import org.springframework.data.domain.PageRequest;
import iuh.fit.laven.model.OrderDetail;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orders;
    private final UserRepository users;
    private final ReturnRecordRepository returns;
    private final ColorStockRepository inventory;
    private final ProductImageService productImages;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        var daily = orders.summarizeCreatedOrders(today.atStartOfDay(), today.plusDays(1).atStartOfDay(),
            OrderStatus.CANCELLED);
        var byStatus = new EnumMap<OrderStatus, Long>(OrderStatus.class);
        orders.countGroupedByOrderStatus().forEach(row -> byStatus.put(row.getStatus(), row.getOrders()));
        long totalOrders = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long totalCustomers = users.countByRole(UserRole.CUSTOMER);
        String registeredTodayPrefix = "USR" + today.format(DateTimeFormatter.BASIC_ISO_DATE);
        long newCustomers = users.countByRoleAndUserIdStartingWith(UserRole.CUSTOMER, registeredTodayPrefix);
        var yesterday = orders.summarizeCreatedOrders(today.minusDays(1).atStartOfDay(), today.atStartOfDay(),
            OrderStatus.CANCELLED);
        var todo = new Todo(byStatus.getOrDefault(OrderStatus.PENDING, 0L),
            orders.countByPaymentStatus(PaymentStatus.WAITING), byStatus.getOrDefault(OrderStatus.CONFIRMED, 0L),
            orders.countByPaymentStatus(PaymentStatus.REFUND_PENDING),
            returns.countByStatus(ReturnStatus.REQUESTED), returns.countByStatus(ReturnStatus.IN_TRANSIT));
        long provisional = orders.sumUnfinalizedOrders(OrderStatus.COMPLETED);

        List<Revenue> revenue = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            revenue.add(revenueFor(month.minusMonths(offset)));
        }
        List<RecentOrder> recent = orders.findTop8ByOrderByCreatedAtDescOrderIdDesc().stream()
            .map(o -> new RecentOrder(o.getOrderId(), o.getReceiverName(), o.getTotalAmount(), o.getCreatedAt(),
                o.getOrderStatus(), o.getPaymentStatus(), o.getPaymentMethod(),
                o.getOrderDetails().stream().mapToInt(OrderDetail::getQuantity).sum()))
            .toList();

        long lowStockCount = inventory.countActiveLowStock(ColorStockRepository.LOW_STOCK_THRESHOLD);
        List<ColorStock> stocks = inventory.findActiveLowStock(ColorStockRepository.LOW_STOCK_THRESHOLD, PageRequest.of(0, 6));
        List<String> productIds = stocks.stream().map(s -> s.getVariant().getProduct().getProductId()).distinct().toList();
        Map<String, String> covers = productImages.findCoverUrls(productIds);
        List<LowStock> lowStock = stocks.stream().map(s -> {
            var v = s.getVariant();
            var p = v.getProduct();
            var c = s.getColor();
            var product = new StockProduct(p.getProductId(), p.getProductName(), p.getBrand(),
                p.getCategory().getCategoryName(), s.getImageUrl() == null ? covers.get(p.getProductId()) : s.getImageUrl());
            return new LowStock(v.getVariantId(), c.getColorId(), s.getStock(), v.getSize(), c.getName(), product);
        }).toList();

        Revenue current = revenue.getLast();
        return new DashboardResponse(todo, new Today(daily.getOrders(), daily.getAmount(), newCustomers, daily.getAmount() - yesterday.getAmount()),
            new Month(current.revenue(), current.orders()),
            provisional, revenue, byStatus, totalOrders,
            totalCustomers, lowStockCount, inventory.countActiveLowStock(0), lowStock, recent);
    }

    private Revenue revenueFor(YearMonth month) {
        var totals = orders.summarizeFinalizedOrders(month.atDay(1).atStartOfDay(),
            month.plusMonths(1).atDay(1).atStartOfDay());
        return new Revenue(month.toString(), "T" + month.getMonthValue(), totals.getAmount(), totals.getOrders());
    }
}
