package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.Order;
import iuh.fit.laven.model.enums.OrderStatus;
import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffOrderSummaryResponse {

    private String orderId;
    private String customerId;
    private String customerName;
    private String receiverName;
    private String receiverPhone;
    private Integer totalAmount;
    private OrderStatus orderStatus;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;

    public static StaffOrderSummaryResponse from(Order order) {
        return StaffOrderSummaryResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomer().getUserId())
                .customerName(order.getCustomer().getFullName())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
