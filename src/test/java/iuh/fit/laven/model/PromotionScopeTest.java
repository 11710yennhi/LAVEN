package iuh.fit.laven.model;

import iuh.fit.laven.model.enums.DiscountType;
import iuh.fit.laven.model.enums.PromotionApplyType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromotionScopeTest {

    private final LocalDate today = LocalDate.of(2026, 11, 17);

    private Promotion kmSanPham() {
        return new Promotion("PRM2026111722000001", "Giảm 10% vali", null, PromotionApplyType.PRODUCT,
                null, DiscountType.PERCENT, 10, 0, 0, today, today.plusDays(30), true, null, null);
    }

    private Promotion maDonHang() {
        return new Promotion("PRM2026111722000002", "Giảm 50K", null, PromotionApplyType.ORDER,
                "GIAM50K", DiscountType.FIXED_AMOUNT, 50000, 0, 0, today, today.plusDays(30), true, null, null);
    }

    // Category là entity của TV1: dùng constructor rỗng + setter hiện có của bạn ấy
    private Category vali() {
        Category c = new Category();
        c.setCategoryId("CAT001");
        return c;
    }

    @Test
    void thuongHieuHopLe_khoaChinhKepDuocDien() {
        PromotionBrand b = new PromotionBrand(kmSanPham(), "  Samsonite ");
        assertEquals("Samsonite", b.getBrand());
        assertEquals("PRM2026111722000001", b.getId().getPromotionId());
    }

    @Test
    void thuongHieuRong_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PromotionBrand(kmSanPham(), " "));
        assertEquals("Tên thương hiệu không được rỗng", ex.getMessage());
    }

    @Test
    void thieuKhuyenMai_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PromotionBrand(null, "Samsonite"));
        assertEquals("Khuyến mãi không được rỗng", ex.getMessage());
    }

    @Test
    void ganPhamViChoMaDonHang_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PromotionBrand(maDonHang(), "Samsonite"));
        assertEquals("Khuyến mãi không hợp lệ", ex.getMessage());
    }

    @Test
    void danhMucHopLe_khoaChinhKepDuocDien() {
        PromotionCategory pc = new PromotionCategory(kmSanPham(), vali());
        assertEquals("CAT001", pc.getId().getCategoryId());
    }

    @Test
    void thieuDanhMuc_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PromotionCategory(kmSanPham(), null));
        assertEquals("Danh mục sản phẩm không được rỗng", ex.getMessage());
    }

    @Test
    void thieuSanPham_baoLoi() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new PromotionProduct(kmSanPham(), null));
        assertEquals("Sản phẩm không được rỗng", ex.getMessage());
    }
}