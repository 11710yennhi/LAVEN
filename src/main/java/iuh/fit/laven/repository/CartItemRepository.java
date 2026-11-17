package iuh.fit.laven.repository;

import iuh.fit.laven.model.CartItem;
import iuh.fit.laven.model.CartItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, CartItemId> {
    List<CartItem> findByCart_CartId(String cartId);
}
