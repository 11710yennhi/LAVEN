package iuh.fit.laven.dto.response.order;

import iuh.fit.laven.model.enums.PaymentStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQrResponse {
    private String orderId;
    private String bankId;
    private String accountNo;
    private String accountName;
    private Integer amount;
    private String content;
    private String qrUrl;
    private PaymentStatus paymentStatus;
}
