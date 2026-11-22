package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import iuh.fit.laven.model.enums.ReturnReason;
import iuh.fit.laven.model.enums.ReturnStatus;
import iuh.fit.laven.model.enums.ReturnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderDetailResponse {

    private String orderId;
    private OrderStatus orderStatus;
    private LocalDateTime createdAt;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddressLine;
    private String receiverWard;
    private String receiverCity;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private Integer productTotal;
    private Integer shippingFee;
    private Integer orderDiscount;
    private Integer shippingDiscount;
    private Integer discountAmount;
    private Integer totalAmount;
    private String trackingCode;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippingAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime finalizedAt;
    private String note;
    private List<OrderItemResponse> items;
    private CancellationResponse cancellation;
    private ReturnRecordResponse returnRecord;


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {

        private String productId;
        private String productName;
        private String imageUrl;
        private String variantId;
        private String variantSize;
        private String colorId;
        private String colorName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer subtotal;
    }


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancellationResponse {

        private String reason;
        private LocalDateTime cancelledAt;
        private String compensationPromotionCode;
    }


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnRecordResponse {

        private ReturnType returnType;
        private ReturnReason returnReason;
        private ReturnStatus status;
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