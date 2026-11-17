package iuh.fit.laven.repository;

import iuh.fit.laven.model.CartItem;
import iuh.fit.laven.model.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    List<CartItem> findByCart_CartId(String cartId);

    /** Quy trình 7 bước 6: xóa món đã đặt khỏi giỏ (TV3 gọi trong hàm đặt đơn). colorId truyền chữ hoa. */
    @Modifying
    @Query("""
            DELETE FROM CartItem ci
            WHERE ci.id.cartId = (SELECT c.cartId FROM Cart c WHERE c.customer.userId = :customerId)
              AND ci.id.variantId = :variantId
              AND ci.id.colorId = :colorId
            """)
    void deleteSelectedItem(@Param("customerId") String customerId,
                            @Param("variantId") String variantId,
                            @Param("colorId") String colorId);
}