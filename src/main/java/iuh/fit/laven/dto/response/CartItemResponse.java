package iuh.fit.laven.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String variantId;
    private String colorId;
    private String colorName;
    private String productId;
    private String productName;
    private String size;
    private int quantity;       // số lượng sau khi (nếu cần) bị cắt về mức cho phép
    private int originalPrice;  // salePrice
    private int unitPrice;      // giá sau khuyến mãi sản phẩm tốt nhất
    private int subtotal;       // quantity × unitPrice (0 nếu không mua được)
    private int stock;
    private int maxQuantityPerOrder;
    private boolean available;
    private String warning;     // null nếu không có cảnh báo
}
