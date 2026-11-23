package iuh.fit.laven.dto.response;

/** Khoảng giá bán của 1 sản phẩm theo các size, để hiển thị "Từ X₫". */
public record SalePriceRange(Integer min, Integer max) {
}
