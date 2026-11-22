package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CartTest {

    private User khach() {
        return new User("USR2026111722000001", "an@gmail.com", "0901234567", "hash", "Nguyễn Văn An",
                null, null, null, UserRole.CUSTOMER, true, LocalDate.of(2002, 5, 6), null,
                null, null, null, false, null, 0, null);
    }

    private User nhanVien() {
        return new User("USR2026111722000002", "staff@gmail.com", "0901234567", "hash", "Trần Bình",
                null, null, null, UserRole.STAFF, true, LocalDate.of(1999, 1, 1), null,
                LocalDate.of(2025, 1, 1), null, null, false, null, 0, null);
    }

    // Variant, Color là entity của TV1: dùng constructor rỗng + setter hiện có của bạn ấy
    private Variant bienThe() {
        Variant v = new Variant();
        v.setVariantId("VAR2026111722000001");
        return v;
    }

    private Color mauDen() {
        Color c = new Color();
        c.setColorId("#000000");
        return c;
    }

    @Test
    void cartHopLe() {
        Cart cart = new Cart("CRT2026111722000001", khach());
        assertEquals("USR2026111722000001", cart.getCustomer().getUserId());
    }

    @Test
    void cartIdSaiDinhDang_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Cart("CART001", khach()));
        assertEquals("Mã giỏ hàng không hợp lệ", ex.getMessage());
    }

    @Test
    void cartThieuKhach_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Cart("CRT2026111722000001", null));
        assertEquals("Khách hàng không được rỗng", ex.getMessage());
    }

    @Test
    void nhanVienKhongCoGio_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new Cart("CRT2026111722000001", nhanVien()));
        assertEquals("Khách hàng không được rỗng", ex.getMessage());
    }

    @Test
    void cartItemHopLe_khoaChinhKepDuocDien() {
        Cart cart = new Cart("CRT2026111722000001", khach());
        CartItem item = new CartItem(cart, bienThe(), mauDen(), 2);
        assertEquals("CRT2026111722000001", item.getId().getCartId());
        assertEquals("VAR2026111722000001", item.getId().getVariantId());
        assertEquals("#000000", item.getId().getColorId());
    }

    @Test
    void cartItemSoLuong0_baoLoi() {
        Cart cart = new Cart("CRT2026111722000001", khach());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new CartItem(cart, bienThe(), mauDen(), 0));
        assertEquals("Số lượng phải lớn hơn 0", ex.getMessage());
    }
}