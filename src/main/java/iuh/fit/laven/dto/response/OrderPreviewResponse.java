package iuh.fit.laven.dto.response;

import iuh.fit.laven.model.enums.PaymentMethod;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPreviewResponse {

    private List<Item> items;
    private String receiverCity;
    private PaymentMethod paymentMethod;
    private Promotion orderPromotion;
    private Promotion shippingPromotion;
    private Integer productTotal;
    private Integer shippingFee;
    private Integer orderDiscount;
    private Integer shippingDiscount;
    private Integer discountAmount;
    private Integer totalAmount;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String variantId;
        private String colorId;
        private String productId;
        private String productDisplayName;
        private String imageUrl;
        private String size;
        private String colorName;
        private Integer quantity;
        private Integer salePrice;
        private Integer productDiscount;
        private Integer unitPrice;
        private Integer subtotal;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Promotion {
        private String promotionCode;
        private String promotionName;
        private Integer discountAmount;
    }
}