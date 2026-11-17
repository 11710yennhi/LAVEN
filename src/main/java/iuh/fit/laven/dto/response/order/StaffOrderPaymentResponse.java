package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.PaymentMethod;
import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffOrderPaymentResponse {
    private String orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Integer refundAmount;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String message;
}
