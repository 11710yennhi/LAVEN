package iuh.fit.laven.service;

import iuh.fit.laven.model.UserEvent.Type;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventService {
    private final JdbcTemplate jdbc;

    public record RankedProduct(String productId, String size, long score, long soldCount,
                                long viewCount, long addToCartCount, long purchaseCount) {}

    public void record(String userId, String productId, Type type) {
        jdbc.update("INSERT INTO user_events(userId, productId, eventType, createdAt) VALUES (?, ?, ?, ?)",
            userId, productId, type.name(), LocalDateTime.now());
    }

    public void recordPurchase(String userId, String productId, String orderId) {
        jdbc.update("""
            INSERT INTO user_events(userId, productId, eventType, createdAt, purchaseOrderId)
            VALUES (?, ?, 'PURCHASE', ?, ?) ON DUPLICATE KEY UPDATE id = id
            """, userId, productId, LocalDateTime.now(), orderId);
    }

    public List<RankedProduct> featured(String categoryId) {
        var now = LocalDateTime.now();
        return jdbc.query("""
            SELECT p.productId, inventory.size,
                   COALESCE(activity.viewCount, 0) AS viewCount,
                   COALESCE(activity.addToCartCount, 0) AS addToCartCount,
                   COALESCE(activity.purchaseCount, 0) AS purchaseCount,
                   COALESCE(activity.viewCount + 2 * activity.addToCartCount + 5 * activity.purchaseCount, 0) AS score,
                   (SELECT COALESCE(SUM(d.quantity), 0) FROM order_details d
                    JOIN variants v ON v.variantId = d.variantId JOIN orders o ON o.orderId = d.orderId
                    WHERE v.productId = p.productId AND o.orderStatus = 'COMPLETED') AS soldCount
            FROM products p
            JOIN categories c ON c.categoryId = p.categoryId
            JOIN (
                SELECT v.productId, MIN(v.size) AS size
                FROM variants v JOIN color_stocks s ON s.variantId = v.variantId
                WHERE s.stock > 0 GROUP BY v.productId
            ) inventory ON inventory.productId = p.productId
            LEFT JOIN (
                SELECT productId,
                       COUNT(CASE WHEN eventType = 'VIEW' THEN 1 END) AS viewCount,
                       COUNT(CASE WHEN eventType = 'ADD_TO_CART' THEN 1 END) AS addToCartCount,
                       COUNT(DISTINCT CASE WHEN eventType = 'PURCHASE' THEN purchaseOrderId END) AS purchaseCount
                FROM user_events WHERE createdAt BETWEEN ? AND ? GROUP BY productId
            ) activity ON activity.productId = p.productId
            WHERE p.status = TRUE AND c.status = TRUE AND (? IS NULL OR p.categoryId = ?)
            ORDER BY score DESC, p.productName ASC, p.productId ASC
            LIMIT 5
            """, (rs, row) -> new RankedProduct(rs.getString("productId"), rs.getString("size"),
                rs.getLong("score"), rs.getLong("soldCount"), rs.getLong("viewCount"),
                rs.getLong("addToCartCount"), rs.getLong("purchaseCount")),
            now.minusDays(7), now, categoryId, categoryId);
    }
}
