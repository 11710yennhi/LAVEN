package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(Todo todo, Today today, Month month, long provisional, List<Revenue> revenue,
                           Map<OrderStatus, Long> byStatus, long totalOrders, long totalCustomers,
                           long lowStockCount, long outOfStockCount, List<LowStock> lowStock, List<RecentOrder> recentOrders) {

    public record Todo(long pending, long waitingPay, long toShip, long refundPending,
                       long returnRequested, long returnInTransit) {}
    public record Today(long orders, long amount, long customers, long revenueDelta) {}
    public record Month(long revenue, long orders, Long estimatedProfit) {}
    public record Revenue(String key, String label, long revenue, long orders) {}
    public record RecentOrder(String orderId, String receiverName, int totalAmount, LocalDateTime createdAt,
                              OrderStatus orderStatus, PaymentStatus paymentStatus, PaymentMethod paymentMethod,
                              int itemCount) {}
    public record LowStock(String variantId, String colorId, int stock, String size, String colorName,
                           StockProduct product) {}
    public record StockProduct(String productId, String productName, String brand, String categoryName, String imageUrl) {}
}
