package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotifyResponse {
    private String orderId;
    private PaymentStatus paymentStatus;
    private String message;
}