package iuh.fit.laven.dto.response.ai;

import iuh.fit.laven.dto.response.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Kết quả tìm kiếm ngữ nghĩa: danh sách sản phẩm kèm điểm tương đồng. */
@Getter
@AllArgsConstructor
public class SemanticSearchResponse {

    private final String query;

    /** true: AI không dùng được hoặc không có kết quả đủ giống, đã chuyển sang tìm từ khóa. */
    private final boolean keywordFallback;

    private final List<Item> items;

    @Getter
    @AllArgsConstructor
    public static class Item {
        /** Thông tin sản phẩm công khai (DTO có sẵn, không lộ giá nhập). */
        private final ProductResponse product;

        /** Giá sau khuyến mãi thấp nhất trong các size khớp khoảng giá ("Từ X₫"), đọc từ DB lúc tìm. */
        private final Integer finalPrice;

        /** Giá sau khuyến mãi cao nhất trong các size khớp khoảng giá. */
        private final Integer maxFinalPrice;

        /** Tổng tồn kho của mọi biến thể–màu, đọc từ DB lúc tìm (0 = hết hàng). */
        private final Integer stock;

        /** Độ giống 0..1 (càng gần 1 càng giống); null nếu kết quả đến từ tìm từ khóa. */
        private final Double score;
    }
}