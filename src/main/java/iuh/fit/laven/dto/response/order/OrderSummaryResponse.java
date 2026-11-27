package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private String orderId;
    private String customerId;
    private String receiverName;
    private String receiverPhone;
    private String receiverCity;
    private Integer totalAmount;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime finalizedAt;

    public static OrderSummaryResponse from(Order o) {
        return OrderSummaryResponse.builder()
                .orderId(o.getOrderId())
                .customerId(o.getCustomer().getUserId())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .receiverCity(o.getReceiverCity())
                .totalAmount(o.getTotalAmount())
                .orderStatus(o.getOrderStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .createdAt(o.getCreatedAt())
                .completedAt(o.getCompletedAt())
                .finalizedAt(o.getFinalizedAt())
                .build();
    }
}