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

/** Phiếu trả hàng / giao thất bại kèm món trả và bằng chứng. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnDetailResponse {

    private String orderId;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Integer totalAmount;

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

    /** Hạn khách phải gửi hàng (approvedAt + 7 ngày); chỉ có khi đang APPROVED và lý do khác NOT_RECEIVED */
    private LocalDateTime sendBackDeadline;

    private LocalDateTime receivedAt;
    private String receivedByName;

    private List<Item> items;
    private List<String> evidenceUrls;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productId;
        private String productDisplayName;
        private String variantId;
        private String size;
        private String colorId;
        private String colorName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer damagedQuantity;
        private String inspectionNote;
        private String receivedVariantId;
        private String receivedColorId;
    }
}
