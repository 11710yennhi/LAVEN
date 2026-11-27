package iuh.fit.laven.model;

import iuh.fit.laven.model.id.CartItemId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    // Khóa chính kép (cartId, variantId, colorId); các setter bên dưới tự điền vào đây
    @EmbeddedId
    private CartItemId id = new CartItemId();

    @MapsId("cartId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cartId", nullable = false)
    private Cart cart;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variantId", nullable = false)
    private Variant variant;

    @MapsId("colorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colorId", nullable = false)
    private Color color;

    @Column(nullable = false)
    private Integer quantity;

    public CartItem(Cart cart, Variant variant, Color color, Integer quantity) {
        setCart(cart);
        setVariant(variant);
        setColor(color);
        setQuantity(quantity);
    }

    public void setCart(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Giỏ hàng không được rỗng");
        }
        this.cart = cart;
        this.id.setCartId(cart.getCartId());
    }

    public void setVariant(Variant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("Biến thể không được rỗng");
        }
        this.variant = variant;
        this.id.setVariantId(variant.getVariantId());
    }

    // Kiểm tra cặp (variant, color) có trong ColorStock: cần truy vấn DB nên nằm ở CartService
    public void setColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Màu sắc không được rỗng");
        }
        this.color = color;
        this.id.setColorId(color.getColorId());
    }

    // Kiểm tra <= tồn kho và <= maxQuantityPerOrder: cần truy vấn DB nên nằm ở CartService
    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{cartId='" + id.getCartId() + "', variantId='" + id.getVariantId()
                + "', colorId='" + id.getColorId() + "', quantity=" + quantity + "}";
    }
}