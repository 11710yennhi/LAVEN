package iuh.fit.laven.repository;

import iuh.fit.laven.model.ReturnItem;
import iuh.fit.laven.model.id.ReturnItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, ReturnItemId> {

    List<ReturnItem> findAllById_OrderId(String orderId);

    /** Món trả của nhiều đơn, JOIN luôn sản phẩm và màu để hiển thị (tránh N+1). */
    @Query("""
        SELECT i FROM ReturnItem i
        JOIN FETCH i.variant v
        JOIN FETCH v.product p
        JOIN FETCH p.category
        JOIN FETCH i.color
        WHERE i.id.orderId IN :orderIds
        """)
    List<ReturnItem> findAllWithProductByOrderIds(@Param("orderIds") Collection<String> orderIds);
}
