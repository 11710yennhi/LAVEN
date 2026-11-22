package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOrderDetailResponse {

    private String orderId;
    private String customerId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddressLine;
    private String receiverWard;
    private String receiverCity;
    private Integer productTotal;
    private Integer shippingFee;
    private Integer orderDiscount;
    private Integer shippingDiscount;
    private Integer discountAmount;
    private Integer totalAmount;
    private PromotionInfo orderPromotion;
    private PromotionInfo shippingPromotion;
    private String note;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private String manualPaymentReference;
    private LocalDateTime refundedAt;
    private String confirmedById;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String trackingCode;
    private LocalDateTime shippingAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime finalizedAt;
    private LocalDateTime createdAt;
    private CustomerInfo customer;
    private List<CustomerOrderDetailResponse.OrderItemResponse> items;
    private CustomerOrderDetailResponse.CancellationResponse cancellation;
    private CustomerOrderDetailResponse.ReturnRecordResponse returnRecord;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String userId;
        private String fullName;
        private String email;
        private String phone;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionInfo {
        private String promotionId;
        private String promotionCode;
        private String promotionName;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private String productId;
        private String productName;
        private String imageUrl;
        private String variantId;
        private String size;
        private String colorName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationInfo {
        private String reason;
        private LocalDateTime cancelledAt;
        private String compensationPromotionCode;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRecordInfo {
        private String returnType;
        private String returnReason;
        private String status;
        private String note;
        private String rejectionReason;
        private Integer returnShippingCost;
        private Integer refundAmount;
        private String refundBankName;
        private String refundAccountNumber;
        private String refundAccountName;
        private LocalDateTime createdAt;
        private LocalDateTime approvedAt;
        private LocalDateTime receivedAt;
    }
}
