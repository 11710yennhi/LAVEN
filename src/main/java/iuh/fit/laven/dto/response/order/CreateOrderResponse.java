package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderResponse {

    private String orderId;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddressLine;
    private String receiverWard;
    private String receiverCity;
    private String note;
    private List<Item> items;
    private Integer productTotal;
    private Integer shippingFee;
    private Integer orderDiscount;
    private Integer shippingDiscount;
    private Integer discountAmount;
    private Integer totalAmount;
    private String paymentQrUrl;
    private LocalDateTime createdAt;

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
        private Integer unitPrice;
        private Integer subtotal;
    }
}