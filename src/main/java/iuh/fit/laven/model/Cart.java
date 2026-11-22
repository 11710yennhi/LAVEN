package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    private static final Pattern CART_ID_PATTERN = Pattern.compile("^CRT\\d{16}$");

    @Id
    @Column(length = 19, nullable = false)
    private String cartId;

    // unique = true: mỗi khách chỉ có đúng 1 giỏ hàng (quan hệ 1-1)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customerId", nullable = false, unique = true)
    private User customer;

    public Cart(String cartId, User customer) {
        setCartId(cartId);
        setCustomer(customer);
    }

    public void setCartId(String cartId) {
        if (cartId == null || !CART_ID_PATTERN.matcher(cartId).matches()) {
            throw new IllegalArgumentException("Mã giỏ hàng không hợp lệ");
        }
        this.cartId = cartId;
    }

    public void setCustomer(User customer) {
        // Admin và Staff không mua hàng nên không có giỏ
        if (customer == null || customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Khách hàng không được rỗng");
        }
        this.customer = customer;
    }

    @Override
    public String toString() {
        return "Cart{cartId='" + cartId + "', customerId='"
                + (customer == null ? null : customer.getUserId()) + "'}";
    }
}