package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import iuh.fit.laven.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromotionTest {

    private final LocalDate today = LocalDate.of(2026, 11, 17);

    private Promotion tao(PromotionApplyType type, String code, DiscountType dType, int value,
                          LocalDate start, LocalDate end, User customer, Integer maxUsage) {
        return new Promotion("PRM2026111722000001", "Giảm giá tháng 11", null, type, code, dType, value,
                0, 0, start, end, true, customer, maxUsage);
    }

    @Test
    void maOrderHopLe_chuThuongDoiThanhInHoa() {
        Promotion p = tao(PromotionApplyType.ORDER, "giam50k", DiscountType.FIXED_AMOUNT, 50000,
                today, today.plusDays(30), null, 100);
        assertEquals("GIAM50K", p.getCode());
    }

    @Test
    void khuyenMaiSanPhamKhongCoMa() {
        Promotion p = tao(PromotionApplyType.PRODUCT, null, DiscountType.PERCENT, 10,
                today, today.plusDays(30), null, null);
        assertNull(p.getCode());
    }

    @Test
    void maOrderThieuMa_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tao(PromotionApplyType.ORDER, " ", DiscountType.FIXED_AMOUNT, 50000,
                        today, today.plusDays(30), null, null));
        assertEquals("Mã giảm giá không được rỗng đối với khuyến mãi đơn hàng/vận chuyển", ex.getMessage());
    }

    @Test
    void phanTramVuot100_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tao(PromotionApplyType.PRODUCT, null, DiscountType.PERCENT, 150,
                        today, today.plusDays(30), null, null));
        assertEquals("Phần trăm giảm không được vượt quá 100", ex.getMessage());
    }

    @Test
    void ngayKetThucTruocNgayBatDau_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tao(PromotionApplyType.SHIPPING, "FREESHIP", DiscountType.PERCENT, 100,
                        today, today.minusDays(1), null, null));
        assertEquals("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu", ex.getMessage());
    }

    @Test
    void soLuotToiDaBang0_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tao(PromotionApplyType.ORDER, "GIAM50K", DiscountType.FIXED_AMOUNT, 50000,
                        today, today.plusDays(30), null, 0));
        assertEquals("Số lượt sử dụng tối đa phải lớn hơn hoặc bằng 1", ex.getMessage());
    }

    @Test
    void maRiengChoNhanVien_baoLoi() {
        User staff = new User("USR2026111722000002", "staff@gmail.com", "0901234567", "hash", "Trần Bình",
                null, null, null, UserRole.STAFF, true, LocalDate.of(1999, 1, 1), null,
                LocalDate.of(2025, 1, 1), null, null, false, null, 0, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                tao(PromotionApplyType.ORDER, "BOITHUONG", DiscountType.FIXED_AMOUNT, 50000,
                        today, today.plusMonths(1), staff, 1));
        assertEquals("Khách hàng không hợp lệ", ex.getMessage());
    }
}